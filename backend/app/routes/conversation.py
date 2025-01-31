from flask import jsonify, request, current_app, Blueprint
import json
from app import db
from app.models import (
    ConversationSession, Message, Scene,
    UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression, SceneLevel
)
from app.llm.client import LLMClient
from app.llm.prompts import Prompts
import logging
import re
import traceback
from typing import List

# Set up logger
logger = logging.getLogger(__name__)

print("Creating LLM client instance")  # Debug print
llm_client = LLMClient()

# Create blueprint with url_prefix
bp = Blueprint('conversation', __name__, url_prefix='/api')


def extract_tutor_feedback(response: str) -> dict:
    """Extract feedback JSON from tutor response"""
    import json
    import re
    
    print(f"Attempting to extract tutor feedback from response of length {len(response)}", flush=True)
    
    # If response starts with "tutor_message:", try to convert to JSON
    if response.startswith("tutor_message:"):
        message = response.replace("tutor_message:", "").strip()
        return {
            "feedback": json.dumps({
                "unfamiliar_words": [],
                "not_so_good_expressions": {},
                "grammar_errors": {},
                "best_fit_words": {}
            }),
            "tutor_message": message,
            "needs_correction": False
        }
    
    # First try: Extract JSON from markdown code block
    code_block_match = re.search(r'```json\s*(\{.*?\})\s*```', response, re.DOTALL)
    if code_block_match:
        try:
            json_str = code_block_match.group(1)
            parsed_json = json.loads(json_str)
            if 'feedback' in parsed_json:
                return {
                    "feedback": json.dumps(parsed_json['feedback']),
                    "tutor_message": parsed_json['tutor_message'],
                    "needs_correction": any([
                        parsed_json['feedback'].get('unfamiliar_words', []),
                        parsed_json['feedback'].get('grammar_errors', {}),
                        parsed_json['feedback'].get('not_so_good_expressions', {}),
                        parsed_json['feedback'].get('best_fit_words', {})
                    ])
                }
        except json.JSONDecodeError:
            print("Failed to parse JSON from code block", flush=True)
    
    # Second try: Parse the entire response as JSON
    try:
        parsed_json = json.loads(response)
        if 'feedback' in parsed_json:
            return {
                "feedback": json.dumps(parsed_json['feedback']),
                "tutor_message": parsed_json['tutor_message'],
                "needs_correction": any([
                    parsed_json['feedback'].get('unfamiliar_words', []),
                    parsed_json['feedback'].get('grammar_errors', {}),
                    parsed_json['feedback'].get('not_so_good_expressions', {}),
                    parsed_json['feedback'].get('best_fit_words', {})
                ])
            }
    except json.JSONDecodeError:
        print("Failed to parse entire response as JSON", flush=True)
    
    # Fallback
    return {
        "feedback": json.dumps({
            "unfamiliar_words": [],
            "not_so_good_expressions": {},
            "grammar_errors": {},
            "best_fit_words": {}
        }),
        "needs_correction": False
    }

def extract_partner_message(response: str) -> dict:
    """Extract message from partner response"""
    import json
    
    print(f"Attempting to extract partner message from response of length {len(response)}", flush=True)
    
    try:
        # Try to parse as JSON first in case it's wrapped
        parsed_json = json.loads(response)
        if isinstance(parsed_json, dict) and 'message' in parsed_json:
            return {"message": parsed_json['message']}
    except json.JSONDecodeError:
        # If it's not JSON, use the raw response as the message
        return {"message": response.strip()}

def handle_tutor_feedback(session, scene, user_input):
    """Process feedback from the tutor"""
    try:
        # Get conversation history
        messages = Message.query.filter_by(session_id=session.id).order_by(Message.timestamp).all()
        conversation_history = "\n".join([f"{msg.role}: {msg.text}" for msg in messages])
        
        # Prepare scene info for tutor prompt
        scene_info = {
            "title": scene.name,
            "description": scene.description,
            "vocabulary": get_scene_vocabulary(scene),
            "phrases": [],
            "questions": []
        }
        
        # Generate tutor prompt
        prompt = Prompts.generate_tutor_prompt(
            user_level="B1",  # TODO: Get actual user level
            scene_context=scene_info,
            conversation_history=conversation_history,
            user_input=user_input
        )
        
        # Get AI response
        ai_response = llm_client.get_completion(prompt, user_input)
        print(f"\n=== LLM TUTOR RESPONSE ===\n{ai_response}\n===================\n", flush=True)
        
        # Parse feedback using tutor-specific function
        return jsonify(extract_tutor_feedback(ai_response))

    except Exception as e:
        print(f"Error in tutor feedback: {str(e)}", flush=True)
        raise

