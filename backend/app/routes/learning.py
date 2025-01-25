from flask import Blueprint, jsonify
from app import db
from app.models import UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression, ConversationSession

bp = Blueprint('learning', __name__, url_prefix='/api')

@bp.route('/session/<int:session_id>/unfamiliar-words', methods=['GET'])
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

@bp.route('/session/<int:session_id>/wrong-grammar', methods=['GET'])
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

@bp.route('/session/<int:session_id>/best-fit-words', methods=['GET'])
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

@bp.route('/session/<int:session_id>/better-expressions', methods=['GET'])
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

# Get all learning data for a session
@bp.route('/session/<int:session_id>/learning-data', methods=['GET'])
def get_session_learning_data(session_id):
    ConversationSession.query.get_or_404(session_id)  # Verify session exists
    
    unfamiliar_words = UnfamiliarWord.query.filter_by(session_id=session_id).all()
    wrong_grammar = WrongGrammar.query.filter_by(session_id=session_id).all()
    best_fit_words = BestFitWord.query.filter_by(session_id=session_id).all()
    better_expressions = BetterExpression.query.filter_by(session_id=session_id).all()
    
    return jsonify({
        "unfamiliar_words": [{
            "id": word.id,
            "word": word.word,
            "definition": word.definition,
            "example": word.example,
            "created_at": word.created_at
        } for word in unfamiliar_words],
        
        "grammar_errors": [{
            "id": grammar.id,
            "incorrect_text": grammar.incorrect_text,
            "correct_text": grammar.correct_text,
            "explanation": grammar.explanation,
            "created_at": grammar.created_at
        } for grammar in wrong_grammar],
        
        "best_fit_words": [{
            "id": best_fit.id,
            "context": best_fit.context,
            "original_word": best_fit.original_word,
            "better_word": best_fit.better_word,
            "explanation": best_fit.explanation,
            "created_at": best_fit.created_at
        } for best_fit in best_fit_words],
        
        "better_expressions": [{
            "id": expression.id,
            "original_text": expression.original_text,
            "better_text": expression.better_text,
            "context": expression.context,
            "explanation": expression.explanation,
            "created_at": expression.created_at
        } for expression in better_expressions]
    })

# Get all learning data for a user across sessions
@bp.route('/person/<int:person_id>/learning-data', methods=['GET'])
def get_person_learning_data(person_id):
    # Get all sessions for this person
    sessions = ConversationSession.query.filter_by(person_id=person_id).all()
    session_ids = [session.id for session in sessions]
    
    unfamiliar_words = UnfamiliarWord.query.filter(UnfamiliarWord.session_id.in_(session_ids)).all()
    wrong_grammar = WrongGrammar.query.filter(WrongGrammar.session_id.in_(session_ids)).all()
    best_fit_words = BestFitWord.query.filter(BestFitWord.session_id.in_(session_ids)).all()
    better_expressions = BetterExpression.query.filter(BetterExpression.session_id.in_(session_ids)).all()
    
    return jsonify({
        "unfamiliar_words": [{
            "id": word.id,
            "word": word.word,
            "definition": word.definition,
            "example": word.example,
            "session_id": word.session_id,
            "created_at": word.created_at
        } for word in unfamiliar_words],
        
        "grammar_errors": [{
            "id": grammar.id,
            "incorrect_text": grammar.incorrect_text,
            "correct_text": grammar.correct_text,
            "explanation": grammar.explanation,
            "session_id": grammar.session_id,
            "created_at": grammar.created_at
        } for grammar in wrong_grammar],
        
        "best_fit_words": [{
            "id": best_fit.id,
            "context": best_fit.context,
            "original_word": best_fit.original_word,
            "better_word": best_fit.better_word,
            "explanation": best_fit.explanation,
            "session_id": best_fit.session_id,
            "created_at": best_fit.created_at
        } for best_fit in best_fit_words],
        
        "better_expressions": [{
            "id": expression.id,
            "original_text": expression.original_text,
            "better_text": expression.better_text,
            "context": expression.context,
            "explanation": expression.explanation,
            "session_id": expression.session_id,
            "created_at": expression.created_at
        } for expression in better_expressions]
    }) 