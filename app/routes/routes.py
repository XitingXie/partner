from flask import Blueprint, jsonify, request
from app import db
from app.models import Person, ConversationSession, Message, UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression, Topic, Scene
from datetime import datetime
from app.llm.client import LLMClient

main = Blueprint('main', __name__)

# Initialize LLM client
llm_client = LLMClient()

@main.route('/')
def index():
    return jsonify({"message": "Welcome to the Flask API!"})

@main.route('/health')
def health_check():
    return jsonify({"status": "healthy"})

# Person endpoints
@main.route('/person', methods=['POST'])
def create_person():
    data = request.get_json()
    if not data or 'name' not in data:
        return jsonify({"error": "Name is required"}), 400
    
    new_person = Person(name=data['name'])
    try:
        db.session.add(new_person)
        db.session.commit()
        return jsonify({
            "id": new_person.id,
            "name": new_person.name,
            "created_at": new_person.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/person/<int:person_id>', methods=['GET'])
def get_person(person_id):
    person = Person.query.get_or_404(person_id)
    return jsonify({
        "id": person.id,
        "name": person.name,
        "created_at": person.created_at
    })

@main.route('/person/<int:person_id>', methods=['PUT'])
def update_person(person_id):
    person = Person.query.get_or_404(person_id)
    data = request.get_json()
    
    if 'name' in data:
        person.name = data['name']
        
    try:
        db.session.commit()
        return jsonify({
            "id": person.id,
            "name": person.name,
            "created_at": person.created_at
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/person/<int:person_id>', methods=['DELETE'])
def delete_person(person_id):
    person = Person.query.get_or_404(person_id)
    try:
        db.session.delete(person)
        db.session.commit()
        return jsonify({"message": "Person deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

# ConversationSession endpoints
@main.route('/session', methods=['POST'])
def create_session():
    data = request.get_json()
    if not data or 'person_id' not in data:
        return jsonify({"error": "Person ID is required"}), 400
    
    new_session = ConversationSession(
        person_id=data['person_id'],
        scene_id=data.get('scene_id')  # Optional scene_id
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

@main.route('/session/<int:session_id>', methods=['GET'])
def get_session(session_id):
    session = ConversationSession.query.get_or_404(session_id)
    return jsonify({
        "id": session.id,
        "person_id": session.person_id,
        "scene_id": session.scene_id,
        "started_at": session.started_at
    })

# Message endpoints
@main.route('/message', methods=['POST'])
def create_message():
    data = request.get_json()
    if not data or not all(k in data for k in ('session_id', 'role', 'text')):
        return jsonify({"error": "session_id, role, and text are required"}), 400
    
    # If it's a user message, get AI response
    if data['role'] == 'user':
        session = ConversationSession.query.get_or_404(data['session_id'])
        previous_messages = Message.query.filter_by(session_id=data['session_id']).order_by(Message.timestamp).all()
        
        # Get LLM response
        llm_response = llm_client.generate_response(
            messages=[msg.text for msg in previous_messages] + [data['text']],
            scene_id=session.scene_id
        )
        
        # Create user message
        user_message = Message(
            session_id=data['session_id'],
            role='user',
            text=data['text'],
            voice=data.get('voice')
        )
        
        # Create AI response message
        ai_message = Message(
            session_id=data['session_id'],
            role='ai',
            text=llm_response['response'],
            voice=None  # You might want to generate voice here
        )
        
        try:
            db.session.add(user_message)
            db.session.add(ai_message)
            db.session.commit()
            
            # Process any analysis from LLM
            if 'analysis' in llm_response:
                # Add unfamiliar words, grammar mistakes, etc.
                # Implementation depends on your needs
                pass
            
            return jsonify({
                "user_message": {
                    "id": user_message.id,
                    "text": user_message.text,
                    "timestamp": user_message.timestamp
                },
                "ai_message": {
                    "id": ai_message.id,
                    "text": ai_message.text,
                    "timestamp": ai_message.timestamp
                }
            }), 201
        except Exception as e:
            db.session.rollback()
            return jsonify({"error": str(e)}), 400

@main.route('/message/<int:message_id>', methods=['GET'])
def get_message(message_id):
    message = Message.query.get_or_404(message_id)
    return jsonify({
        "id": message.id,
        "session_id": message.session_id,
        "role": message.role,
        "text": message.text,
        "voice": message.voice,
        "timestamp": message.timestamp
    })

# Query endpoints
@main.route('/person/<int:person_id>/sessions', methods=['GET'])
def get_person_sessions(person_id):
    Person.query.get_or_404(person_id)  # Verify person exists
    sessions = ConversationSession.query.filter_by(person_id=person_id).all()
    return jsonify([{
        "id": session.id,
        "started_at": session.started_at
    } for session in sessions])

@main.route('/session/<int:session_id>/messages', methods=['GET'])
def get_session_messages(session_id):
    ConversationSession.query.get_or_404(session_id)  # Verify session exists
    messages = Message.query.filter_by(session_id=session_id).order_by(Message.timestamp).all()
    return jsonify([{
        "id": message.id,
        "role": message.role,
        "text": message.text,
        "voice": message.voice,
        "timestamp": message.timestamp
    } for message in messages])

# UnfamiliarWord endpoints
@main.route('/unfamiliar-word', methods=['POST'])
def create_unfamiliar_word():
    data = request.get_json()
    if not data or not all(k in data for k in ('word', 'session_id')):
        return jsonify({"error": "Word and session_id are required"}), 400
    
    new_word = UnfamiliarWord(
        word=data['word'],
        definition=data.get('definition'),
        example=data.get('example'),
        session_id=data['session_id']
    )
    
    try:
        db.session.add(new_word)
        db.session.commit()
        return jsonify({
            "id": new_word.id,
            "word": new_word.word,
            "definition": new_word.definition,
            "example": new_word.example,
            "session_id": new_word.session_id,
            "created_at": new_word.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/session/<int:session_id>/unfamiliar-words', methods=['GET'])
def get_session_unfamiliar_words(session_id):
    ConversationSession.query.get_or_404(session_id)  # Verify session exists
    words = UnfamiliarWord.query.filter_by(session_id=session_id).order_by(UnfamiliarWord.created_at).all()
    return jsonify([{
        "id": word.id,
        "word": word.word,
        "definition": word.definition,
        "example": word.example,
        "created_at": word.created_at
    } for word in words])

@main.route('/unfamiliar-word/<int:word_id>', methods=['GET'])
def get_unfamiliar_word(word_id):
    word = UnfamiliarWord.query.get_or_404(word_id)
    return jsonify({
        "id": word.id,
        "word": word.word,
        "definition": word.definition,
        "example": word.example,
        "created_at": word.created_at
    })

@main.route('/unfamiliar-words', methods=['GET'])
def get_all_unfamiliar_words():
    words = UnfamiliarWord.query.order_by(UnfamiliarWord.word).all()
    return jsonify([{
        "id": word.id,
        "word": word.word,
        "definition": word.definition,
        "example": word.example,
        "created_at": word.created_at
    } for word in words])

@main.route('/unfamiliar-word/<int:word_id>', methods=['PUT'])
def update_unfamiliar_word(word_id):
    word = UnfamiliarWord.query.get_or_404(word_id)
    data = request.get_json()
    
    if 'word' in data:
        word.word = data['word']
    if 'definition' in data:
        word.definition = data['definition']
    if 'example' in data:
        word.example = data['example']
    
    try:
        db.session.commit()
        return jsonify({
            "id": word.id,
            "word": word.word,
            "definition": word.definition,
            "example": word.example,
            "created_at": word.created_at
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/unfamiliar-word/<int:word_id>', methods=['DELETE'])
def delete_unfamiliar_word(word_id):
    word = UnfamiliarWord.query.get_or_404(word_id)
    try:
        db.session.delete(word)
        db.session.commit()
        return jsonify({"message": "Unfamiliar word deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

# WrongGrammar endpoints
@main.route('/wrong-grammar', methods=['POST'])
def create_wrong_grammar():
    data = request.get_json()
    if not data or not all(k in data for k in ('incorrect_text', 'correct_text', 'session_id')):
        return jsonify({"error": "incorrect_text, correct_text, and session_id are required"}), 400
    
    new_grammar = WrongGrammar(
        incorrect_text=data['incorrect_text'],
        correct_text=data['correct_text'],
        explanation=data.get('explanation'),
        session_id=data['session_id']
    )
    
    try:
        db.session.add(new_grammar)
        db.session.commit()
        return jsonify({
            "id": new_grammar.id,
            "incorrect_text": new_grammar.incorrect_text,
            "correct_text": new_grammar.correct_text,
            "explanation": new_grammar.explanation,
            "session_id": new_grammar.session_id,
            "created_at": new_grammar.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/wrong-grammar/<int:grammar_id>', methods=['GET'])
def get_wrong_grammar(grammar_id):
    grammar = WrongGrammar.query.get_or_404(grammar_id)
    return jsonify({
        "id": grammar.id,
        "incorrect_text": grammar.incorrect_text,
        "correct_text": grammar.correct_text,
        "explanation": grammar.explanation,
        "session_id": grammar.session_id,
        "created_at": grammar.created_at
    })

@main.route('/session/<int:session_id>/wrong-grammar', methods=['GET'])
def get_session_wrong_grammar(session_id):
    ConversationSession.query.get_or_404(session_id)  # Verify session exists
    grammar_mistakes = WrongGrammar.query.filter_by(session_id=session_id).order_by(WrongGrammar.created_at).all()
    return jsonify([{
        "id": grammar.id,
        "incorrect_text": grammar.incorrect_text,
        "correct_text": grammar.correct_text,
        "explanation": grammar.explanation,
        "created_at": grammar.created_at
    } for grammar in grammar_mistakes])

@main.route('/wrong-grammar/<int:grammar_id>', methods=['PUT'])
def update_wrong_grammar(grammar_id):
    grammar = WrongGrammar.query.get_or_404(grammar_id)
    data = request.get_json()
    
    if 'incorrect_text' in data:
        grammar.incorrect_text = data['incorrect_text']
    if 'correct_text' in data:
        grammar.correct_text = data['correct_text']
    if 'explanation' in data:
        grammar.explanation = data['explanation']
    
    try:
        db.session.commit()
        return jsonify({
            "id": grammar.id,
            "incorrect_text": grammar.incorrect_text,
            "correct_text": grammar.correct_text,
            "explanation": grammar.explanation,
            "session_id": grammar.session_id,
            "created_at": grammar.created_at
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/wrong-grammar/<int:grammar_id>', methods=['DELETE'])
def delete_wrong_grammar(grammar_id):
    grammar = WrongGrammar.query.get_or_404(grammar_id)
    try:
        db.session.delete(grammar)
        db.session.commit()
        return jsonify({"message": "Grammar mistake deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

# BestFitWord endpoints
@main.route('/best-fit-word', methods=['POST'])
def create_best_fit_word():
    data = request.get_json()
    if not data or not all(k in data for k in ('context', 'original_word', 'better_word', 'session_id')):
        return jsonify({"error": "context, original_word, better_word, and session_id are required"}), 400
    
    new_best_fit = BestFitWord(
        context=data['context'],
        original_word=data['original_word'],
        better_word=data['better_word'],
        explanation=data.get('explanation'),
        session_id=data['session_id']
    )
    
    try:
        db.session.add(new_best_fit)
        db.session.commit()
        return jsonify({
            "id": new_best_fit.id,
            "context": new_best_fit.context,
            "original_word": new_best_fit.original_word,
            "better_word": new_best_fit.better_word,
            "explanation": new_best_fit.explanation,
            "session_id": new_best_fit.session_id,
            "created_at": new_best_fit.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/best-fit-word/<int:word_id>', methods=['GET'])
def get_best_fit_word(word_id):
    best_fit = BestFitWord.query.get_or_404(word_id)
    return jsonify({
        "id": best_fit.id,
        "context": best_fit.context,
        "original_word": best_fit.original_word,
        "better_word": best_fit.better_word,
        "explanation": best_fit.explanation,
        "session_id": best_fit.session_id,
        "created_at": best_fit.created_at
    })

@main.route('/session/<int:session_id>/best-fit-words', methods=['GET'])
def get_session_best_fit_words(session_id):
    ConversationSession.query.get_or_404(session_id)  # Verify session exists
    best_fits = BestFitWord.query.filter_by(session_id=session_id).order_by(BestFitWord.created_at).all()
    return jsonify([{
        "id": best_fit.id,
        "context": best_fit.context,
        "original_word": best_fit.original_word,
        "better_word": best_fit.better_word,
        "explanation": best_fit.explanation,
        "created_at": best_fit.created_at
    } for best_fit in best_fits])

@main.route('/best-fit-word/<int:word_id>', methods=['PUT'])
def update_best_fit_word(word_id):
    best_fit = BestFitWord.query.get_or_404(word_id)
    data = request.get_json()
    
    if 'context' in data:
        best_fit.context = data['context']
    if 'original_word' in data:
        best_fit.original_word = data['original_word']
    if 'better_word' in data:
        best_fit.better_word = data['better_word']
    if 'explanation' in data:
        best_fit.explanation = data['explanation']
    
    try:
        db.session.commit()
        return jsonify({
            "id": best_fit.id,
            "context": best_fit.context,
            "original_word": best_fit.original_word,
            "better_word": best_fit.better_word,
            "explanation": best_fit.explanation,
            "session_id": best_fit.session_id,
            "created_at": best_fit.created_at
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/best-fit-word/<int:word_id>', methods=['DELETE'])
def delete_best_fit_word(word_id):
    best_fit = BestFitWord.query.get_or_404(word_id)
    try:
        db.session.delete(best_fit)
        db.session.commit()
        return jsonify({"message": "Best fit word deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

# BetterExpression endpoints
@main.route('/better-expression', methods=['POST'])
def create_better_expression():
    data = request.get_json()
    if not data or not all(k in data for k in ('original_text', 'better_text', 'session_id')):
        return jsonify({"error": "original_text, better_text, and session_id are required"}), 400
    
    new_expression = BetterExpression(
        original_text=data['original_text'],
        better_text=data['better_text'],
        context=data.get('context'),
        explanation=data.get('explanation'),
        session_id=data['session_id']
    )
    
    try:
        db.session.add(new_expression)
        db.session.commit()
        return jsonify({
            "id": new_expression.id,
            "original_text": new_expression.original_text,
            "better_text": new_expression.better_text,
            "context": new_expression.context,
            "explanation": new_expression.explanation,
            "session_id": new_expression.session_id,
            "created_at": new_expression.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/better-expression/<int:expression_id>', methods=['GET'])
def get_better_expression(expression_id):
    expression = BetterExpression.query.get_or_404(expression_id)
    return jsonify({
        "id": expression.id,
        "original_text": expression.original_text,
        "better_text": expression.better_text,
        "context": expression.context,
        "explanation": expression.explanation,
        "session_id": expression.session_id,
        "created_at": expression.created_at
    })

@main.route('/session/<int:session_id>/better-expressions', methods=['GET'])
def get_session_better_expressions(session_id):
    ConversationSession.query.get_or_404(session_id)  # Verify session exists
    expressions = BetterExpression.query.filter_by(session_id=session_id).order_by(BetterExpression.created_at).all()
    return jsonify([{
        "id": expression.id,
        "original_text": expression.original_text,
        "better_text": expression.better_text,
        "context": expression.context,
        "explanation": expression.explanation,
        "created_at": expression.created_at
    } for expression in expressions])

@main.route('/better-expression/<int:expression_id>', methods=['PUT'])
def update_better_expression(expression_id):
    expression = BetterExpression.query.get_or_404(expression_id)
    data = request.get_json()
    
    if 'original_text' in data:
        expression.original_text = data['original_text']
    if 'better_text' in data:
        expression.better_text = data['better_text']
    if 'context' in data:
        expression.context = data['context']
    if 'explanation' in data:
        expression.explanation = data['explanation']
    
    try:
        db.session.commit()
        return jsonify({
            "id": expression.id,
            "original_text": expression.original_text,
            "better_text": expression.better_text,
            "context": expression.context,
            "explanation": expression.explanation,
            "session_id": expression.session_id,
            "created_at": expression.created_at
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/better-expression/<int:expression_id>', methods=['DELETE'])
def delete_better_expression(expression_id):
    expression = BetterExpression.query.get_or_404(expression_id)
    try:
        db.session.delete(expression)
        db.session.commit()
        return jsonify({"message": "Better expression deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

# Topic endpoints
@main.route('/topic', methods=['POST'])
def create_topic():
    data = request.get_json()
    if not data or 'name' not in data:
        return jsonify({"error": "name is required"}), 400
    
    new_topic = Topic(
        name=data['name'],
        description=data.get('description'),
        keywords=data.get('keywords')
    )
    
    try:
        db.session.add(new_topic)
        db.session.commit()
        return jsonify({
            "id": new_topic.id,
            "name": new_topic.name,
            "description": new_topic.description,
            "keywords": new_topic.keywords,
            "created_at": new_topic.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/topics', methods=['GET'])
def get_all_topics():
    topics = Topic.query.order_by(Topic.name).all()
    return jsonify([{
        "id": topic.id,
        "name": topic.name,
        "description": topic.description,
        "keywords": topic.keywords,
        "created_at": topic.created_at
    } for topic in topics])

# Scene endpoints
@main.route('/scene', methods=['POST'])
def create_scene():
    data = request.get_json()
    if not data or not all(k in data for k in ('name', 'topic_id')):
        return jsonify({"error": "name and topic_id are required"}), 400
    
    new_scene = Scene(
        name=data['name'],
        context=data.get('context'),
        example_dialogs=data.get('example_dialogs'),
        key_phrases=data.get('key_phrases'),
        topic_id=data['topic_id']
    )
    
    try:
        db.session.add(new_scene)
        db.session.commit()
        return jsonify({
            "id": new_scene.id,
            "name": new_scene.name,
            "context": new_scene.context,
            "example_dialogs": new_scene.example_dialogs,
            "key_phrases": new_scene.key_phrases,
            "topic_id": new_scene.topic_id,
            "created_at": new_scene.created_at
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/scene/<int:scene_id>', methods=['GET'])
def get_scene(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    return jsonify({
        "id": scene.id,
        "name": scene.name,
        "context": scene.context,
        "example_dialogs": scene.example_dialogs,
        "key_phrases": scene.key_phrases,
        "topic_id": scene.topic_id,
        "created_at": scene.created_at
    })

@main.route('/topic/<int:topic_id>/scenes', methods=['GET'])
def get_topic_scenes(topic_id):
    Topic.query.get_or_404(topic_id)  # Verify topic exists
    scenes = Scene.query.filter_by(topic_id=topic_id).order_by(Scene.name).all()
    return jsonify([{
        "id": scene.id,
        "name": scene.name,
        "context": scene.context,
        "example_dialogs": scene.example_dialogs,
        "key_phrases": scene.key_phrases,
        "created_at": scene.created_at
    } for scene in scenes])

@main.route('/scene/<int:scene_id>', methods=['PUT'])
def update_scene(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    data = request.get_json()
    
    if 'name' in data:
        scene.name = data['name']
    if 'context' in data:
        scene.context = data['context']
    if 'example_dialogs' in data:
        scene.example_dialogs = data['example_dialogs']
    if 'key_phrases' in data:
        scene.key_phrases = data['key_phrases']
    
    try:
        db.session.commit()
        return jsonify({
            "id": scene.id,
            "name": scene.name,
            "context": scene.context,
            "example_dialogs": scene.example_dialogs,
            "key_phrases": scene.key_phrases,
            "topic_id": scene.topic_id,
            "created_at": scene.created_at
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@main.route('/scene/<int:scene_id>', methods=['DELETE'])
def delete_scene(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    try:
        db.session.delete(scene)
        db.session.commit()
        return jsonify({"message": "Scene deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

# Add endpoint to get sessions for a specific scene
@main.route('/scene/<int:scene_id>/sessions', methods=['GET'])
def get_scene_sessions(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    sessions = ConversationSession.query.filter_by(scene_id=scene_id).order_by(ConversationSession.started_at).all()
    return jsonify([{
        "id": session.id,
        "person_id": session.person_id,
        "started_at": session.started_at
    } for session in sessions])

# Add endpoint to get messages for a specific scene
@main.route('/scene/<int:scene_id>/messages', methods=['GET'])
def get_scene_messages(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    messages = Message.query.filter_by(scene_id=scene_id).order_by(Message.timestamp).all()
    return jsonify([{
        "id": message.id,
        "session_id": message.session_id,
        "role": message.role,
        "text": message.text,
        "voice": message.voice,
        "timestamp": message.timestamp
    } for message in messages])

@main.route('/scene/<int:scene_id>/children', methods=['GET'])
def get_scene_children(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    return jsonify([{
        "id": child.id,
        "name": child.name,
        "context": child.context,
        "example_dialogs": child.example_dialogs,
        "key_phrases": child.key_phrases,
        "created_at": child.created_at
    } for child in scene.children])

@main.route('/scene/<int:scene_id>/parent', methods=['GET'])
def get_scene_parent(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    if not scene.parent:
        return jsonify({"message": "This is a top-level scene"}), 404
    
    return jsonify({
        "id": scene.parent.id,
        "name": scene.parent.name,
        "context": scene.parent.context,
        "example_dialogs": scene.parent.example_dialogs,
        "key_phrases": scene.parent.key_phrases,
        "created_at": scene.parent.created_at
    }) 