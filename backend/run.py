import sys
from app import create_app, db

# Force flush of print statements
sys.stdout.reconfigure(line_buffering=True)

app = create_app()

if __name__ == '__main__':
    print("\n=== Starting Flask Server ===", flush=True)
    with app.app_context():
        print("\nRegistered routes:", flush=True)
        for rule in app.url_map.iter_rules():
            print(f"{rule.endpoint}: {rule.methods} {rule}", flush=True)
    print("\n=== Server Ready on http://localhost:8008 ===\n", flush=True)
    app.run(debug=True, port=8008, host='0.0.0.0') 