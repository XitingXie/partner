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
import traceback

# Set up logger
logger = logging.getLogger(__name__)

print("Creating LLM client instance")  # Debug print
llm_client = LLMClient()

# Create blueprint with url_prefix
bp = Blueprint('conversation', __name__, url_prefix='/api')


# def extract_json_from_response(response: str) -> dict:
#     """
#     Extract JSON from LLM response with multiple parsing strategies.
    
#     Args:
#         response (str): The full text response from the LLM
    
#     Returns:
#         dict: Extracted JSON data with conversation and feedback keys
#     """
#     import re
#     import json
    
#     print(f"Attempting to extract JSON from response of length {len(response)}", flush=True)
    
#     # Strategy 1: Look for JSON enclosed in markdown code block
#     json_markdown_match = re.search(r'```json\s*({.*?})\s*```', response, re.DOTALL | re.MULTILINE)
#     if json_markdown_match:
#         try:
#             parsed_json = json.loads(json_markdown_match.group(1))
#             if validate_json_structure(parsed_json):
#                 return parse_feedback_json(parsed_json)
#         except json.JSONDecodeError:
#             print("Failed to parse JSON from markdown code block", flush=True)
    
#     # Strategy 2: Look for JSON between first { and last }
#     json_block_match = re.search(r'\{.*\}', response, re.DOTALL)
#     if json_block_match:
#         try:
#             parsed_json = json.loads(json_block_match.group(0))
#             if validate_json_structure(parsed_json):
#                 return parse_feedback_json(parsed_json)
#         except json.JSONDecodeError:
#             print("Failed to parse JSON from block", flush=True)
    
#     # Strategy 3: Attempt to parse the entire response
#     try:
#         parsed_json = json.loads(response)
#         if validate_json_structure(parsed_json):
#             return parse_feedback_json(parsed_json)
#     except json.JSONDecodeError:
#         print("Failed to parse entire response as JSON", flush=True)
    
#     # Fallback: Create a default structure with the full response as conversation
#     print("Falling back to default JSON structure", flush=True)
#     return {
#         "conversation": response,
#         "feedback": json.dumps({
#             "unfamiliar_words": [],
#             "not_so_good_expressions": {},
#             "grammar_errors": {},
#             "best_fit_words": {}
#         })
#     }

# def parse_feedback_json(json_data: dict) -> dict:
#     """
#     Parse the feedback JSON, ensuring it's a JSON-formatted string.
    
#     Args:
#         json_data (dict): Parsed JSON data
    
#     Returns:
#         dict: Processed JSON with feedback as a JSON-formatted string
#     """
#     # If feedback is already a string, try to parse it
#     if isinstance(json_data.get('feedback'), str):
#         try:
#             # Attempt to parse the feedback string as JSON
#             json_data['feedback'] = json.loads(json_data['feedback'])
#         except json.JSONDecodeError:
#             # If parsing fails, create a default feedback structure
#             print("Failed to parse feedback JSON string", flush=True)
#             json_data['feedback'] = {
#                 "unfamiliar_words": [],
#                 "not_so_good_expressions": {},
#                 "grammar_errors": {},
#                 "best_fit_words": {}
#             }
    
#     # Validate the feedback structure
#     if not validate_feedback_structure(json_data['feedback']):
#         # If validation fails, create a default feedback structure
#         json_data['feedback'] = {
#             "unfamiliar_words": [],
#             "not_so_good_expressions": {},
#             "grammar_errors": {},
#             "best_fit_words": {}
#         }
    
#     # Convert feedback back to a JSON-formatted string
#     json_data['feedback'] = json.dumps(json_data['feedback'])
    
#     return json_data

# def validate_json_structure(json_data: dict) -> bool:
#     """
#     Validate that the JSON has the required structure.
    
#     Args:
#         json_data (dict): JSON data to validate
    
#     Returns:
#         bool: Whether the JSON has the correct structure
#     """
#     required_keys = {"conversation", "feedback"}
#     if not all(key in json_data for key in required_keys):
#         print(f"Missing required keys. Found: {set(json_data.keys())}", flush=True)
#         return False
    
#     return True

# def validate_feedback_structure(feedback_data: dict) -> bool:
#     """
#     Validate the structure of the feedback dictionary.
    
#     Args:
#         feedback_data (dict): Feedback data to validate
    
#     Returns:
#         bool: Whether the feedback has the correct structure
#     """
#     feedback_keys = {
#         "unfamiliar_words", 
#         "not_so_good_expressions", 
#         "grammar_errors", 
#         "best_fit_words"
#     }
    
#     if not all(key in feedback_data for key in feedback_keys):
#         print(f"Missing feedback keys. Found: {set(feedback_data.keys())}", flush=True)
#         return False
    
#     return True

def extract_tutor_feedback(response: str) -> dict:
    """Extract feedback JSON from tutor response"""
    import json
    import re
    
    print(f"Attempting to extract tutor feedback from response of length {len(response)}", flush=True)
    
    # First try: Extract JSON from markdown code block
    code_block_match = re.search(r'```json\s*(\{.*?\})\s*```', response, re.DOTALL)
    if code_block_match:
        try:
            json_str = code_block_match.group(1)
            parsed_json = json.loads(json_str)
            if 'feedback' in parsed_json:
                return {
                    "feedback": json.dumps(parsed_json['feedback']),
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
        
        # Prepare scene data for prompt
        scene_data = {
            "title": scene.name,
            "setting": scene.context,
            "vocabulary": scene.key_phrases.split(",") if scene.key_phrases else [],
            "phrases": [],
            "questions": []
        }
        
        # Generate tutor prompt
        prompt = Prompts.generate_tutor_prompt(
            scene=scene_data,
            conversation_history=conversation_history,
            tutor_tasks=Prompts.tutor_tasks_new_user
        )
        
        # Get AI response
        ai_response = llm_client.get_completion(prompt, user_input)
        print(f"Raw AI response length: {len(ai_response)}", flush=True)
        
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
            "setting": scene.context,
            "vocabulary": scene.key_phrases.split(",") if scene.key_phrases else [],
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
        raise

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


# ... other conversation-related routes ... 