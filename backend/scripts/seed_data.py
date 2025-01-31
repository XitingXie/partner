import sys
import os
import datetime

# Add the backend directory to Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import create_app, db
from app.models import Topic, Scene, SceneLevel
from app.llm.client import LLMClient
import json
from typing import List, Set
import hashlib
import re

ENGLISH_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
RESPONSE_LOG_DIR = "llm_responses"

# Guidelines for different English levels
level_guidelines = {
    'A1': 'Use basic greetings and simple present tense. Focus on everyday objects and actions. Keep sentences very short and simple.',
    'A2': 'Use simple past and present continuous. Include basic questions and answers. Cover daily life situations.',
    'B1': 'Introduce more complex tenses. Include opinions and preferences. Discuss familiar topics in detail.',
    'B2': 'Use a range of tenses confidently. Express and justify opinions. Handle unexpected situations.',
    'C1': 'Use idiomatic expressions. Handle abstract discussions. Express subtle meanings.',
    'C2': 'Use sophisticated vocabulary. Handle complex academic topics. Express nuanced opinions.'
}

# Predefined categories to guide topic generation
TOPIC_CATEGORIES = [
    "Daily Life", "Work & Career", "Travel", "Education", "Health & Wellness",
    "Shopping", "Entertainment", "Technology", "Social Relationships", "Culture",
    "Food & Dining", "Sports & Fitness", "Home & Living", "Nature & Environment",
    "Transportation", "Emergency Situations", "Business", "Arts & Media",
    "Science & Innovation", "Community Services"
]

class DuplicateChecker:
    def __init__(self, db):
        self.db = db
    
    def is_duplicate_topic(self, name: str, description: str) -> bool:
        """Check if similar topic exists in database"""
        # Check for exact name match
        existing_topic = Topic.query.filter_by(name=name).first()
        if existing_topic:
            return True
        
        # Check for similar names (case insensitive)
        similar_topics = Topic.query.filter(
            Topic.name.ilike(f"%{name}%")
        ).all()
        return len(similar_topics) > 0
    
    def is_duplicate_scene(self, name: str, context: str) -> bool:
        """Check if similar scene exists in database"""
        # Check for exact name match
        existing_scene = Scene.query.filter_by(name=name).first()
        if existing_scene:
            return True
        
        # Check for similar names (case insensitive)
        similar_scenes = Scene.query.filter(
            Scene.name.ilike(f"%{name}%")
        ).all()
        return len(similar_scenes) > 0

def generate_topic_prompt(topic_number: int, used_categories: Set[str]) -> str:
    available_categories = [cat for cat in TOPIC_CATEGORIES if cat not in used_categories]
    if not available_categories:
        used_categories.clear()  # Reset if all categories used
        available_categories = TOPIC_CATEGORIES
    
    category = available_categories[topic_number % len(available_categories)]
    used_categories.add(category)

    return f"""Generate a unique language learning topic in the category of "{category}" with 5 different scenes.
        Return in this exact JSON format:
        {{
            "name": "Specific and unique topic name",
            "description": "Detailed topic description",
            "scenes": [
                {{
                    "name": "Unique scene name",
                    "context": "Specific scene context/setting description"
                }},
                // exactly 5 scenes required
            ]
        }}

        IMPORTANT:
        1. RETURN ONLY THE JSON OBJECT - NO INTRODUCTION OR EXTRA TEXT
        2. THE TOPIC MUST BE A SPECIFIC SUBSET OF {category}
        3. INCLUDE EXACTLY 5 SCENES
        4. EACH SCENE MUST BE UNIQUE
        5. USE PROPER JSON FORMAT WITH NO TRAILING COMMAS
        """

def generate_scene_level_prompt(scene_name: str, level: str) -> str:
    return f"""You are a JSON generator. Your task is to generate a conversation for English level {level} about "{scene_name}".
    YOU MUST:
    1. Return ONLY valid JSON
    2. Follow the EXACT structure below
    3. Include NO explanation text
    4. Use NO special characters
    5. Keep all text on a single line
    6. Use double quotes for all strings
    7. Include NO trailing commas
    
    Return in this exact JSON format:
    {{
      "example_dialogs": {{
        "speaker_1": "Person A",
        "speaker_2": "Person B",
        "dialogue": [
          {{"speaker": "Person A", "text": "Hello! How are you?"}},
          {{"speaker": "Person B", "text": "I'm good, thanks! How about you?"}}
        ]
      }},
      "key_phrases": [
        "Hello! How are you?",
        "I'm good, thanks!",
        "How about you?"
      ],
      "vocabulary": [
        {{"word": "good", "meaning": "Something positive or of high quality"}},
        {{"word": "thanks", "meaning": "A polite expression of gratitude"}}
      ],
      "grammar_points": [
        {{"point": "Present Simple", "example": "I am happy"}},
        {{"point": "Question Formation", "example": "How are you?"}}
      ]
    }}
    
    Generate a JSON conversation on the topic "{scene_name}" at level "{level}":
    """

def clean_json_response(response: str) -> str:
    """Clean up common JSON formatting issues in LLM responses"""
    # Find JSON object in response
    json_string_match = re.search(r'\{.*\}', response, re.DOTALL)
    if not json_string_match:
        return None
    
    json_str = json_string_match.group(0)
    
    # Fix common JSON formatting issues
    json_str = json_str.replace(',]', ']')  # Remove trailing commas in arrays
    json_str = json_str.replace(',}', '}')  # Remove trailing commas in objects
    
    # Fix unclosed arrays/objects
    open_brackets = json_str.count('[') - json_str.count(']')
    open_braces = json_str.count('{') - json_str.count('}')
    
    # Add missing closing brackets/braces
    json_str += ']' * open_brackets + '}' * open_braces
    
    return json_str

