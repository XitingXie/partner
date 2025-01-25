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
        
        # Create topics
        coffee_topic = Topic(
            name="Coffee Shop",
            description="Practice ordering and chatting at a coffee shop"
        )
        db.session.add(coffee_topic)
        
        # Create scenes with topic_id
        scene = Scene(
            name="Ordering Coffee",
            context="You are at a coffee shop and want to order a drink",
            key_phrases="coffee, latte, espresso, order",
            topic_id=coffee_topic.id
        )
        db.session.add(scene)
        
        # Create a session
        session = ConversationSession(
            person=user,
            scene=scene
        )
        db.session.add(session)
        
        # Commit all changes
        db.session.commit()
        
        print("Database seeded successfully!")
        print(f"Created user: {user.name} (ID: {user.id})")
        print(f"Created topics: {coffee_topic.name}")
        print(f"Created scenes: {scene.name}")

if __name__ == "__main__":
    seed_data() 