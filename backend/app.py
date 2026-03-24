from flask import Flask, request, jsonify
from flask_cors import CORS
from pymongo import MongoClient
import random
from datetime import datetime

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


# -------------------------------
# Register API
# -------------------------------
@app.route("/register", methods=["POST"])
def register():

    data = request.get_json()

    if not data:
        return jsonify({"message": "No data received"}), 400

    print("REGISTER DATA:", data)

    name = data.get("name")
    email = data.get("email")
    mobile = data.get("mobile")
    password = data.get("password")
    upi_pin = data.get("upi_pin")
    balance = data.get("balance")

    if not all([name, email, mobile, password, upi_pin, balance]):
        return jsonify({"message": "All fields are required"}), 400

    if len(str(upi_pin)) != 4:
        return jsonify({"message": "UPI PIN must be 4 digits"}), 400

    try:
        balance = float(balance)
    except:
        return jsonify({"message": "Invalid balance"}), 400

    if users.find_one({"email": email}):
        return jsonify({"message": "Email already registered"}), 400

    if users.find_one({"mobile": mobile}):
        return jsonify({"message": "Mobile already registered"}), 400

    upi_id = generate_upi(name)

    user = {
        "name": name,
        "email": email,
        "mobile": mobile,
        "password": password,
        "upi_id": upi_id,
        "upi_pin": str(upi_pin),
        "balance": balance
    }

    users.insert_one(user)

    return jsonify({
        "message": "User registered successfully",
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
        "balance": user.get("balance", 0)
    }), 200


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
            "message": "Insufficient balance"
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

    transaction = {
        "sender_upi": sender_upi,
        "receiver_upi": receiver_upi,
        "amount": float(amount),
        "timestamp": datetime.now(),
        "status": "SUCCESS"
    }

    transactions.insert_one(transaction)

    return jsonify({
        "success": True,
        "message": "Transaction successful"
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
    }))

    result = []

    for t in user_transactions:
        result.append({
            "amount": float(t.get("amount", 0)),
            "type": "SENT" if t["sender_upi"] == upi else "RECEIVED"
        })

    return jsonify(result), 200


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