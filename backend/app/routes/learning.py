from flask import Blueprint, jsonify, request
from app.extensions import mongo
from app.models.mongo_models import UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression
from datetime import datetime
from bson import ObjectId
from app.auth import verify_token, verify_same_user

bp = Blueprint('learning', __name__, url_prefix='/api')

@bp.route('/learning/unfamiliar-words', methods=['POST'])
@verify_token
@verify_same_user
def add_unfamiliar_word():
    data = request.get_json()
    if not data or not all(k in data for k in ('session_id', 'user_uid', 'word')):
        return jsonify({"error": "session_id, user_uid, and word are required"}), 400
    
    new_word = UnfamiliarWord(
        session_id=data['session_id'],
        user_uid=data['user_uid'],
        word=data['word'],
        context=data.get('context')
    )
    
    result = mongo.db.unfamiliar_words.insert_one(new_word.to_dict())
    
    return jsonify({
        "id": str(result.inserted_id),
        "word": new_word.word,
        "context": new_word.context,
        "timestamp": new_word.timestamp.isoformat()
    }), 201

@bp.route('/learning/grammar-mistakes', methods=['POST'])
@verify_token
@verify_same_user
def add_grammar_mistake():
    data = request.get_json()
    if not data or not all(k in data for k in ('session_id', 'user_uid', 'wrong_text', 'correct_text')):
        return jsonify({"error": "session_id, user_uid, wrong_text, and correct_text are required"}), 400
    
    new_mistake = WrongGrammar(
        session_id=data['session_id'],
        user_uid=data['user_uid'],
        wrong_text=data['wrong_text'],
        correct_text=data['correct_text'],
        explanation=data.get('explanation')
    )
    
    result = mongo.db.grammar_mistakes.insert_one(new_mistake.to_dict())
    
    return jsonify({
        "id": str(result.inserted_id),
        "wrong_text": new_mistake.wrong_text,
        "correct_text": new_mistake.correct_text,
        "explanation": new_mistake.explanation,
        "timestamp": new_mistake.timestamp.isoformat()
    }), 201

@bp.route('/learning/word-improvements', methods=['POST'])
@verify_token
@verify_same_user
def add_word_improvement():
    data = request.get_json()
    if not data or not all(k in data for k in ('session_id', 'user_uid', 'original_word', 'suggested_word')):
        return jsonify({"error": "session_id, user_uid, original_word, and suggested_word are required"}), 400
    
    new_improvement = BestFitWord(
        session_id=data['session_id'],
        user_uid=data['user_uid'],
        original_word=data['original_word'],
        suggested_word=data['suggested_word'],
        context=data.get('context')
    )
    
    result = mongo.db.word_improvements.insert_one(new_improvement.to_dict())
    
    return jsonify({
        "id": str(result.inserted_id),
        "original_word": new_improvement.original_word,
        "suggested_word": new_improvement.suggested_word,
        "context": new_improvement.context,
        "timestamp": new_improvement.timestamp.isoformat()
    }), 201

@bp.route('/learning/expression-improvements', methods=['POST'])
@verify_token
@verify_same_user
def add_expression_improvement():
    data = request.get_json()
    if not data or not all(k in data for k in ('session_id', 'user_uid', 'original_text', 'suggested_text')):
        return jsonify({"error": "session_id, user_uid, original_text, and suggested_text are required"}), 400
    
    new_improvement = BetterExpression(
        session_id=data['session_id'],
        user_uid=data['user_uid'],
        original_text=data['original_text'],
        suggested_text=data['suggested_text'],
        explanation=data.get('explanation')
    )
    
    result = mongo.db.expression_improvements.insert_one(new_improvement.to_dict())
    
    return jsonify({
        "id": str(result.inserted_id),
        "original_text": new_improvement.original_text,
        "suggested_text": new_improvement.suggested_text,
        "explanation": new_improvement.explanation,
        "timestamp": new_improvement.timestamp.isoformat()
    }), 201

@bp.route('/learning/user/<user_uid>/progress', methods=['GET'])
@verify_token
@verify_same_user
def get_user_learning_progress(user_uid):
    # Get counts from each collection
    unfamiliar_words = list(mongo.db.unfamiliar_words.find(
        {'user_uid': user_uid},
        sort=[('timestamp', -1)],
        limit=10
    ))
    
    grammar_mistakes = list(mongo.db.grammar_mistakes.find(
        {'user_uid': user_uid},
        sort=[('timestamp', -1)],
        limit=10
    ))
    
    word_improvements = list(mongo.db.word_improvements.find(
        {'user_uid': user_uid},
        sort=[('timestamp', -1)],
        limit=10
    ))
    
    expression_improvements = list(mongo.db.expression_improvements.find(
        {'user_uid': user_uid},
        sort=[('timestamp', -1)],
        limit=10
    ))
    
    return jsonify({
        "unfamiliar_words": [{
            "id": str(word['_id']),
            "word": word['word'],
            "context": word.get('context'),
            "timestamp": word['timestamp']
        } for word in unfamiliar_words],
        
        "grammar_mistakes": [{
            "id": str(mistake['_id']),
            "wrong_text": mistake['wrong_text'],
            "correct_text": mistake['correct_text'],
            "explanation": mistake.get('explanation'),
            "timestamp": mistake['timestamp']
        } for mistake in grammar_mistakes],
        
        "word_improvements": [{
            "id": str(improvement['_id']),
            "original_word": improvement['original_word'],
            "suggested_word": improvement['suggested_word'],
            "context": improvement.get('context'),
            "timestamp": improvement['timestamp']
        } for improvement in word_improvements],
        
        "expression_improvements": [{
            "id": str(improvement['_id']),
            "original_text": improvement['original_text'],
            "suggested_text": improvement['suggested_text'],
            "explanation": improvement.get('explanation'),
            "timestamp": improvement['timestamp']
        } for improvement in expression_improvements]
    }) 