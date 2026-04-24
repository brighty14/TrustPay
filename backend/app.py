from flask import Flask, request, jsonify
from flask_cors import CORS
import base64
import cv2
import numpy as np
from pymongo import MongoClient
import random
from datetime import datetime
from scipy.spatial.distance import cosine

try:
    from keras_facenet import FaceNet
    facenet_embedder = FaceNet()
except Exception as e:
    facenet_embedder = None
    print("FaceNet unavailable, using OpenCV fallback:", e)

FACE_MATCH_THRESHOLD = 0.75 if facenet_embedder is not None else 0.86

app = Flask(__name__)
CORS(app)

# -------------------------------
# MongoDB connection
# -------------------------------
client = MongoClient("mongodb://localhost:27017/")
db = client["trustpay_db"]

users = db["users"]
transactions = db["transactions"]


@app.route("/")
def home():
    return "TrustPay Backend Running"


# -------------------------------
# Generate Unique UPI ID
# -------------------------------
def generate_upi(name):
    username = name.lower().replace(" ", ".")

    while True:
        random_number = random.randint(1000, 9999)
        upi_id = f"{username}{random_number}@trustpay"

        if not users.find_one({"upi_id": upi_id}):
            return upi_id


def generate_mobile():
    while True:
        mobile = str(random.randint(6000000000, 9999999999))
        if not users.find_one({"mobile": mobile}):
            return mobile


def decode_base64_image(face_image_base64):
    if "," in face_image_base64:
        face_image_base64 = face_image_base64.split(",", 1)[1]

    image_bytes = base64.b64decode(face_image_base64)
    np_array = np.frombuffer(image_bytes, np.uint8)
    return cv2.imdecode(np_array, cv2.IMREAD_COLOR)


