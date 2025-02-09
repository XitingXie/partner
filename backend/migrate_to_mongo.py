import sqlite3
from pymongo import MongoClient
from datetime import datetime
from bson import ObjectId

# Connect to SQLite
sqlite_conn = sqlite3.connect('instance/app.db')
sqlite_conn.row_factory = sqlite3.Row
cursor = sqlite_conn.cursor()

# Connect to MongoDB
mongo_client = MongoClient('mongodb://localhost:27017/')
mongo_db = mongo_client['lingomia']

# Migrate topics
print("Migrating topics...")
cursor.execute('SELECT * FROM topic')
topics = cursor.fetchall()
for topic in topics:
    topic_dict = dict(topic)
    # Convert SQLite datetime to Python datetime
    if topic_dict['created_at']:
        topic_dict['created_at'] = datetime.fromisoformat(topic_dict['created_at'])
    # Convert id to _id and remove old id
    topic_dict['_id'] = ObjectId()
    old_id = topic_dict.pop('id')
    # Insert into MongoDB
    mongo_db.topics.insert_one(topic_dict)
    print(f"Migrated topic: {topic_dict['name']}")

# Get the topic id mapping (old SQLite id to new MongoDB _id)
topic_id_map = {}
for topic in mongo_db.topics.find():
    cursor.execute('SELECT id FROM topic WHERE name = ?', (topic['name'],))
    old_id = cursor.fetchone()[0]
    topic_id_map[old_id] = topic['_id']

# Migrate scenes
print("\nMigrating scenes...")
cursor.execute('SELECT * FROM scene')
scenes = cursor.fetchall()
for scene in scenes:
    scene_dict = dict(scene)
    # Convert SQLite datetime to Python datetime
    if scene_dict['created_at']:
        scene_dict['created_at'] = datetime.fromisoformat(scene_dict['created_at'])
    # Convert id to _id and remove old id
    scene_dict['_id'] = ObjectId()
    old_id = scene_dict.pop('id')
    # Update topic_id reference
    scene_dict['topic_id'] = topic_id_map[scene_dict['topic_id']]
    # Update parent_id reference if exists
    if scene_dict['parent_id']:
        scene_dict['parent_id'] = ObjectId()  # We'll update this in a second pass
    # Insert into MongoDB
    mongo_db.scenes.insert_one(scene_dict)
    print(f"Migrated scene: {scene_dict['name']}")

# Get the scene id mapping (old SQLite id to new MongoDB _id)
scene_id_map = {}
for scene in mongo_db.scenes.find():
    cursor.execute('SELECT id FROM scene WHERE name = ?', (scene['name'],))
    old_id = cursor.fetchone()[0]
    scene_id_map[old_id] = scene['_id']

# Update scene parent_id references
for scene in mongo_db.scenes.find({'parent_id': {'$ne': None}}):
    cursor.execute('SELECT parent_id FROM scene WHERE name = ?', (scene['name'],))
    old_parent_id = cursor.fetchone()[0]
    if old_parent_id:
        mongo_db.scenes.update_one(
            {'_id': scene['_id']},
            {'$set': {'parent_id': scene_id_map[old_parent_id]}}
        )

# Migrate scene levels
print("\nMigrating scene levels...")
cursor.execute('SELECT * FROM scene_level')
scene_levels = cursor.fetchall()
for level in scene_levels:
    level_dict = dict(level)
    # Convert SQLite datetime to Python datetime
    if level_dict['created_at']:
        level_dict['created_at'] = datetime.fromisoformat(level_dict['created_at'])
    # Convert id to _id and remove old id
    level_dict['_id'] = ObjectId()
    old_id = level_dict.pop('id')
    # Update scene_id reference
    level_dict['scene_id'] = scene_id_map[level_dict['scene_id']]
    # Insert into MongoDB
    mongo_db.scene_levels.insert_one(level_dict)
    print(f"Migrated scene level: {level_dict['english_level']} for scene {level_dict['scene_id']}")

print("\nMigration completed!")

# Close connections
sqlite_conn.close()
mongo_client.close() 