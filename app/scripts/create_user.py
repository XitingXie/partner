import sys
from pathlib import Path
import os

# Add parent directory to Python path
sys.path.append(str(Path(__file__).parent.parent.parent))

from app import create_app, db
from app.models import Person
from sqlalchemy import text

def create_test_user():
    app = create_app()
    with app.app_context():
        try:
            # Print database URI
            print("Database URI:", app.config['SQLALCHEMY_DATABASE_URI'])
            print("Database path:", os.path.abspath(os.path.join(os.getcwd(), 'instance', 'app.db')))
            
            # Check if database is accessible
            result = db.session.execute(text("SELECT name FROM sqlite_master WHERE type='table'")).fetchall()
            print("Available tables:", result)
            
            # Check if test user already exists
            existing_user = Person.query.filter_by(name="Test User").first()
            if existing_user:
                print(f"Test user already exists: {existing_user.name} (ID: {existing_user.id})")
                return existing_user
            
            # Create a test user if none exists
            user = Person(name="Test User")
            db.session.add(user)
            db.session.commit()
            print(f"Created new test user: {user.name} (ID: {user.id})")
            
            # Verify users in database
            users = db.session.execute(text("SELECT * FROM person")).fetchall()
            print("All users:", users)
            
            return user
            
        except Exception as e:
            print(f"Error: {str(e)}")
            db.session.rollback()
            return None

if __name__ == "__main__":
    create_test_user() 