def extract_face_embedding(face_image_base64):
    image = decode_base64_image(face_image_base64)
    if image is None:
        return None

    if facenet_embedder is not None:
        rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        detections = facenet_embedder.extract(rgb_image, threshold=0.90)

        if len(detections) == 1:
            return detections[0]["embedding"].astype("float32").tolist()

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gray = cv2.GaussianBlur(gray, (3, 3), 0)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    gray = clahe.apply(gray)

    frontal_detector = cv2.CascadeClassifier(
        cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    )
    profile_detector = cv2.CascadeClassifier(
        cv2.data.haarcascades + "haarcascade_profileface.xml"
    )

    faces = frontal_detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5)
    if len(faces) != 1:
        faces = profile_detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5)

    if len(faces) != 1:
        flipped_gray = cv2.flip(gray, 1)
        faces = profile_detector.detectMultiScale(flipped_gray, scaleFactor=1.1, minNeighbors=5)
        if len(faces) == 1:
            gray = flipped_gray

    if len(faces) == 1:
        x, y, w, h = faces[0]
        face_crop = gray[y:y + h, x:x + w]
    else:
        height, width = gray.shape
        crop_size = int(min(height, width) * 0.7)
        start_x = max((width - crop_size) // 2, 0)
        start_y = max((height - crop_size) // 2, 0)
        face_crop = gray[start_y:start_y + crop_size, start_x:start_x + crop_size]

        if face_crop.size == 0:
            return None
    face_crop = cv2.resize(face_crop, (16, 8))
    face_crop = cv2.equalizeHist(face_crop)
    embedding = face_crop.astype("float32").flatten() / 255.0
    return embedding.tolist()


def cosine_similarity(first_embedding, second_embedding):
    first_vector = np.array(first_embedding, dtype=np.float32)
    second_vector = np.array(second_embedding, dtype=np.float32)

    if first_vector.shape != second_vector.shape:
        return None

    try:
        dist = cosine(first_vector, second_vector)
        if np.isnan(dist):
            return None
        return 1 - dist
    except Exception as e:
        return None


# -------------------------------
# Register API
# -------------------------------
@app.route("/register", methods=["POST"])
def register():

    data = request.get_json()

    if not data:
        return jsonify({"message": "No data received"}), 400

    print("REGISTER DATA:", {
        key: value for key, value in data.items()
        if not key.endswith("face_image")
    })

    name = data.get("username") or data.get("name")
    email = data.get("email")
    mobile = data.get("mobile")
    password = data.get("password")
    upi_pin = data.get("upi_pin")
    balance = data.get("balance")
    front_face_image = data.get("front_face_image") or data.get("face_image")
    left_face_image = data.get("left_face_image") or data.get("face_image")
    right_face_image = data.get("right_face_image") or data.get("face_image")

    if not all([name, email, mobile, password, upi_pin,
                front_face_image, left_face_image, right_face_image]) or balance is None or str(balance).strip() == "":
        return jsonify({
            "status": "error",
            "message": "All fields and front/left/right face images are required"
        }), 400

    if len(str(upi_pin)) != 4:
        return jsonify({"status": "error", "message": "UPI PIN must be 4 digits"}), 400

    try:
        balance = float(balance)
    except:
        return jsonify({"status": "error", "message": "Invalid balance"}), 400

    if users.find_one({"email": email}):
        return jsonify({"status": "error", "message": "Email already registered"}), 400

    if users.find_one({"mobile": mobile}):
        return jsonify({"status": "error", "message": "Mobile already registered"}), 400

    front_embedding = extract_face_embedding(front_face_image)
    left_embedding = extract_face_embedding(left_face_image)
    right_embedding = extract_face_embedding(right_face_image)

    if front_embedding is None:
        return jsonify({
            "status": "error",
            "message": "Could not detect front face. Please recapture in good light"
        }), 400

    if left_embedding is None:
        left_embedding = front_embedding

    if right_embedding is None:
        right_embedding = front_embedding

    upi_id = generate_upi(name)

    user = {
        "username": name,
        "name": name,
        "email": email,
        "mobile": mobile,
        "password": password,
        "upi_id": upi_id,
        "upi_pin": str(upi_pin),
        "balance": balance,
        "face_embedding": front_embedding,
        "face_embeddings": [front_embedding, left_embedding, right_embedding],
        "role": data.get("role", "user"),
        "created_at": datetime.utcnow()
    }

    result = users.insert_one(user)

    return jsonify({
        "status": "success",
        "message": "User registered with face data",
        "user_id": str(result.inserted_id),
        "upi_id": upi_id
    }), 200


# -------------------------------
# Login API
# -------------------------------
@app.route("/login", methods=["POST"])
def login():

    data = request.get_json()

    if not data:
        return jsonify({"message": "No data received"}), 400

    email = data.get("email")
    password = data.get("password")

    if not email or not password:
        return jsonify({"message": "Email and password required"}), 400

    user = users.find_one({"email": email})

    if not user:
        return jsonify({"message": "User not found"}), 404

    if user["password"] != password:
        return jsonify({"message": "Invalid password"}), 401

    return jsonify({
        "message": "Login successful",
        "name": user["name"],
        "email": user["email"],
        "mobile": user["mobile"],
        "upi": user["upi_id"],
        "balance": user.get("balance", 0),
        "role": user.get("role", "user")
    }), 200


@app.route("/receiver/<receiver_input>", methods=["GET"])
def receiver_details(receiver_input):
    user = users.find_one({
        "$or": [
            {"upi_id": receiver_input},
            {"mobile": receiver_input}
        ]
    })

    if not user:
        return jsonify({
            "success": False,
            "message": "Receiver not found"
        }), 404

    return jsonify({
        "success": True,
        "name": user.get("name", ""),
        "mobile": user.get("mobile", ""),
        "upi": user.get("upi_id", "")
    }), 200


@app.route("/verify-face", methods=["POST"])
def verify_face():

    data = request.get_json()
    if not data:
        return jsonify({"status": "error", "message": "No data received"}), 400

    upi_id = data.get("upi_id")
    face_image = data.get("face_image")

    if not upi_id or not face_image:
        return jsonify({
            "status": "error",
            "verified": False,
            "message": "UPI ID and face image are required"
        }), 400

    user = users.find_one({"upi_id": upi_id})
    if not user:
        return jsonify({
            "status": "error",
            "verified": False,
            "message": "User not found"
        }), 404

    stored_embeddings = user.get("face_embeddings")
    if not stored_embeddings:
        stored_embeddings = [user.get("face_embedding")]

    live_embedding = extract_face_embedding(face_image)

    stored_embeddings = [embedding for embedding in stored_embeddings if embedding]

    if not stored_embeddings or live_embedding is None:
        return jsonify({
            "status": "error",
            "verified": False,
            "message": "Face data missing or no valid face detected"
        }), 400

    similarity_scores = []
    for stored_embedding in stored_embeddings:
        score = cosine_similarity(stored_embedding, live_embedding)
        if score is not None:
            similarity_scores.append(score)

    if not similarity_scores:
        return jsonify({
            "status": "error",
            "verified": False,
            "message": "Face embeddings are incompatible. Please re-register this user with the latest FaceNet backend"
        }), 400

    similarity = max(similarity_scores)
    is_verified = similarity >= FACE_MATCH_THRESHOLD

    if is_verified:
        return jsonify({
            "status": "success",
            "verified": True,
            "similarity": similarity,
            "message": "Face verified"
        }), 200

    return jsonify({
        "status": "failed",
        "verified": False,
        "similarity": similarity,
        "message": "Face does not match registered user"
    }), 401


# -------------------------------
# Risk Scoring Logic
# -------------------------------
def calculate_risk(sender_upi):
    past_anomalies = transactions.count_documents({"sender_upi": sender_upi, "is_anomaly": True})
    risk_score = min(past_anomalies * 25, 100)
    if risk_score >= 80:
        risk_level = "High"
    elif risk_score >= 50:
        risk_level = "Medium"
    else:
        risk_level = "Low"
    return risk_score, risk_level

# -------------------------------
# Transaction API
# -------------------------------
@app.route("/transaction", methods=["POST"])
def make_transaction():

    data = request.get_json()

    if not data:
        return jsonify({"message": "No data received"}), 400

    print("TRANSACTION DATA:", data)

    sender_upi = data.get("sender_upi")
    receiver_input = data.get("receiver_upi")
    amount = data.get("amount")
    entered_pin = data.get("upi_pin")

    if not all([sender_upi, receiver_input, amount, entered_pin]):
        return jsonify({"message": "All fields required"}), 400

    sender = users.find_one({"upi_id": sender_upi})

    if not sender:
        return jsonify({"message": "Sender not found"}), 404

    if str(sender["upi_pin"]) != str(entered_pin):
        return jsonify({
            "success": False,
            "message": "Invalid UPI PIN"
        }), 401

    if sender.get("balance", 0) < float(amount):
        return jsonify({
            "success": False,
            "message": "Insufficient balance",
            "balance": sender.get("balance", 0)
        }), 400

    receiver = users.find_one({"upi_id": receiver_input})

    if not receiver:
        receiver = users.find_one({"mobile": receiver_input})

    if not receiver:
        return jsonify({"message": "Receiver not found"}), 404

    receiver_upi = receiver["upi_id"]

    # update balances
    users.update_one(
        {"upi_id": sender_upi},
        {"$inc": {"balance": -float(amount)}}
    )

    users.update_one(
        {"upi_id": receiver_upi},
        {"$inc": {"balance": float(amount)}}
    )

    risk_score, risk_level = calculate_risk(sender_upi)

    transaction = {
        "sender_upi": sender_upi,
        "receiver_upi": receiver_upi,
        "amount": float(amount),
        "timestamp": datetime.now(),
        "status": "SUCCESS",
        "is_anomaly": False,
        "risk_score": risk_score,
        "risk_level": risk_level
    }

    transactions.insert_one(transaction)

    return jsonify({
        "success": True,
        "message": "Transaction successful",
        "sender_balance": users.find_one({"upi_id": sender_upi}).get("balance", 0),
        "receiver_balance": users.find_one({"upi_id": receiver_upi}).get("balance", 0)
    }), 200


# -------------------------------
# ✅ FIXED HISTORY API (IMPORTANT)
# -------------------------------
@app.route('/history', methods=['GET'])
def history():

    upi = request.args.get('upi')

    if not upi:
        return jsonify([]), 200

    user_transactions = list(transactions.find({
        "$or": [
            {"sender_upi": upi},
            {"receiver_upi": upi}
        ]
    }).sort("timestamp", -1))

    result = []

    for t in user_transactions:
        # 3-sigma rule should only consider SUCCESSFUL transactions
        if t.get("status", "SUCCESS") != "SUCCESS":
            continue
            
        result.append({
            "amount": float(t.get("amount", 0)),
            "type": "SENT" if t["sender_upi"] == upi else "RECEIVED"
        })

    return jsonify(result), 200


@app.route("/transactions/<upi_id>", methods=["GET"])
def transactions_by_upi(upi_id):

    user_transactions = list(transactions.find({
        "$or": [
            {"sender_upi": upi_id},
            {"receiver_upi": upi_id}
        ]
    }).sort("timestamp", -1))

    result = []
    for t in user_transactions:
        status = t.get("status", "SUCCESS")
        
        # Only the sender should see FAILED transactions. If the user is the receiver, skip it.
        if status == "FAILED" and t.get("receiver_upi") == upi_id:
            continue
            
        result.append({
            "sender_upi": t.get("sender_upi", ""),
            "receiver_upi": t.get("receiver_upi", ""),
            "amount": float(t.get("amount", 0)),
            "status": status,
            "timestamp": t.get("timestamp", datetime.now()).strftime("%d %b %Y %I:%M %p")
        })

    return jsonify(result), 200

# -------------------------------
# Admin Dashboard API
# -------------------------------
@app.route("/admin/dashboard-data", methods=["GET"])
def admin_dashboard_data():
    all_transactions = list(transactions.find({}).sort("timestamp", -1))
    
    result = []
    normal_count = 0
    anomaly_count = 0
    
    for t in all_transactions:
        is_anomaly = t.get("is_anomaly", False)
        if is_anomaly:
            anomaly_count += 1
        else:
            normal_count += 1
            
        result.append({
            "sender_upi": t.get("sender_upi", ""),
            "receiver_upi": t.get("receiver_upi", ""),
            "amount": float(t.get("amount", 0)),
            "status": t.get("status", "SUCCESS"),
            "timestamp": t.get("timestamp", datetime.now()).strftime("%d %b %Y %I:%M %p"),
            "is_anomaly": is_anomaly,
            "risk_score": t.get("risk_score", 0),
            "risk_level": t.get("risk_level", "Low")
        })
        
    # Generate simple alerts based on recent anomalies
    alerts = []
    if anomaly_count > 0:
        alerts.append(f"{anomaly_count} total anomalies detected in the system.")
    else:
        alerts.append("System is running smoothly. No recent anomalies.")
        
    fraud_rate = 0
    total_txns = len(all_transactions)
    if total_txns > 0:
        fraud_rate = round((anomaly_count / total_txns) * 100, 2)
        
    return jsonify({
        "transactions": result,
        "normal_count": normal_count,
        "anomaly_count": anomaly_count,
        "total_txns_today": total_txns,
        "fraud_rate": fraud_rate,
        "alerts": alerts
    }), 200

# -------------------------------
# Log Failed Transaction API
# -------------------------------
@app.route("/failed-transaction", methods=["POST"])
def failed_transaction():
    data = request.get_json()
    if not data:
        return jsonify({"message": "No data received"}), 400

    sender_upi = data.get("sender_upi")
    receiver_upi = data.get("receiver_upi")
    amount = data.get("amount")
    
    if not all([sender_upi, receiver_upi, amount]):
        return jsonify({"message": "Missing required fields"}), 400

    risk_score, risk_level = calculate_risk(sender_upi)
    # Give an immediate bump because they just failed
    risk_score = min(risk_score + 25, 100)
    if risk_score >= 80:
        risk_level = "High"
    elif risk_score >= 50:
        risk_level = "Medium"
    else:
        risk_level = "Low"

    transaction = {
        "sender_upi": sender_upi,
        "receiver_upi": receiver_upi,
        "amount": float(amount),
        "timestamp": datetime.now(),
        "status": "FAILED",
        "is_anomaly": True,
        "risk_score": risk_score,
        "risk_level": risk_level
    }
    transactions.insert_one(transaction)

    return jsonify({"success": True, "message": "Failed transaction logged"}), 200

# -------------------------------
# Admin User Profile API
# -------------------------------
@app.route("/admin/user-profile/<upi_id>", methods=["GET"])
def admin_user_profile(upi_id):
    user = users.find_one({"upi_id": upi_id})
    if not user:
        return jsonify({"message": "User not found"}), 404
        
    user_txns = list(transactions.find({"sender_upi": upi_id}).sort("timestamp", -1))
    
    total_txns = len(user_txns)
    anomalies = sum(1 for t in user_txns if t.get("is_anomaly", False))
    
    risk_score, risk_level = calculate_risk(upi_id)
    
    recent_txns = []
    for t in user_txns[:10]: # Return top 10 recent
        recent_txns.append({
            "receiver_upi": t.get("receiver_upi", ""),
            "amount": float(t.get("amount", 0)),
            "status": t.get("status", "SUCCESS"),
            "timestamp": t.get("timestamp", datetime.now()).strftime("%d %b %Y %I:%M %p"),
            "is_anomaly": t.get("is_anomaly", False),
            "risk_level": t.get("risk_level", "Low")
        })

    return jsonify({
        "name": user.get("name", ""),
        "mobile": user.get("mobile", ""),
        "email": user.get("email", ""),
        "upi_id": user.get("upi_id", ""),
        "total_txns": total_txns,
        "anomalies": anomalies,
        "risk_score": risk_score,
        "risk_level": risk_level,
        "recent_txns": recent_txns
    }), 200

# -------------------------------
# Balance API
# -------------------------------
@app.route("/balance/<upi>", methods=["GET"])
def get_balance(upi):

    user = users.find_one({"upi_id": upi})

    if not user:
        return jsonify({"balance": 0})

    return jsonify({"balance": user.get("balance", 0)})


# -------------------------------
# Run Server
# -------------------------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
