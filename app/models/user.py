from datetime import datetime
from .base import db

# Association table for Person-Scene many-to-many relationship
completed_scenes = db.Table('completed_scenes',
    db.Column('person_id', db.Integer, db.ForeignKey('person.id'), primary_key=True),
    db.Column('scene_id', db.Integer, db.ForeignKey('scene.id'), primary_key=True),
    db.Column('completed_at', db.DateTime, default=datetime.utcnow),
    db.Column('score', db.Integer, nullable=True),  # Optional score/rating for the scene
    db.Column('feedback', db.Text, nullable=True)  # Optional feedback about the scene
)

class Person(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    sessions = db.relationship('ConversationSession', backref='person', lazy=True)
    completed_scenes = db.relationship('Scene',
        secondary=completed_scenes,
        lazy='dynamic',
        backref=db.backref('completed_by', lazy='dynamic')
    )

    def __repr__(self):
        return f'<Person {self.name}>'

    def complete_scene(self, scene, score=None, feedback=None):
        """Mark a scene as completed by this person"""
        if scene not in self.completed_scenes:
            self.completed_scenes.append(scene)
            # Update the association table with additional data
            db.session.execute(
                completed_scenes.update().where(
                    (completed_scenes.c.person_id == self.id) &
                    (completed_scenes.c.scene_id == scene.id)
                ).values(
                    score=score,
                    feedback=feedback
                )
            ) 