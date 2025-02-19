import sys
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Force flush of print statements
sys.stdout.reconfigure(line_buffering=True)

# Only import create_app when running locally
if __name__ == '__main__':
    from app import create_app

    # Create Flask app
    app = create_app()

    print("\n=== Starting Flask Server ===", flush=True)
    with app.app_context():
        print("\nRegistered routes:", flush=True)
        for rule in app.url_map.iter_rules():
            print(f"{rule.endpoint}: {rule.methods} {rule}", flush=True)
    print("\n=== Server Ready on http://localhost:8008 ===\n", flush=True)

    # Do not include app.run() for production, it should be handled by WSGI
    app.run(debug=True, port=8008, host='0.0.0.0')  # Only for local testing