def save_level_response(scene_name: str, level: str, response: str, success: bool = True):
    """Log problematic AI responses to file"""
    if not os.path.exists(RESPONSE_LOG_DIR):
        os.makedirs(RESPONSE_LOG_DIR)
    
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{RESPONSE_LOG_DIR}/{timestamp}_{scene_name}_{level}.txt"
    
    with open(filename, "w") as f:
        f.write(f"Scene: {scene_name}\n")
        f.write(f"Level: {level}\n")
        f.write("\nResponse:\n")
        f.write(response)

def validate_level_data(data: dict) -> bool:
    """Validate the structure of level data"""
    try:
        # Check required fields
        required_fields = ['example_dialogs', 'key_phrases', 'vocabulary', 'grammar_points']
        if not all(field in data for field in required_fields):
            return False
        
        # Validate example_dialogs structure
        dialogs = data['example_dialogs']
        if not all(field in dialogs for field in ['speaker_1', 'speaker_2', 'dialogue']):
            return False
        if not isinstance(dialogs['dialogue'], list):
            return False
        
        # Validate arrays
        if not isinstance(data['key_phrases'], list):
            return False
        if not isinstance(data['vocabulary'], list):
            return False
        if not isinstance(data['grammar_points'], list):
            return False
        
        return True
    except:
        return False

def seed_database():
    app = create_app()
    llm_client = LLMClient()
    duplicate_checker = DuplicateChecker(db)
    used_categories: Set[str] = set()
    
    with app.app_context():
        topic_count = 0
        attempt = 0
        
        while topic_count < 2 and attempt < 150:  # Max attempts to avoid infinite loop
            attempt += 1
            try:
                topic_response = llm_client.get_completion(
                    prompt=generate_topic_prompt(attempt, used_categories),
                    message=f"Generate unique topic number {topic_count + 1}",
                    role="system",
                    temperature=0.8
                )
                topic_data = json.loads(topic_response)
                
                # Check for duplicates
                if duplicate_checker.is_duplicate_topic(topic_data['name'], topic_data['description']):
                    print(f"Duplicate topic detected: {topic_data['name']}, retrying...")
                    continue
                
                # Create topic
                topic = Topic(
                    name=topic_data['name'],
                    description=topic_data['description'],

                )
                db.session.add(topic)
                db.session.flush()
                
                # Process scenes
                valid_scenes = []
                for scene_data in topic_data['scenes']:
                    if not duplicate_checker.is_duplicate_scene(scene_data['name'], scene_data['context']):
                        valid_scenes.append(scene_data)
                
                if len(valid_scenes) < 3:  # Minimum scene requirement
                    print(f"Not enough unique scenes for topic: {topic.name}, retrying...")
                    db.session.rollback()
                    continue
                
                # Create scenes for this topic
                for scene_data in valid_scenes:
                    scene = Scene(
                        name=scene_data['name'],
                        description=scene_data['context'],
                        topic_id=topic.id
                    )
                    db.session.add(scene)
                    db.session.flush()  # Get scene ID
                    
                    print(f"Created scene: {scene.name}")
                    
                    # Generate content for each English level
                    for level in ENGLISH_LEVELS:
                        max_attempts = 3
                        attempt = 0
                        while attempt < max_attempts:
                            attempt += 1
                            try:
                                level_response = llm_client.get_completion(
                                    prompt=generate_scene_level_prompt(scene.name, level),
                                    message=f"Generate content for {level} level",
                                    temperature=0.5,
                                    role="system"
                                )
                                
                                # Clean and validate JSON response
                                cleaned_response = clean_json_response(level_response)
                                if cleaned_response:
                                    level_data = json.loads(cleaned_response)
                                    if validate_level_data(level_data):
                                        scene_level = SceneLevel(
                                            scene_id=scene.id,
                                            english_level=level,
                                            example_dialogs=json.dumps(level_data['example_dialogs']),
                                            key_phrases=','.join(level_data['key_phrases']),
                                            vocabulary=json.dumps(level_data['vocabulary'], ensure_ascii=False),
                                            grammar_points=json.dumps(level_data['grammar_points'], ensure_ascii=False)
                                        )
                                        db.session.add(scene_level)
                                        print(f"Created {level} content for scene: {scene.name}")
                                        break  # Success - exit retry loop
                                
                                if attempt == max_attempts:
                                    print(f"Failed to generate valid content after {max_attempts} attempts")
                            except Exception as e:
                                print(f"Error in attempt {attempt}: {str(e)}")
                                if attempt == max_attempts:
                                    save_level_response(scene.name, level, level_response, success=False)
                    
                    db.session.commit()
                    print(f"Committed scene {scene.name} with all levels")
                
                topic_count += 1
                print(f"Successfully created topic {topic_count}/100: {topic.name}")
                
            except Exception as e:
                print(f"Error in attempt {attempt}: {str(e)}")
                db.session.rollback()
                continue

def clean_database():
    """Clean all data from the database"""
    app = create_app()
    
    with app.app_context():
        print("Cleaning database...")
        try:
            # Drop all tables in correct order due to foreign key constraints
            SceneLevel.__table__.drop(db.engine, checkfirst=True)
            Scene.__table__.drop(db.engine, checkfirst=True)
            Topic.__table__.drop(db.engine, checkfirst=True)
            
            # Recreate all tables
            db.create_all()
            print("Database cleaned successfully")
            
        except Exception as e:
            print(f"Error cleaning database: {str(e)}")
            raise

if __name__ == '__main__':
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == 'clean':
        clean_database()
    
    seed_database() 