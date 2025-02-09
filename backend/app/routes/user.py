from flask import Blueprint, jsonify, request
from app.extensions import mongo
from app.models.mongo_models import User, CompletedScene
from datetime import datetime
from bson import ObjectId
import logging

# Set up logger
logger = logging.getLogger(__name__)

bp = Blueprint('user', __name__, url_prefix='/api')

@bp.route('/users', methods=['POST'])
def create_user():
    data = request.get_json()
    if not data:
        return jsonify({"error": "Request data is required"}), 400
    
    # Create new user document
    new_user = User(
        uid=data.get('uid'),
        email=data.get('email'),
        name=data.get('display_name') or data.get('email').split('@')[0],
        first_language=data.get('first_language'),
        display_name=data.get('display_name'),
        given_name=data.get('given_name'),
        family_name=data.get('family_name'),
        photo_url=data.get('photo_url')
    )
    
    try:
        # Insert into MongoDB
        mongo.db.users.insert_one(new_user.to_dict())
        return jsonify({
            "exists": True,
            "message": "User created successfully",
            "first_language": new_user.first_language,
            "uid": new_user.uid,
            "user_info": {
                "display_name": new_user.display_name,
                "given_name": new_user.given_name,
                "family_name": new_user.family_name,
                "photo_url": new_user.photo_url
            }
        }), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 400

@bp.route('/users/<string:uid>', methods=['GET'])
def check_user_exists(uid):
    user = mongo.db.users.find_one({"uid": uid})
    if user:
        return jsonify({
            "exists": True,
            "message": "User found",
            "first_language": user.get('first_language'),
            "uid": user['uid']
        })
    return jsonify({
        "exists": False,
        "message": "User not found",
        "first_language": None,
        "uid": None
    })

@bp.route('/users/<string:uid>/completed-scenes', methods=['GET'])
def get_completed_scenes(uid):
    completed = list(mongo.db.completed_scenes.find({"user_uid": uid}))
    return jsonify([{
        "id": scene['scene_id'],
        "completed_at": scene['completed_at'],
        "score": scene.get('score'),
        "feedback": scene.get('feedback')
    } for scene in completed])

@bp.route('/users/<string:uid>/complete-scene/<int:scene_id>', methods=['POST'])
def complete_scene(uid, scene_id):
    data = request.get_json() or {}
    
    completed_scene = CompletedScene(
        user_uid=uid,
        scene_id=scene_id,
        score=data.get('score'),
        feedback=data.get('feedback')
    )
    
    try:
        # Use upsert to handle both new completions and updates
        mongo.db.completed_scenes.update_one(
            {"user_uid": uid, "scene_id": scene_id},
            {"$set": completed_scene.to_dict()},
            upsert=True
        )
        return jsonify({
            "message": f"Scene {scene_id} marked as completed for user {uid}",
            "completed_at": completed_scene.completed_at,
            "score": completed_scene.score,
            "feedback": completed_scene.feedback
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 400

@bp.route('/users/<string:uid>/progress', methods=['GET'])
def get_person_progress(uid):
    # Get total scenes count
    total_scenes = mongo.db.scenes.count_documents({})
    
    # Get completed scenes
    completed = list(mongo.db.completed_scenes.find(
        {"user_uid": uid},
        sort=[("completed_at", -1)],
        limit=5
    ))
    
    completed_count = len(completed)
    
    return jsonify({
        "total_scenes": total_scenes,
        "completed_scenes": completed_count,
        "progress_percentage": (completed_count / total_scenes * 100) if total_scenes > 0 else 0,
        "recent_completions": [{
            "id": scene['scene_id'],
            "completed_at": scene['completed_at']
        } for scene in completed]
    })

@bp.route('/db/stats', methods=['GET'])
def get_db_stats():
    return jsonify({
        "users": mongo.db.users.count_documents({}),
        "topics": mongo.db.topics.count_documents({}),
        "scenes": mongo.db.scenes.count_documents({}),
        "sessions": mongo.db.conversation_sessions.count_documents({}),
        "completed_scenes": mongo.db.completed_scenes.count_documents({})
    })

@bp.route('/users/<string:uid>/language', methods=['PUT'])
def update_first_language(uid):
    logger.info(f"Received language update request for user {uid}")
    logger.info(f"Request data: {request.get_json()}")
    logger.info(f"Request path: {request.path}")
    logger.info(f"Full URL: {request.url}")
    
    data = request.get_json()
    if not data or 'first_language' not in data:
        logger.error("Missing first_language in request data")
        return jsonify({"error": "First language is required"}), 400
    
    try:
        result = mongo.db.users.update_one(
            {"uid": uid},
            {"$set": {"first_language": data['first_language']}}
        )
        
        if result.matched_count == 0:
            logger.error(f"No user found with uid {uid}")
            return jsonify({"error": "User not found"}), 404
            
        user = mongo.db.users.find_one({"uid": uid})
        logger.info(f"Successfully updated language for user {uid} to {data['first_language']}")
        return jsonify({
            "exists": True,
            "message": "First language updated successfully",
            "first_language": user['first_language'],
            "uid": user['uid']
        })
    except Exception as e:
        logger.error(f"Error updating language: {str(e)}")
        return jsonify({"error": str(e)}), 400

# ... other person-related routes ... 