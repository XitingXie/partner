from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from config import Config
from app.models import db
from dotenv import load_dotenv
import os
import logging

# Load environment variables
load_dotenv()

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

    from app.routes import main
    app.register_blueprint(main)

    return app 