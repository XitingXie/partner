from flask import Blueprint, jsonify, request
from app.extensions import mongo
from app.models.mongo_models import Topic, Scene, SceneLevel
from datetime import datetime
from bson import ObjectId

bp = Blueprint('scene', __name__, url_prefix='/api')

@bp.route('/topics', methods=['GET'])
def get_topics():
    topics = list(mongo.db.topics.find())
    return jsonify([{
        'id': str(topic['_id']),
        'name': topic['name'],
        'description': topic.get('description')
    } for topic in topics])

@bp.route('/topics/<topic_id>/scenes', methods=['GET'])
def get_scenes(topic_id):
    scenes = list(mongo.db.scenes.find({'topic_id': ObjectId(topic_id)}))
    return jsonify([{
        'id': str(scene['_id']),
        'name': scene['name'],
        'description': scene.get('description')
    } for scene in scenes])

@bp.route('/scenes/<scene_id>/levels/<level>', methods=['GET'])
def get_scene_level(scene_id, level):
    scene_level = mongo.db.scene_levels.find_one({
        'scene_id': ObjectId(scene_id),
        'english_level': level.upper()
    })
    
    if not scene_level:
        return jsonify({
            'error': 'Scene level not found'
        }), 404
    
    return jsonify({
        'id': str(scene_level['_id']),
        'sceneId': str(scene_level['scene_id']),
        'englishLevel': scene_level['english_level'],
        'exampleDialogs': scene_level.get('example_dialogs'),
        'keyPhrases': scene_level.get('key_phrases'),
        'vocabulary': scene_level.get('vocabulary'),
        'grammarPoints': scene_level.get('grammar_points'),
        'createdAt': scene_level.get('created_at')
    })

@bp.route('/topics', methods=['POST'])
def create_topic():
    data = request.get_json()
    if not data or 'name' not in data:
        return jsonify({'error': 'Name is required'}), 400
    
    new_topic = Topic(
        name=data['name'],
        description=data.get('description'),
        icon_path=data.get('icon_path')
    )
    
    result = mongo.db.topics.insert_one(new_topic.to_dict())
    new_topic_id = result.inserted_id
    
    return jsonify({
        'id': str(new_topic_id),
        'name': new_topic.name,
        'description': new_topic.description
    }), 201

@bp.route('/topics/<topic_id>/scenes', methods=['POST'])
def create_scene(topic_id):
    data = request.get_json()
    if not data or 'name' not in data:
        return jsonify({'error': 'Name is required'}), 400
    
    new_scene = Scene(
        name=data['name'],
        topic_id=ObjectId(topic_id),
        description=data.get('description'),
        icon_path=data.get('icon_path'),
        parent_id=ObjectId(data['parent_id']) if data.get('parent_id') else None
    )
    
    result = mongo.db.scenes.insert_one(new_scene.to_dict())
    new_scene_id = result.inserted_id
    
    return jsonify({
        'id': str(new_scene_id),
        'name': new_scene.name,
        'description': new_scene.description
    }), 201

@bp.route('/scenes/<scene_id>/levels', methods=['POST'])
def create_scene_level(scene_id):
    data = request.get_json()
    if not data or 'english_level' not in data:
        return jsonify({'error': 'English level is required'}), 400
    
    new_scene_level = SceneLevel(
        scene_id=ObjectId(scene_id),
        english_level=data['english_level'].upper(),
        example_dialogs=data.get('example_dialogs'),
        key_phrases=data.get('key_phrases'),
        vocabulary=data.get('vocabulary'),
        grammar_points=data.get('grammar_points')
    )
    
    result = mongo.db.scene_levels.insert_one(new_scene_level.to_dict())
    new_level_id = result.inserted_id
    
    return jsonify({
        'id': str(new_level_id),
        'sceneId': str(scene_id),
        'englishLevel': new_scene_level.english_level
    }), 201