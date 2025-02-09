from datetime import datetime
from .base import db

class UnfamiliarWord(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id'), nullable=False)
    person_uid = db.Column(db.String(128), db.ForeignKey('person.uid'), nullable=False)
    word = db.Column(db.String(100), nullable=False)
    context = db.Column(db.Text, nullable=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

class WrongGrammar(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id'), nullable=False)
    person_uid = db.Column(db.String(128), db.ForeignKey('person.uid'), nullable=False)
    wrong_text = db.Column(db.Text, nullable=False)
    correct_text = db.Column(db.Text, nullable=False)
    explanation = db.Column(db.Text, nullable=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

class BestFitWord(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id'), nullable=False)
    person_uid = db.Column(db.String(128), db.ForeignKey('person.uid'), nullable=False)
    original_word = db.Column(db.String(100), nullable=False)
    suggested_word = db.Column(db.String(100), nullable=False)
    context = db.Column(db.Text, nullable=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

class BetterExpression(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id'), nullable=False)
    person_uid = db.Column(db.String(128), db.ForeignKey('person.uid'), nullable=False)
    original_text = db.Column(db.Text, nullable=False)
    suggested_text = db.Column(db.Text, nullable=False)
    explanation = db.Column(db.Text, nullable=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow) 