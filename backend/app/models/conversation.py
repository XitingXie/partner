from datetime import datetime
from .base import db

class ConversationSession(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    person_id = db.Column(db.Integer, db.ForeignKey('person.id'), nullable=False)
    scene_id = db.Column(db.Integer, db.ForeignKey('scene.id'), nullable=True)
    started_at = db.Column(db.DateTime, default=datetime.utcnow)
    messages = db.relationship('Message', backref='session', lazy=True)
    unfamiliar_words = db.relationship('UnfamiliarWord', backref='session', lazy=True)
    wrong_grammar = db.relationship('WrongGrammar', backref='session', lazy=True)
    best_fit_words = db.relationship('BestFitWord', backref='session', lazy=True)
    better_expressions = db.relationship('BetterExpression', backref='session', lazy=True)

    def __repr__(self):
        return f'<ConversationSession {self.id} by Person {self.person_id}>'

class Message(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id'), nullable=False)
    role = db.Column(db.String(20), nullable=False)  # 'user' or 'assistant'
    text = db.Column(db.Text, nullable=False)
    voice = db.Column(db.Text, nullable=True)  # Optional: URL or path to voice recording
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<Message {self.role}: {self.text[:20]}...>' 