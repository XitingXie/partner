from datetime import datetime
from .base import db

class Topic(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False, unique=True)
    description = db.Column(db.Text, nullable=True)
    keywords = db.Column(db.Text, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    scenes = db.relationship('Scene', backref='topic', lazy=True)

    def __repr__(self):
        return f'<Topic "{self.name}">'

class Scene(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False)
    context = db.Column(db.Text, nullable=True)
    example_dialogs = db.Column(db.Text, nullable=True)
    key_phrases = db.Column(db.Text, nullable=True)
    topic_id = db.Column(db.Integer, db.ForeignKey('topic.id'), nullable=False)
    parent_id = db.Column(db.Integer, db.ForeignKey('scene.id'), nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    sessions = db.relationship('ConversationSession', backref='scene', lazy=True)
    children = db.relationship('Scene', backref=db.backref('parent', remote_side=[id]), lazy=True)

    def __repr__(self):
        return f'<Scene "{self.name}" for Topic {self.topic_id}>' 