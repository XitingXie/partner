from datetime import datetime
from .base import db

class ConversationSession(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    person_uid = db.Column(db.String(128), db.ForeignKey('person.uid'), nullable=False)
    scene_id = db.Column(db.Integer, db.ForeignKey('scene.id'), nullable=False)
    started_at = db.Column(db.DateTime, default=datetime.utcnow)
    ended_at = db.Column(db.DateTime, nullable=True)
    
    # Relationships
    messages = db.relationship('Message', backref='session', lazy=True)
    scene = db.relationship('Scene', backref='sessions', lazy=True)

    def __repr__(self):
        return f'<ConversationSession {self.id}>'

class Message(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id'), nullable=False)
    person_uid = db.Column(db.String(128), db.ForeignKey('person.uid'), nullable=True)
    role = db.Column(db.String(50), nullable=False)  # 'user' or 'assistant'
    text = db.Column(db.Text, nullable=False)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<Message {self.role}: {self.text[:20]}...>' 