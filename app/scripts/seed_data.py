import sys
from pathlib import Path

# Add parent directory to Python path
sys.path.append(str(Path(__file__).parent.parent.parent))

from app import create_app, db
from app.models import Person, Topic, Scene, ConversationSession

def seed_data():
    app = create_app()
    with app.app_context():
        # Create a mock user
        user = Person(name="Test User")
        db.session.add(user)
        
        # Create some topics
        topic1 = Topic(
            name="Daily Life",
            description="Common scenarios in everyday life",
            keywords="daily,life,routine"
        )
        topic2 = Topic(
            name="Travel",
            description="Travel and tourism scenarios",
            keywords="travel,tourism,vacation"
        )
        db.session.add_all([topic1, topic2])
        
        # Create some scenes
        scene1 = Scene(
            name="At the Coffee Shop",
            context="Ordering coffee and having a casual conversation",
            key_phrases="coffee,order,drink,menu",
            topic=topic1
        )
        scene2 = Scene(
            name="Airport Check-in",
            context="Checking in for a flight at the airport",
            key_phrases="flight,check-in,passport,luggage",
            topic=topic2
        )
        db.session.add_all([scene1, scene2])
        
        # Create a session
        session = ConversationSession(
            person=user,
            scene=scene1
        )
        db.session.add(session)
        
        # Commit all changes
        db.session.commit()
        
        print("Database seeded successfully!")
        print(f"Created user: {user.name} (ID: {user.id})")
        print(f"Created topics: {topic1.name}, {topic2.name}")
        print(f"Created scenes: {scene1.name}, {scene2.name}")

if __name__ == "__main__":
    seed_data() 