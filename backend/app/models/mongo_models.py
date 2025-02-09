from datetime import datetime
from bson import ObjectId

class MongoModel:
    """Base class for MongoDB models"""
    @classmethod
    def from_dict(cls, data):
        return cls(**data)

    def to_dict(self):
        return {k: v for k, v in self.__dict__.items() if not k.startswith('_')}

class User(MongoModel):
    def __init__(self, uid, email, name=None, first_language=None, display_name=None, 
                 given_name=None, family_name=None, photo_url=None, created_at=None,
                 auth_provider='password'):
        self.uid = uid
        self.email = email
        self.name = name or email.split('@')[0]
        self.first_language = first_language
        self.display_name = display_name
        self.given_name = given_name
        self.family_name = family_name
        self.photo_url = photo_url
        self.created_at = created_at or datetime.utcnow()
        self.auth_provider = auth_provider

    def to_dict(self):
        return {
            'uid': self.uid,
            'email': self.email,
            'name': self.name,
            'first_language': self.first_language,
            'display_name': self.display_name,
            'given_name': self.given_name,
            'family_name': self.family_name,
            'photo_url': self.photo_url,
            'created_at': self.created_at,
            'auth_provider': self.auth_provider
        }

class ConversationSession(MongoModel):
    def __init__(self, user_uid, scene_id, started_at=None, ended_at=None, _id=None):
        self._id = _id or ObjectId()
        self.user_uid = user_uid
        self.scene_id = scene_id
        self.started_at = started_at or datetime.utcnow()
        self.ended_at = ended_at
        self.messages = []
        self.learning_points = {
            'unfamiliar_words': [],
            'grammar_mistakes': [],
            'better_expressions': [],
            'best_fit_words': []
        }

    def add_message(self, role, text, timestamp=None):
        message = {
            'role': role,
            'text': text,
            'timestamp': timestamp or datetime.utcnow()
        }
        self.messages.append(message)
        return message

    def add_learning_point(self, category, data):
        if category in self.learning_points:
            data['timestamp'] = datetime.utcnow()
            self.learning_points[category].append(data)
            return data
        return None

    def to_dict(self):
        return {
            '_id': self._id,
            'user_uid': self.user_uid,
            'scene_id': self.scene_id,
            'started_at': self.started_at,
            'ended_at': self.ended_at,
            'messages': self.messages,
            'learning_points': self.learning_points
        }

class CompletedScene(MongoModel):
    def __init__(self, user_uid, scene_id, completed_at=None, score=None, feedback=None):
        self.user_uid = user_uid
        self.scene_id = scene_id
        self.completed_at = completed_at or datetime.utcnow()
        self.score = score
        self.feedback = feedback

    def to_dict(self):
        return {
            'user_uid': self.user_uid,
            'scene_id': self.scene_id,
            'completed_at': self.completed_at,
            'score': self.score,
            'feedback': self.feedback
        }

class Topic(MongoModel):
    def __init__(self, name, description=None, icon_path=None, created_at=None, _id=None):
        self._id = _id or ObjectId()
        self.name = name
        self.description = description
        self.icon_path = icon_path
        self.created_at = created_at or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'name': self.name,
            'description': self.description,
            'icon_path': self.icon_path,
            'created_at': self.created_at
        }

class Scene(MongoModel):
    def __init__(self, name, topic_id, description=None, icon_path=None, parent_id=None, created_at=None, _id=None):
        self._id = _id or ObjectId()
        self.name = name
        self.description = description
        self.icon_path = icon_path
        self.topic_id = topic_id
        self.parent_id = parent_id
        self.created_at = created_at or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'name': self.name,
            'description': self.description,
            'icon_path': self.icon_path,
            'topic_id': self.topic_id,
            'parent_id': self.parent_id,
            'created_at': self.created_at
        }

class SceneLevel(MongoModel):
    def __init__(self, scene_id, english_level, example_dialogs=None, key_phrases=None, 
                 vocabulary=None, grammar_points=None, created_at=None, _id=None):
        self._id = _id or ObjectId()
        self.scene_id = scene_id
        self.english_level = english_level
        self.example_dialogs = example_dialogs
        self.key_phrases = key_phrases
        self.vocabulary = vocabulary
        self.grammar_points = grammar_points
        self.created_at = created_at or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'scene_id': self.scene_id,
            'english_level': self.english_level,
            'example_dialogs': self.example_dialogs,
            'key_phrases': self.key_phrases,
            'vocabulary': self.vocabulary,
            'grammar_points': self.grammar_points,
            'created_at': self.created_at
        }

class UnfamiliarWord(MongoModel):
    def __init__(self, session_id, user_uid, word, context=None, timestamp=None, _id=None):
        self._id = _id or ObjectId()
        self.session_id = session_id
        self.user_uid = user_uid
        self.word = word
        self.context = context
        self.timestamp = timestamp or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'session_id': self.session_id,
            'user_uid': self.user_uid,
            'word': self.word,
            'context': self.context,
            'timestamp': self.timestamp
        }

class WrongGrammar(MongoModel):
    def __init__(self, session_id, user_uid, wrong_text, correct_text, explanation=None, timestamp=None, _id=None):
        self._id = _id or ObjectId()
        self.session_id = session_id
        self.user_uid = user_uid
        self.wrong_text = wrong_text
        self.correct_text = correct_text
        self.explanation = explanation
        self.timestamp = timestamp or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'session_id': self.session_id,
            'user_uid': self.user_uid,
            'wrong_text': self.wrong_text,
            'correct_text': self.correct_text,
            'explanation': self.explanation,
            'timestamp': self.timestamp
        }

class BestFitWord(MongoModel):
    def __init__(self, session_id, user_uid, original_word, suggested_word, context=None, timestamp=None, _id=None):
        self._id = _id or ObjectId()
        self.session_id = session_id
        self.user_uid = user_uid
        self.original_word = original_word
        self.suggested_word = suggested_word
        self.context = context
        self.timestamp = timestamp or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'session_id': self.session_id,
            'user_uid': self.user_uid,
            'original_word': self.original_word,
            'suggested_word': self.suggested_word,
            'context': self.context,
            'timestamp': self.timestamp
        }

class BetterExpression(MongoModel):
    def __init__(self, session_id, user_uid, original_text, suggested_text, explanation=None, timestamp=None, _id=None):
        self._id = _id or ObjectId()
        self.session_id = session_id
        self.user_uid = user_uid
        self.original_text = original_text
        self.suggested_text = suggested_text
        self.explanation = explanation
        self.timestamp = timestamp or datetime.utcnow()

    def to_dict(self):
        return {
            '_id': self._id,
            'session_id': self.session_id,
            'user_uid': self.user_uid,
            'original_text': self.original_text,
            'suggested_text': self.suggested_text,
            'explanation': self.explanation,
            'timestamp': self.timestamp
        } 