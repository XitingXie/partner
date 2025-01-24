import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent.parent.parent))

from app import create_app, db
from app.models import Person
from sqlalchemy import text

def cleanup_users():
    app = create_app()
    with app.app_context():
        try:
            # Delete all existing users
            Person.query.delete()
            db.session.commit()
            print("Deleted all users")
            
            # Verify deletion
            users = db.session.execute(text("SELECT * FROM person")).fetchall()
            print("Users after cleanup:", users)
            
        except Exception as e:
            print(f"Error: {str(e)}")
            db.session.rollback()

if __name__ == "__main__":
    cleanup_users() 