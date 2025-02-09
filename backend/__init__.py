import os
from flask import Flask
from flask_pymongo import PyMongo
from config import Config

mongo = PyMongo()

def create_app(test_config=None):
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
    from app.routes import user, scene, conversation, learning
    app.register_blueprint(user.bp)
    app.register_blueprint(scene.bp)
    app.register_blueprint(conversation.bp)
    app.register_blueprint(learning.bp)

    return app 