def handle_partner_chat(session, scene, user_input):
    """Process chat with the conversation partner"""
    try:
        # Get conversation history
        messages = Message.query.filter_by(session_id=session.id).order_by(Message.timestamp).all()
        conversation_history = "\n".join([f"{msg.role}: {msg.text}" for msg in messages])
        
        # Prepare scene data for prompt
        scene_data = {
            "title": scene.name,
            "description": scene.description,
            "vocabulary": get_scene_vocabulary(scene),
            "phrases": [],
            "questions": []
        }
        
        # Generate partner prompt
        prompt = Prompts.generate_partner_prompt(
            scene=scene_data,
            conversation_history=conversation_history
        )
        
        # Get AI response
        ai_response = llm_client.get_completion(prompt, user_input)
        print(f"\n=== LLM PARTNER RESPONSE ===\n{ai_response}\n===================\n", flush=True)
        
        # Parse message using partner-specific function
        response_data = extract_partner_message(ai_response)
        
        # Save AI message
        ai_message = Message(
            session_id=session.id,
            role='assistant',
            text=response_data['message']
        )
        db.session.add(ai_message)
        db.session.commit()
        
        return jsonify(response_data)

    except Exception as e:
        print(f"Error in partner chat: {str(e)}", flush=True)
        print(f"Error details: {traceback.format_exc()}", flush=True)
        db.session.rollback()
        return jsonify({
            "error": "An unexpected error occurred while processing your message.",
            "details": str(e)
        }), 500

@bp.route('/conversation/tutor', methods=['POST'])
def process_tutor_feedback():
    print("\n=== TUTOR ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Request data: {data}", flush=True)
    
    # Validate input data
    required_fields = ['session_id', 'user_id', 'scene_id', 'user_input']
    missing_fields = [field for field in required_fields if field not in data]
    
    if missing_fields:
        error_msg = f"Missing required fields: {', '.join(missing_fields)}"
        print(f"ERROR: {error_msg}", flush=True)
        return jsonify({"error": error_msg}), 400
    
    try:
        # Get scene and session
        scene = Scene.query.get_or_404(data['scene_id'])
        session = ConversationSession.query.get_or_404(data['session_id'])
        print(f"Found scene: {scene.name}", flush=True)
        
        # Save user message (but don't save to conversation history yet)
        user_message = Message(
            session_id=data['session_id'],
            role='user',
            text=data['user_input']
        )
        db.session.add(user_message)
        db.session.commit()

        return handle_tutor_feedback(session, scene, data['user_input'])

    except Exception as e:
        print(f"Unexpected error in tutor feedback: {str(e)}", flush=True)
        print(f"Error details: {traceback.format_exc()}", flush=True)
        db.session.rollback()
        return jsonify({
            "error": "An unexpected error occurred while processing your message.",
            "details": str(e)
        }), 500

@bp.route('/conversation/partner', methods=['POST'])
def process_partner_message():
    print("\n=== PARTNER ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Request data: {data}", flush=True)
    
    # Validate input data
    required_fields = ['session_id', 'user_id', 'scene_id', 'user_input']
    missing_fields = [field for field in required_fields if field not in data]
    
    if missing_fields:
        error_msg = f"Missing required fields: {', '.join(missing_fields)}"
        print(f"ERROR: {error_msg}", flush=True)
        return jsonify({"error": error_msg}), 400
    
    try:
        # Get scene and session
        scene = Scene.query.get_or_404(data['scene_id'])
        session = ConversationSession.query.get_or_404(data['session_id'])
        print(f"Found scene: {scene.name}", flush=True)

        return handle_partner_chat(session, scene, data['user_input'])

    except Exception as e:
        print(f"Unexpected error in partner chat: {str(e)}", flush=True)
        print(f"Error details: {traceback.format_exc()}", flush=True)
        db.session.rollback()
        return jsonify({
            "error": "An unexpected error occurred while processing your message.",
            "details": str(e)
        }), 500

@bp.route('/conversation/session', methods=['POST'])
def create_session():
    data = request.get_json()
    if not data or 'user_id' not in data or 'scene_id' not in data:
        return jsonify({"error": "user_id and scene_id are required"}), 400
    
    try:
        new_session = ConversationSession(
            person_id=data['user_id'],  # Map user_id to person_id
            scene_id=data['scene_id']
        )
        db.session.add(new_session)
        db.session.commit()
        
        return jsonify({
            "id": str(new_session.id),
            "user_id": new_session.person_id,  # Return user_id
            "scene_id": new_session.scene_id,
            "started_at": new_session.started_at.isoformat()
        }), 201
        
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

def get_scene_vocabulary(scene: Scene) -> List[str]:
    """Get vocabulary for a scene from its scene levels"""
    scene_level = SceneLevel.query.filter_by(scene_id=scene.id).first()
    if scene_level and scene_level.key_phrases:
        return scene_level.key_phrases.split(",")
    return []

# ... other conversation-related routes ... 