from flask import Blueprint, jsonify

# Remove the main blueprint and routes
# Just export the blueprints from other route files
from .conversation import bp as conversation_bp
from .scene import bp as scene_bp
from .user import bp as user_bp
from .learning import bp as learning_bp

__all__ = ['conversation_bp', 'scene_bp', 'user_bp', 'learning_bp']

# Remove these routes since we removed the main blueprint
# @main.route('/')
# def index():
#     return jsonify({"message": "Welcome to the Flask API!"})
# 
# @main.route('/health')
# def health_check():
#     return jsonify({"status": "healthy"}) 