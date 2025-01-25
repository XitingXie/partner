from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from config import Config
from app.models import db
from dotenv import load_dotenv
import os
import logging
from flask_cors import CORS

# Load environment variables
load_dotenv()

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)
    
    # Set up logging
    app.logger.setLevel(logging.DEBUG)
    logging.basicConfig(level=logging.DEBUG)
    
    # Enable debug mode
    app.debug = True
    
    db.init_app(app)
    migrate = Migrate(app, db)

    # Configure CORS to allow requests from Android emulator
    CORS(app, resources={
        r"/api/*": {
            "origins": ["http://10.0.2.2:8008", "http://localhost:8008"],
            "methods": ["GET", "POST", "PUT", "DELETE"],
            "allow_headers": ["Content-Type"]
        }
    })
    
    # Register blueprints
    from .routes.scene import bp as scene_bp
    from .routes.conversation import bp as conversation_bp
    from .routes.user import bp as user_bp
    from .routes.learning import bp as learning_bp
    
    app.register_blueprint(scene_bp)
    app.register_blueprint(conversation_bp)
    app.register_blueprint(user_bp)
    app.register_blueprint(learning_bp)

    print("\nRegistered routes:", flush=True)
    for rule in app.url_map.iter_rules():
        print(f"{rule.endpoint}: {rule.methods} {rule}", flush=True)
    
    return app 