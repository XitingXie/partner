
from flask import jsonify, request, current_app, Blueprint

# Create blueprint with url_prefix
bp = Blueprint('test', __name__, url_prefix='/api')

@bp.route('/test', methods=['GET'])
def test_endpoint():
    # Test both logging and print
    current_app.logger.info("Test endpoint hit! (from logger)")
    print("Test endpoint hit! (from print)", flush=True)
    return jsonify({"message": "Test endpoint working"})

@bp.route('/test-post', methods=['POST'])
def test_post():
    print("\n=== TEST POST ENDPOINT CALLED ===", flush=True)
    data = request.get_json()
    print(f"Received POST data: {data}", flush=True)
    return jsonify({"message": "Test POST working", "received": data})