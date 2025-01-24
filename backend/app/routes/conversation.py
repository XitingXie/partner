from flask import jsonify, request, current_app
import json
from app import db
from app.models import (
    ConversationSession, Message, Scene,
    UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression
)
from app.llm.client import LLMClient
from app.llm.prompts import Prompts
from . import main

print("Creating LLM client instance")  # Debug print
llm_client = LLMClient()

@main.route('/conversation/chat', methods=['POST'])
def process_chat():
    print("\n=== CHAT ENDPOINT CALLED ===")
    print("Received chat request")
    data = request.get_json()
    print(f"Request data: {data}")
    
    if not data or not all(k in data for k in ('session_id', 'user_id', 'scene_id', 'user_input')):
        print("ERROR: Missing required fields")
        return jsonify({"error": "session_id, user_id, scene_id, and user_input are required"}), 400
    
    try:
        print("\n=== PROCESSING CHAT REQUEST ===")
        # Get the scene and session
        scene = Scene.query.get_or_404(data['scene_id'])
        print(f"Found scene: {scene.name}")
        session = ConversationSession.query.get_or_404(data['session_id'])
        print(f"Found session: {session.id}")
        
        # Get conversation history
        messages = Message.query.filter_by(session_id=session.id).order_by(Message.timestamp).all()
        conversation_history = "\n".join([f"{msg.role}: {msg.text}" for msg in messages])
        print("Conversation history:", conversation_history)  # Debug print
        
        # Prepare scene data for the prompt
        scene_data = {
            "title": scene.name,
            "setting": scene.context,
            "vocabulary": scene.key_phrases.split(",") if scene.key_phrases else [],
            "phrases": [],  # You might want to add this to your Scene model
            "questions": []  # You might want to add this to your Scene model
        }
        print("Scene data:", scene_data)  # Debug print
        
        # Generate prompt and get AI response
        prompt = Prompts.generate_tutor_prompt(
            scene=scene_data,
            conversation_history=conversation_history,
            tutor_tasks=Prompts.tutor_tasks_new_user
        )
        print("Generated prompt:", prompt)  # Debug print
        
        print("Calling LLM client get_completion")  # Debug print
        try:
            ai_response = llm_client.get_completion(prompt)
            print("AI response:", ai_response)
            
            # Extract JSON part from the response
            json_str = ai_response[ai_response.find('{'):ai_response.rfind('}')+1]
            print("Extracted JSON:", json_str)
            
            response_data = json.loads(json_str)
            print("Parsed response data:", response_data)
        except Exception as e:
            print(f"Error getting AI response: {str(e)}")
            return jsonify({
                "error": "Failed to get AI response",
                "details": str(e)
            }), 500
        
        try:
            # Save user message
            user_message = Message(
                session_id=session.id,
                role="user",
                text=data['user_input'],
                voice=data.get('voice', None)
            )
            db.session.add(user_message)
            print(f"Saved user message: {user_message}")

            # Save AI response
            ai_message = Message(
                session_id=session.id,
                role="assistant",
                text=response_data["conversation"],
                voice=None
            )
            db.session.add(ai_message)
            print(f"Saved AI message: {ai_message}")

            # Commit messages first
            db.session.commit()
            print("Messages committed to database")

            # Verify messages were saved
            saved_messages = Message.query.filter_by(session_id=session.id).order_by(Message.timestamp).all()
            print(f"All messages in session {session.id}:", saved_messages)

            # Now save learning data
            try:
                # Save unfamiliar words
                for word in response_data["feedback"]["unfamiliar_words"]:
                    unfamiliar = UnfamiliarWord(
                        word=word,
                        session_id=session.id,
                        person_id=data['user_id'],
                        definition=None,
                        example=None
                    )
                    db.session.add(unfamiliar)
                
                # Grammar errors
                for incorrect, correct in response_data["feedback"]["grammar_errors"].items():
                    grammar = WrongGrammar(
                        incorrect_text=incorrect,
                        correct_text=correct,
                        session_id=session.id,
                        person_id=data['user_id']
                    )
                    db.session.add(grammar)
                
                # Better expressions
                for original, better in response_data["feedback"]["not_so_good_expressions"].items():
                    expression = BetterExpression(
                        original_text=original,
                        better_text=better,
                        session_id=session.id,
                        person_id=data['user_id']
                    )
                    db.session.add(expression)
                
                # Best fit words
                for original, better in response_data["feedback"]["best_fit_words"].items():
                    best_fit = BestFitWord(
                        original_word=original,
                        better_word=better,
                        context=data['user_input'],
                        session_id=session.id,
                        person_id=data['user_id']
                    )
                    db.session.add(best_fit)
                
                # Commit learning data
                db.session.commit()
                print("Learning data committed to database")
                
            except Exception as e:
                print(f"Error saving learning data: {str(e)}")
                db.session.rollback()
                # Continue even if learning data fails
            
            return jsonify({
                "message": response_data["conversation"],
                "feedback": response_data["feedback"]
            })
            
        except Exception as e:
            print(f"Error saving messages: {str(e)}")
            db.session.rollback()
            raise
        
    except Exception as e:
        print(f"Error in process_chat: {str(e)}")  # Debug print
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/session', methods=['POST'])
def create_session():
    data = request.get_json()
    if not data or 'person_id' not in data:
        return jsonify({"error": "Person ID is required"}), 400
    
    new_session = ConversationSession(
        person_id=data['person_id'],
        scene_id=data.get('scene_id')
    )
    try:
        db.session.add(new_session)
        db.session.commit()
        return jsonify({
            "id": new_session.id,
            "person_id": new_session.person_id,
            "scene_id": new_session.scene_id,
            "started_at": new_session.started_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/test', methods=['GET'])
def test_endpoint():
    current_app.logger.info("Test endpoint hit!")
    print("Test endpoint hit!", flush=True)
    return jsonify({"message": "Test endpoint working"})

# ... other conversation-related routes ... 