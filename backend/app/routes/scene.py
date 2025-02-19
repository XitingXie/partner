from flask import Blueprint, jsonify, request, send_file
from app.extensions import mongo
from app.models.mongo_models import Topic, Scene, SceneLevel
from datetime import datetime
from bson import ObjectId
from app.auth import verify_token
import os

bp = Blueprint('scene', __name__, url_prefix='/api')


# Configure audio storage directory
AUDIO_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'static', 'audio')
os.makedirs(AUDIO_DIR, exist_ok=True)

@bp.route('/topics', methods=['GET'])
@verify_token
def get_topics():
    topics = list(mongo.db.topics.find())
    return jsonify([{
        'id': str(topic['_id']),
        'name': topic['name'],
        'description': topic.get('description')
    } for topic in topics])

@bp.route('/topics/<topic_id>/scenes', methods=['GET'])
@verify_token
def get_scenes(topic_id):
    scenes = list(mongo.db.scenes.find({'topic_id': ObjectId(topic_id)}))
    return jsonify([{
        'id': str(scene['_id']),
        'name': scene['name'],
        'description': scene.get('description')
    } for scene in scenes])

@bp.route('/scenes/<scene_id>/levels/<level>', methods=['GET'])
@verify_token
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

@bp.route('/scenes/<scene_id>/opening-remarks', methods=['GET'])
@verify_token
def get_opening_remarks_audio(scene_id):
    try:
        # Get the English level from query parameters
        english_level = request.args.get('level', 'B1').upper()
        
        # Convert scene_id to ObjectId
        scene_id_obj = ObjectId(scene_id)
        
        # Get the scene level
        scene_level = mongo.db.scene_levels.find_one({
            'scene_id': scene_id_obj,
            'english_level': english_level
        })
        
        if not scene_level:
            print(f"Scene level not found for scene_id: {scene_id}, level: {english_level}", flush=True)
            return jsonify({'message': 'No audio available'}), 200
            
        # Try to get scene-specific audio file
        audio_path = None
        if scene_level.get('opening_remarks_audio_path'):
            audio_path = scene_level['opening_remarks_audio_path']
            if os.path.exists(audio_path):
                print(f"Found scene-specific audio at: {audio_path}", flush=True)
                return send_file(
                    audio_path,
                    mimetype='audio/mpeg',
                    as_attachment=True,
                    download_name=f'opening_remarks_{scene_id}_{english_level}.mp3'
                )
        
        # If no scene-specific audio found, use default audio for the level
        default_audio_path = os.path.join(AUDIO_DIR, f'default_{english_level}.mp3')
        if os.path.exists(default_audio_path):
            print(f"Using default audio for level {english_level}", flush=True)
            return send_file(
                default_audio_path,
                mimetype='audio/mpeg',
                as_attachment=True,
                download_name=f'opening_remarks_{scene_id}_{english_level}.mp3'
            )
            
        # If no audio found, return empty response with 200
        print(f"No audio found for level {english_level}", flush=True)
        return jsonify({'message': 'No audio available'}), 200
        
    except Exception as e:
        print(f"Error serving audio file: {str(e)}", flush=True)
        return jsonify({'error': str(e)}), 500