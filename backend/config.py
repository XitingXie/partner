import os
from dotenv import load_dotenv
import logging.config

basedir = os.path.abspath(os.path.dirname(__file__))
load_dotenv()

class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev'
    MONGO_URI = os.environ.get('MONGO_URI') or 'mongodb://localhost:27017/lingomia'
    MONGO_DBNAME = 'lingomia'
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URL') or \
        'sqlite:///' + os.path.join(basedir, 'instance', 'app.db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # Logging Configuration
    LOGGING_CONFIG = {
        'version': 1,
        'disable_existing_loggers': False,
        'formatters': {
            'standard': {
                'format': '%(asctime)s [%(levelname)s] %(name)s: %(message)s'
            },
            'detailed': {
                'format': '%(asctime)s [%(levelname)s] %(name)s:%(lineno)d: %(message)s'
            },
        },
        'handlers': {
            'console': {
                'class': 'logging.StreamHandler',
                'level': 'DEBUG',
                'formatter': 'standard',
                'stream': 'ext://sys.stdout'
            },
            'file': {
                'class': 'logging.handlers.RotatingFileHandler',
                'level': 'DEBUG',
                'formatter': 'detailed',
                'filename': os.path.join(basedir, 'logs', 'app.log'),
                'maxBytes': 10485760,  # 10MB
                'backupCount': 5
            }
        },
        'loggers': {
            '': {  # Root logger
                'handlers': ['console', 'file'],
                'level': os.getenv('LOG_LEVEL', 'INFO'),
                'propagate': True
            },
            'app.auth': {  # Auth module logger
                'handlers': ['console', 'file'],
                'level': 'DEBUG',
                'propagate': False
            }
        }
    }

    @classmethod
    def init_logging(cls):
        # Ensure logs directory exists
        log_dir = os.path.join(basedir, 'logs')
        if not os.path.exists(log_dir):
            os.makedirs(log_dir)
        
        # Configure logging
        logging.config.dictConfig(cls.LOGGING_CONFIG) 