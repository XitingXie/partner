from flask import Blueprint, jsonify

# Remove the main blueprint and routes
# Just export the blueprints from other route files
from .user import bp as user_bp
from .scene import bp as scene_bp
from .conversation import bp as conversation_bp
from .learning import bp as learning_bp
from .config import bp as config_bp  # Now importing from the config package

__all__ = ['user_bp', 'scene_bp', 'conversation_bp', 'learning_bp', 'config_bp']

# Remove these routes since we removed the main blueprint
# @main.route('/')
# def index():
#     return jsonify({"message": "Welcome to the Flask API!"})
# 
# @main.route('/health')
# def health_check():
#     return jsonify({"status": "healthy"}) 