from flask import Blueprint, jsonify, request
from app import db
from app.models import Person, Scene, Topic, ConversationSession, Message, UnfamiliarWord, WrongGrammar, BestFitWord, BetterExpression
from datetime import datetime
from sqlalchemy import text
from app.models.user import completed_scenes  # Import the association table

bp = Blueprint('user', __name__, url_prefix='/api')

@bp.route('/person', methods=['POST'])
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

@bp.route('/person/<int:person_id>', methods=['GET'])
def get_person(person_id):
    person = Person.query.get_or_404(person_id)
    return jsonify({
        "id": person.id,
        "name": person.name,
        "created_at": person.created_at
    })

@bp.route('/person/<int:person_id>', methods=['PUT'])
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

@bp.route('/person/<int:person_id>', methods=['DELETE'])
def delete_person(person_id):
    person = Person.query.get_or_404(person_id)
    try:
        db.session.delete(person)
        db.session.commit()
        return jsonify({"message": "Person deleted successfully"})
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@bp.route('/person/<int:person_id>/completed-scenes', methods=['GET'])
def get_completed_scenes(person_id):
    person = Person.query.get_or_404(person_id)
    completed = person.completed_scenes.all()
    return jsonify([{
        "id": scene.id,
        "name": scene.name,
        "completed_at": db.session.query(completed_scenes.c.completed_at)
            .filter_by(person_id=person_id, scene_id=scene.id).scalar(),
        "score": db.session.query(completed_scenes.c.score)
            .filter_by(person_id=person_id, scene_id=scene.id).scalar(),
        "feedback": db.session.query(completed_scenes.c.feedback)
            .filter_by(person_id=person_id, scene_id=scene.id).scalar()
    } for scene in completed])

@bp.route('/person/<int:person_id>/complete-scene/<int:scene_id>', methods=['POST'])
def complete_scene(person_id, scene_id):
    data = request.get_json() or {}
    person = Person.query.get_or_404(person_id)
    scene = Scene.query.get_or_404(scene_id)
    
    try:
        person.complete_scene(
            scene,
            score=data.get('score'),
            feedback=data.get('feedback')
        )
        db.session.commit()
        return jsonify({
            "message": f"Scene {scene.name} marked as completed for {person.name}",
            "completed_at": datetime.utcnow(),
            "score": data.get('score'),
            "feedback": data.get('feedback')
        })
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 400

@bp.route('/person/<int:person_id>/progress', methods=['GET'])
def get_person_progress(person_id):
    person = Person.query.get_or_404(person_id)
    total_scenes = Scene.query.count()
    completed_count = person.completed_scenes.count()
    
    return jsonify({
        "total_scenes": total_scenes,
        "completed_scenes": completed_count,
        "progress_percentage": (completed_count / total_scenes * 100) if total_scenes > 0 else 0,
        "recent_completions": [{
            "id": scene.id,
            "name": scene.name,
            "completed_at": db.session.query(completed_scenes.c.completed_at)
                .filter_by(person_id=person_id, scene_id=scene.id).scalar()
        } for scene in person.completed_scenes.order_by(completed_scenes.c.completed_at.desc()).limit(5)]
    })

@bp.route('/db/stats', methods=['GET'])
def get_db_stats():
    return jsonify({
        "users": Person.query.count(),
        "topics": Topic.query.count(),
        "scenes": Scene.query.count(),
        "sessions": ConversationSession.query.count(),
        "messages": Message.query.count(),
        "completed_scenes": db.session.query(completed_scenes).count(),
        "unfamiliar_words": UnfamiliarWord.query.count(),
        "grammar_mistakes": WrongGrammar.query.count(),
        "word_improvements": BestFitWord.query.count(),
        "expression_improvements": BetterExpression.query.count()
    })

@bp.route('/db/users', methods=['GET'])
def get_all_users():
    users = Person.query.all()
    return jsonify([{
        "id": user.id,
        "name": user.name,
        "created_at": user.created_at,
        "sessions_count": len(user.sessions),
        "completed_scenes_count": user.completed_scenes.count(),
        "unfamiliar_words_count": len(user.unfamiliar_words)
    } for user in users])

@bp.route('/db/raw', methods=['GET'])
def get_raw_db_data():
    tables = {
        "person": db.session.execute(text("SELECT * FROM person")).fetchall(),
        "topic": db.session.execute(text("SELECT * FROM topic")).fetchall(),
        "scene": db.session.execute(text("SELECT * FROM scene")).fetchall(),
        "conversation_session": db.session.execute(text("SELECT * FROM conversation_session")).fetchall(),
        "message": db.session.execute(text("SELECT * FROM message")).fetchall(),
        "completed_scenes": db.session.execute(text("SELECT * FROM completed_scenes")).fetchall(),
        "unfamiliar_word": db.session.execute(text("SELECT * FROM unfamiliar_word")).fetchall(),
        "wrong_grammar": db.session.execute(text("SELECT * FROM wrong_grammar")).fetchall(),
        "best_fit_word": db.session.execute(text("SELECT * FROM best_fit_word")).fetchall(),
        "better_expression": db.session.execute(text("SELECT * FROM better_expression")).fetchall()
    }
    
    return jsonify({
        table_name: [dict(row) for row in rows]
        for table_name, rows in tables.items()
    })

# ... other person-related routes ... 