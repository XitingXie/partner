from flask import jsonify, request
from app import db
from app.models import Topic, Scene, ConversationSession
from . import main

# Topic routes
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

# Scene routes
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
        topic_id=data['topic_id'],
        parent_id=data.get('parent_id')
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
            "parent_id": new_scene.parent_id,
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
        "parent_id": scene.parent_id,
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

@main.route('/scene/<int:scene_id>/sessions', methods=['GET'])
def get_scene_sessions(scene_id):
    scene = Scene.query.get_or_404(scene_id)
    sessions = ConversationSession.query.filter_by(scene_id=scene_id).order_by(ConversationSession.started_at).all()
    return jsonify([{
        "id": session.id,
        "person_id": session.person_id,
        "started_at": session.started_at
    } for session in sessions])

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
            "parent_id": scene.parent_id,
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