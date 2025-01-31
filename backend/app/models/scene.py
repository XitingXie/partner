from datetime import datetime
from .base import db

class Topic(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False, unique=True)
    description = db.Column(db.Text, nullable=True)
    icon_path = db.Column(db.String(255), nullable=True)  # Path to icon file
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    scenes = db.relationship('Scene', backref='topic', lazy=True)

    def __repr__(self):
        return f'<Topic "{self.name}">'

class SceneLevel(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    scene_id = db.Column(db.Integer, db.ForeignKey('scene.id'), nullable=False)
    english_level = db.Column(db.String(2), nullable=False)  # A1, A2, B1, B2, C1, C2
    example_dialogs = db.Column(db.Text, nullable=True)
    key_phrases = db.Column(db.Text, nullable=True)
    vocabulary = db.Column(db.Text, nullable=True)
    grammar_points = db.Column(db.Text, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<SceneLevel {self.english_level} for Scene {self.scene_id}>'

class Scene(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text, nullable=True)
    icon_path = db.Column(db.String(255), nullable=True)  # Path to icon file
    topic_id = db.Column(db.Integer, db.ForeignKey('topic.id'), nullable=False)
    parent_id = db.Column(db.Integer, db.ForeignKey('scene.id'), nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    levels = db.relationship('SceneLevel', backref='scene', lazy=True)
    sessions = db.relationship('ConversationSession', backref='scene', lazy=True)
    children = db.relationship('Scene', backref=db.backref('parent', remote_side=[id]), lazy=True)

    def __repr__(self):
        return f'<Scene "{self.name}" for Topic {self.topic_id}>' 