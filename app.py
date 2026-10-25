import os
import sqlite3
from datetime import datetime, timezone
from functools import wraps
from flask import Flask, request, jsonify, g

app = Flask(__name__)
DB_PATH = os.environ.get("VANSTORE_DB", "vanstore.db")
API_TOKEN = os.environ.get("VANSTORE_API_TOKEN")
if not API_TOKEN:
    raise RuntimeError("Set VANSTORE_API_TOKEN to a long random secret before starting.")

def db():
    if "db" not in g:
        g.db = sqlite3.connect(DB_PATH)
        g.db.row_factory = sqlite3.Row
    return g.db

@app.teardown_appcontext
def close_db(_error):
    conn = g.pop("db", None)
    if conn is not None:
        conn.close()

def init_db():
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute("""
        CREATE TABLE IF NOT EXISTS devices (
            device_id TEXT PRIMARY KEY,
            device_name TEXT NOT NULL,
            latitude REAL,
            longitude REAL,
            accuracy REAL,
            battery_pct INTEGER,
            last_seen TEXT NOT NULL
        )
        """)

def authorized(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        supplied = request.headers.get("Authorization", "")
        if supplied != f"Bearer {API_TOKEN}":
            return jsonify({"error": "unauthorized"}), 401
        return fn(*args, **kwargs)
    return wrapper

@app.get("/api/health")
def health():
    return jsonify({"status": "ok"})

@app.get("/api/devices")
@authorized
def devices():
    rows = db().execute("SELECT * FROM devices ORDER BY last_seen DESC").fetchall()
    return jsonify([dict(r) for r in rows])

@app.post("/api/location")
@authorized
def update_location():
    data = request.get_json(silent=True) or {}
    required = ("device_id", "device_name", "latitude", "longitude")
    if any(k not in data for k in required):
        return jsonify({"error": "missing required fields", "required": required}), 400
    try:
        lat, lon = float(data["latitude"]), float(data["longitude"])
        accuracy = float(data.get("accuracy", 0))
        battery = data.get("battery_pct")
        battery = int(battery) if battery is not None else None
        if not (-90 <= lat <= 90 and -180 <= lon <= 180):
            raise ValueError()
        if battery is not None and not (0 <= battery <= 100):
            raise ValueError()
    except (ValueError, TypeError):
        return jsonify({"error": "invalid coordinate, accuracy, or battery value"}), 400
    now = datetime.now(timezone.utc).isoformat()
    db().execute("""
        INSERT INTO devices(device_id, device_name, latitude, longitude, accuracy, battery_pct, last_seen)
        VALUES(?,?,?,?,?,?,?)
        ON CONFLICT(device_id) DO UPDATE SET
          device_name=excluded.device_name, latitude=excluded.latitude,
          longitude=excluded.longitude, accuracy=excluded.accuracy,
          battery_pct=excluded.battery_pct, last_seen=excluded.last_seen
    """, (str(data["device_id"])[:128], str(data["device_name"])[:128],
          lat, lon, accuracy, battery, now))
    db().commit()
    return jsonify({"ok": True, "last_seen": now})

if __name__ == "__main__":
    init_db()
    # Local development only. Use a production WSGI server and HTTPS in deployment.
    app.run(host="127.0.0.1", port=int(os.environ.get("PORT", "5000")), debug=False)
