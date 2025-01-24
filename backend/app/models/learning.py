from datetime import datetime
from .base import db

class UnfamiliarWord(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    word = db.Column(db.String(100), nullable=False)  # The unfamiliar word
    definition = db.Column(db.Text, nullable=True)  # Definition or explanation
    example = db.Column(db.Text, nullable=True)  # Example usage of the word
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id', name='fk_unfamiliar_word_session'), nullable=False)
    person_id = db.Column(db.Integer, db.ForeignKey('person.id', name='fk_unfamiliar_word_person'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<UnfamiliarWord {self.word} in Session {self.session_id}>'

class WrongGrammar(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    incorrect_text = db.Column(db.Text, nullable=False)  # The incorrect grammar text
    correct_text = db.Column(db.Text, nullable=False)  # The corrected version
    explanation = db.Column(db.Text, nullable=True)  # Explanation of the grammar rule
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id', name='fk_wrong_grammar_session'), nullable=False)
    person_id = db.Column(db.Integer, db.ForeignKey('person.id', name='fk_wrong_grammar_person'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<WrongGrammar "{self.incorrect_text}" in Session {self.session_id}>'

class BestFitWord(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    context = db.Column(db.Text, nullable=False)  # The context where the word is used
    original_word = db.Column(db.String(100), nullable=False)  # The original word used
    better_word = db.Column(db.String(100), nullable=False)  # The suggested better word
    explanation = db.Column(db.Text, nullable=True)  # Explanation of why this word is a better fit
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id', name='fk_best_fit_word_session'), nullable=False)
    person_id = db.Column(db.Integer, db.ForeignKey('person.id', name='fk_best_fit_word_person'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<BestFitWord "{self.better_word}" for "{self.original_word}" in Session {self.session_id}>'

class BetterExpression(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    original_text = db.Column(db.Text, nullable=False)  # The original expression
    better_text = db.Column(db.Text, nullable=False)  # The improved expression
    context = db.Column(db.Text, nullable=True)  # The context where the expression is used
    explanation = db.Column(db.Text, nullable=True)  # Explanation of why this expression is better
    session_id = db.Column(db.Integer, db.ForeignKey('conversation_session.id', name='fk_better_expression_session'), nullable=False)
    person_id = db.Column(db.Integer, db.ForeignKey('person.id', name='fk_better_expression_person'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<BetterExpression "{self.better_text}" for "{self.original_text}" in Session {self.session_id}>' 