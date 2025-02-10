import os
from flask import Flask
from config import Config
from app.extensions import mongo

def create_app(test_config=None):
    # Initialize logging first
    Config.init_logging()
    
    app = Flask(__name__, instance_relative_config=True)
    
    if test_config is None:
        app.config.from_object(Config)
    else:
        app.config.update(test_config)

    # Initialize MongoDB
    mongo.init_app(app)

    # Ensure instance folder exists
    try:
        os.makedirs(app.instance_path)
    except OSError:
        pass

    # Register blueprints
    from app.routes import user_bp, scene_bp, conversation_bp, learning_bp, config_bp
    app.register_blueprint(user_bp)
    app.register_blueprint(scene_bp)
    app.register_blueprint(conversation_bp)
    app.register_blueprint(learning_bp)
    app.register_blueprint(config_bp)

    return app 