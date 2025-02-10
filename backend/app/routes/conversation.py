from flask import jsonify, request, current_app, Blueprint
import json
from app.extensions import mongo
from app.models.mongo_models import ConversationSession, Scene, SceneLevel
from app.llm.client import LLMClient
from app.llm.prompts import Prompts
from app.auth import verify_token, verify_same_user
import logging
import re
import traceback
from typing import List
from bson import ObjectId
from datetime import datetime

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
                        len(parsed_json['feedback'].get('unfamiliar_words', [])) > 0,
                        len(parsed_json['feedback'].get('grammar_errors', {})) > 0,
                        len(parsed_json['feedback'].get('not_so_good_expressions', {})) > 0,
                        len(parsed_json['feedback'].get('best_fit_words', {})) > 0
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
                    len(parsed_json['feedback'].get('unfamiliar_words', [])) > 0,
                    len(parsed_json['feedback'].get('grammar_errors', {})) > 0,
                    len(parsed_json['feedback'].get('not_so_good_expressions', {})) > 0,
                    len(parsed_json['feedback'].get('best_fit_words', {})) > 0
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

def handle_tutor_feedback(session_id, scene_id, user_input, first_language="zh"):
    """Process feedback from the tutor"""
    try:
        # Get conversation history
        session = mongo.db.conversation_sessions.find_one({'_id': ObjectId(session_id)})
        if not session:
            raise ValueError("Session not found")
            
        messages = session.get('messages', [])
        conversation_history = "\n".join([f"{msg['role']}: {msg['text']}" for msg in messages])
        
        # Get scene info
        scene = mongo.db.scenes.find_one({'_id': ObjectId(scene_id)})
        if not scene:
            raise ValueError("Scene not found")
            
        # Get scene level info
        scene_level = mongo.db.scene_levels.find_one({
            'scene_id': ObjectId(scene_id),
            'english_level': 'B1'  # TODO: Get actual user level
        })
        
        # Prepare scene info for tutor prompt
        scene_info = {
            "title": scene['name'],
            "description": scene.get('description'),
            "vocabulary": scene_level.get('vocabulary', '').split(',') if scene_level and scene_level.get('vocabulary') else [],
            "phrases": [],
            "questions": []
        }
        
        # Generate tutor prompt
        prompt = Prompts.generate_tutor_prompt(
            user_level="B1",  # TODO: Get actual user level
            scene_context=scene_info,
            conversation_history=conversation_history,
            user_input=user_input,
            first_language=first_language
        )
        
        # Get AI response
        ai_response = llm_client.get_completion(prompt, user_input, role="tutor")
        print(f"\n=== LLM TUTOR RESPONSE ===\n{ai_response}\n===================\n", flush=True)
        
        # Parse feedback using tutor-specific function
        return jsonify(extract_tutor_feedback(ai_response))

    except Exception as e:
        print(f"Error in tutor feedback: {str(e)}", flush=True)
        raise

def handle_partner_chat(session_id, scene_id, user_input, user_level):
    """Process chat with the conversation partner"""
    try:
        # Get conversation history
        session = mongo.db.conversation_sessions.find_one({'_id': ObjectId(session_id)})
        if not session:
            raise ValueError("Session not found")
            
        messages = session.get('messages', [])
        conversation_history = "\n".join([f"{msg['role']}: {msg['text']}" for msg in messages])
        
        # Get scene info
        scene = mongo.db.scenes.find_one({'_id': ObjectId(scene_id)})
        if not scene:
            raise ValueError("Scene not found")
            
        # Get scene level info
        scene_level = mongo.db.scene_levels.find_one({
            'scene_id': ObjectId(scene_id),
            'english_level': user_level.upper()
        })
        
        # Prepare scene data for prompt
        scene_data = {
            "title": scene['name'],
            "description": scene.get('description'),
            "vocabulary": scene_level.get('vocabulary', '').split(',') if scene_level and scene_level.get('vocabulary') else [],
            "phrases": [],
            "questions": []
        }
        
        # Generate partner prompt
        prompt = Prompts.generate_partner_prompt(
            user_level=user_level,
            scene=scene_data,
            conversation_history=conversation_history
        )
        
        # Get AI response
        ai_response = llm_client.get_completion(prompt, user_input, role="partner")
        print(f"\n=== LLM PARTNER RESPONSE ===\n{ai_response}\n===================\n", flush=True)
        
        # Parse message using partner-specific function
        response_data = extract_partner_message(ai_response)
        
        # Save AI message to session
        new_message = {
            'role': 'assistant',
            'text': response_data['message'],
            'timestamp': datetime.utcnow()
        }
        
        mongo.db.conversation_sessions.update_one(
            {'_id': ObjectId(session_id)},
            {'$push': {'messages': new_message}}
        )
        
        return jsonify(response_data)

    except Exception as e:
        print(f"Error in partner chat: {str(e)}", flush=True)
        print(f"Error details: {traceback.format_exc()}", flush=True)
        return jsonify({
            "error": "An unexpected error occurred while processing your message.",
            "details": str(e)
        }), 500

