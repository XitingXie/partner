from flask import Blueprint

main = Blueprint('main', __name__)

from . import user, conversation, learning, scene

# Basic routes
@main.route('/')
def index():
    return jsonify({"message": "Welcome to the Flask API!"})

@main.route('/health')
def health_check():
    return jsonify({"status": "healthy"}) 