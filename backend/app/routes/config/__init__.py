from flask import Blueprint, jsonify
from app.auth import verify_token, verify_same_user
import os
from datetime import datetime, timedelta
import logging
from functools import wraps
from flask import request, current_app

bp = Blueprint('config', __name__, url_prefix='/api/config')
logger = logging.getLogger(__name__)

# Cache for rate limiting
api_key_requests = {}

def rate_limit(max_requests=100, window_seconds=3600):
    def decorator(f):
        @wraps(f)
        def wrapped(*args, **kwargs):
            user_id = request.user['uid']
            now = datetime.utcnow()
            
            # Clean up old entries
            api_key_requests.clear()
            
            # Get user's request history
            user_requests = api_key_requests.get(user_id, [])
            # Remove old requests outside the window
            user_requests = [t for t in user_requests 
                           if t > now - timedelta(seconds=window_seconds)]
            
            # Check if user has exceeded rate limit
            if len(user_requests) >= max_requests:
                logger.warning(f"Rate limit exceeded for user {user_id}")
                return jsonify({
                    'error': 'Rate limit exceeded. Please try again later.'
                }), 429
            
            # Add current request
            user_requests.append(now)
            api_key_requests[user_id] = user_requests
            
            return f(*args, **kwargs)
        return wrapped
    return decorator

@bp.route('/openai-key', methods=['GET'])
@verify_token
@verify_same_user
@rate_limit(max_requests=100, window_seconds=3600)  # 100 requests per hour
def get_openai_key():
    try:
        # Get the API key from environment variables
        api_key = os.getenv('OPENAI_API_KEY')
        if not api_key:
            logger.error("OpenAI API key not found in environment variables")
            return jsonify({
                'error': 'API key not configured'
            }), 500

        # Log the request (but not the key itself)
        logger.info(f"OpenAI API key requested by user {request.user['uid']}")
        
        return jsonify({
            'api_key': api_key
        })
        
    except Exception as e:
        logger.error(f"Error retrieving OpenAI API key: {str(e)}")
        return jsonify({
            'error': 'Internal server error'
        }), 500 