@bp.route('/conversation/tutor', methods=['POST'])
@verify_token
@verify_same_user
def process_tutor_feedback():
    print("\n=== TUTOR ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Request data: {data}", flush=True)
    
    # Validate input data
    required_fields = ['session_id', 'uid', 'scene_id', 'user_input']
    missing_fields = [field for field in required_fields if field not in data]
    
    if missing_fields:
        error_msg = f"Missing required fields: {', '.join(missing_fields)}"
        print(f"ERROR: {error_msg}", flush=True)
        return jsonify({"error": error_msg}), 400
    
    try:
        # Save user message
        new_message = {
            'role': 'user',
            'text': data['user_input'],
            'timestamp': datetime.utcnow()
        }
        
        mongo.db.conversation_sessions.update_one(
            {'_id': ObjectId(data['session_id'])},
            {'$push': {'messages': new_message}}
        )

        # Get first_language from request data, default to "zh" if not provided
        first_language = data.get('first_language', 'zh')
        return handle_tutor_feedback(data['session_id'], data['scene_id'], data['user_input'], first_language)

    except Exception as e:
        print(f"Unexpected error in tutor feedback: {str(e)}", flush=True)
        print(f"Error details: {traceback.format_exc()}", flush=True)
        return jsonify({
            "error": "An unexpected error occurred while processing your message.",
            "details": str(e)
        }), 500

@bp.route('/conversation/partner', methods=['POST'])
@verify_token
@verify_same_user
def process_partner_message():
    print("\n=== PARTNER ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Request data: {data}", flush=True)
    
    # Validate input data
    required_fields = ['session_id', 'uid', 'scene_id', 'user_input']
    missing_fields = [field for field in required_fields if field not in data]
    
    if missing_fields:
        error_msg = f"Missing required fields: {', '.join(missing_fields)}"
        print(f"ERROR: {error_msg}", flush=True)
        return jsonify({"error": error_msg}), 400
    
    try:
        return handle_partner_chat(data['session_id'], data['scene_id'], data['user_input'], "B1")

    except Exception as e:
        print(f"Unexpected error in partner chat: {str(e)}", flush=True)
        print(f"Error details: {traceback.format_exc()}", flush=True)
        return jsonify({
            "error": "An unexpected error occurred while processing your message.",
            "details": str(e)
        }), 500

@bp.route('/conversation/session', methods=['POST'])
@verify_token
@verify_same_user
def create_session():
    data = request.get_json()
    if not data or 'uid' not in data or 'scene_id' not in data:
        return jsonify({"error": "uid and scene_id are required"}), 400
    
    try:
        new_session = ConversationSession(
            user_uid=data['uid'],
            scene_id=data['scene_id']
        )
        
        result = mongo.db.conversation_sessions.insert_one(new_session.to_dict())
        session_id = result.inserted_id
        
        return jsonify({
            "id": str(session_id),
            "uid": new_session.user_uid,
            "scene_id": new_session.scene_id,
            "started_at": new_session.started_at.isoformat()
        }), 201
        
    except Exception as e:
        return jsonify({"error": str(e)}), 400

def get_scene_vocabulary(scene: Scene) -> List[str]:
    """Get vocabulary for a scene from its scene levels"""
    scene_level = SceneLevel.query.filter_by(scene_id=scene.id).first()
    if scene_level and scene_level.key_phrases:
        return scene_level.key_phrases.split(",")
    return []

# ... other conversation-related routes ... 