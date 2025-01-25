from flask import jsonify, request, current_app, Blueprint
import json
from app import db
from app.models import (
    ConversationSession, Message, Scene,
    UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression
)
from app.llm.client import LLMClient
from app.llm.prompts import Prompts
import logging
import re

# Set up logger
logger = logging.getLogger(__name__)

print("Creating LLM client instance")  # Debug print
llm_client = LLMClient()

# Create blueprint with url_prefix
bp = Blueprint('conversation', __name__, url_prefix='/api')

def fix_json_response(response_str: str) -> str:
    """Fix common JSON formatting issues in LLM responses."""
    # Fix the "or" syntax in grammar errors
    if '"grammar_errors"' in response_str:
        # Replace 'A" or "B' with just 'A'
        response_str = re.sub(r'"([^"]+)" or "[^"]+"', r'"\1"', response_str)
    return response_str

def extract_json_from_response(response: str) -> dict:
    """Extract JSON from LLM response that might be wrapped in markdown."""
    # Look for JSON between triple backticks
    json_match = re.search(r'```json\n(.*?)\n```', response, re.DOTALL)
    if json_match:
        try:
            fixed_json = fix_json_response(json_match.group(1))
            return json.loads(fixed_json)
        except json.JSONDecodeError as e:
            print(f"Failed to parse JSON from markdown: {json_match.group(1)}")
            print(f"JSON error: {str(e)}")
            raise
            
    # Try parsing the whole response as JSON
    try:
        fixed_json = fix_json_response(response)
        return json.loads(fixed_json)
    except json.JSONDecodeError as e:
        print(f"Failed to parse response as JSON: {response}")
        print(f"JSON error: {str(e)}")
        raise

@bp.route('/conversation/chat', methods=['POST'])
def process_chat():
    print("\n=== CHAT ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Request data: {data}", flush=True)
    
    if not data or not all(k in data for k in ('session_id', 'user_id', 'scene_id', 'user_input')):
        print("ERROR: Missing required fields", flush=True)
        return jsonify({"error": "session_id, user_id, scene_id, and user_input are required"}), 400
    
    try:
        # Get scene and session
        scene = Scene.query.get_or_404(data['scene_id'])
        session = ConversationSession.query.get_or_404(data['session_id'])
        print(f"Found scene: {scene.name}", flush=True)
        
        # Save user message
        user_message = Message(
            session_id=data['session_id'],
            role='user',
            text=data['user_input']
        )
        db.session.add(user_message)
        db.session.commit()
        
        # Get conversation history and generate response
        messages = Message.query.filter_by(session_id=data['session_id']).order_by(Message.timestamp).all()
        conversation_history = "\n".join([f"{msg.role}: {msg.text}" for msg in messages])
        
        # Prepare scene data for prompt
        scene_data = {
            "title": scene.name,
            "setting": scene.context,
            "vocabulary": scene.key_phrases.split(",") if scene.key_phrases else [],
            "phrases": [],
            "questions": []
        }
        
        # Generate prompt
        prompt = Prompts.generate_tutor_prompt(
            scene=scene_data,
            conversation_history=conversation_history,
            tutor_tasks=Prompts.tutor_tasks_new_user
        )
        
        # Get AI response and parse it
        ai_response = llm_client.get_completion(prompt, data['user_input'])
        print(f"Raw AI response: {ai_response}", flush=True)
        response_data = extract_json_from_response(ai_response)
        print(f"Parsed response: {response_data}", flush=True)
        
        # Save AI message
        ai_message = Message(
            session_id=data['session_id'],
            role='assistant',
            text=response_data['conversation']
        )
        db.session.add(ai_message)
        db.session.commit()
        
        # Transform response format
        transformed_response = {
            "message": response_data["conversation"],
            "feedback": response_data["feedback"]
        }
        
        return jsonify(transformed_response)
        
    except Exception as e:
        print(f"Error in process_chat: {str(e)}", flush=True)
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@bp.route('/conversation/session', methods=['POST'])
def create_session():
    data = request.get_json()
    if not data or 'person_id' not in data or 'scene_id' not in data:
        return jsonify({"error": "person_id and scene_id are required"}), 400
    
    try:
        new_session = ConversationSession(
            person_id=data['person_id'],
            scene_id=data['scene_id']
        )
        db.session.add(new_session)
        db.session.commit()
        
        return jsonify({
            "id": str(new_session.id),
            "person_id": new_session.person_id,
            "scene_id": new_session.scene_id,
            "started_at": new_session.started_at.isoformat()
        }), 201
        
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@bp.route('/test', methods=['GET'])
def test_endpoint():
    # Test both logging and print
    current_app.logger.info("Test endpoint hit! (from logger)")
    print("Test endpoint hit! (from print)", flush=True)
    return jsonify({"message": "Test endpoint working"})

@bp.route('/test-post', methods=['POST'])
def test_post():
    print("\n=== TEST POST ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Received POST data: {data}", flush=True)
    return jsonify({"message": "Test POST working", "received": data})

# ... other conversation-related routes ... 