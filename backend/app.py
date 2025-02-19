import sys
from app import create_app
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Force flush of print statements
sys.stdout.reconfigure(line_buffering=True)

# Create Flask app
app = create_app()

# Only run the app when executed directly (for local testing)
if __name__ == '__main__':
    print("\n=== Starting Flask Server ===", flush=True)
    with app.app_context():
        print("\nRegistered routes:", flush=True)
        for rule in app.url_map.iter_rules():
            print(f"{rule.endpoint}: {rule.methods} {rule}", flush=True)
    print("\n=== Server Ready on http://localhost:8008 ===\n", flush=True)
    # Do not include app.run() for production, it should be handled by WSGI