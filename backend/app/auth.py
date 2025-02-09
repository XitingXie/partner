import firebase_admin
from firebase_admin import credentials, auth
from functools import wraps
from flask import request, jsonify, current_app
import os
import logging

# Set up logger
logger = logging.getLogger(__name__)

# Initialize Firebase Admin with credentials only if not already initialized
if not firebase_admin._apps:
    cred_path = os.getenv('FIREBASE_APPLICATION_CREDENTIALS', './firebase-credentials.json')
    logger.info(f"Initializing Firebase Admin SDK with credentials from: {cred_path}")
    try:
        if not os.path.exists(cred_path):
            logger.error(f"Firebase credentials file not found at: {cred_path}")
            raise FileNotFoundError(f"Firebase credentials file not found at: {cred_path}")
        
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        logger.info("Firebase Admin SDK initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize Firebase Admin SDK: {str(e)}", exc_info=True)
        raise

def verify_token(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        
        if not auth_header:
            logger.warning("No authorization header found")
            return jsonify({'error': 'No authorization header'}), 401
        
        if not auth_header.startswith('Bearer '):
            logger.warning("Invalid authorization header format")
            return jsonify({'error': 'Invalid authorization header format'}), 401
        
        token = auth_header.split('Bearer ')[1]
        logger.debug(f"Attempting to verify token: {token[:20]}...")
        
        try:
            # Verify the ID token
            logger.debug("Starting token verification...")
            decoded_token = auth.verify_id_token(token)
            logger.debug(f"Token verified successfully for user: {decoded_token.get('uid')}")
            
            # Get authentication provider information
            provider_id = decoded_token.get('firebase', {}).get('sign_in_provider')
            logger.info(f"User authenticated with provider: {provider_id}")
            
            # Add user info to request context
            request.user = {
                'uid': decoded_token['uid'],
                'email': decoded_token.get('email'),
                'email_verified': decoded_token.get('email_verified', False),
                'auth_provider': provider_id,  # Will be 'password', 'facebook.com', 'google.com', etc.
                'name': decoded_token.get('name'),
                'picture': decoded_token.get('picture')
            }
            
            # Log authentication information
            logger.info(f"Authenticated request from user {request.user['uid']} using provider {provider_id}")
            
            return f(*args, **kwargs)
        except auth.ExpiredIdTokenError as e:
            logger.error(f"Token expired: {str(e)}")
            return jsonify({'error': 'Token has expired'}), 401
        except auth.InvalidIdTokenError as e:
            logger.error(f"Invalid token: {str(e)}", exc_info=True)
            return jsonify({'error': 'Invalid token'}), 401
        except auth.RevokedIdTokenError as e:
            logger.error(f"Token revoked: {str(e)}")
            return jsonify({'error': 'Token has been revoked'}), 401
        except Exception as e:
            logger.error(f"Token verification error: {str(e)}", exc_info=True)
            return jsonify({'error': 'Failed to verify token'}), 401
    
    return decorated_function

def verify_same_user(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not hasattr(request, 'user'):
            logger.error("No user context found in request")
            return jsonify({'error': 'No user context found'}), 401
        
        # Get user_id from URL parameters, view args, or JSON body (for POST/PUT requests)
        user_id = None
        
        # First check URL parameters and view args
        if 'uid' in kwargs:
            user_id = kwargs['uid']
        elif 'uid' in request.view_args:
            user_id = request.view_args['uid']
        # For POST/PUT requests with JSON body
        elif request.is_json and request.json:
            user_id = request.json.get('uid')
        
        if not user_id:
            logger.error("No user ID provided in request")
            return jsonify({'error': 'No user ID provided'}), 400
        
        if request.user['uid'] != user_id:
            logger.error(f"User ID mismatch: {request.user['uid']} != {user_id}")
            return jsonify({'error': 'Unauthorized access'}), 403
        
        return f(*args, **kwargs)
    
    return decorated_function 