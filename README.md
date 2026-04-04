# 🚀 TrustPay – Secure Payment System with Face Verification & Fraud Detection

TrustPay is a **full-stack digital payment application** that enhances transaction security using **face recognition** and **statistical fraud detection (3-Sigma Rule)**.
It integrates **Android, Flask, MongoDB, and Machine Learning** to deliver a secure and intelligent payment experience.

---

## 📌 Features

* 🔐 **User Registration with Face Enrollment**
* 💰 **Wallet-based Transactions (UPI / Phone Number)**
* 👤 **Face Verification before Transactions**
* ⚠️ **Fraud Detection using 3-Sigma Rule**
* 📊 **Transaction History Tracking**
* 🌐 **REST API-based Backend Communication**

---

## 🏗️ Architecture Overview

```
Android App (Java + XML)
        │
        ▼
Flask Backend (REST APIs)
        │
        ▼
MongoDB Database
        │
        ▼
Face Recognition (FaceNet + OpenCV)
        │
        ▼
Fraud Detection (3-Sigma Algorithm)
```

---

## 🧰 Tech Stack

### 📱 Frontend (Android)

* Java (Android SDK)
* XML Layouts
* Material Components
* ConstraintLayout
* AppCompat

### 🌐 Backend

* Python (Flask)
* Flask-CORS

### 🗄️ Database

* MongoDB
* PyMongo

### 🤖 Face Recognition

* CameraX
* ML Kit Face Detection
* OpenCV
* keras-facenet (FaceNet Model)

### 🔗 Networking

* Volley
* Retrofit
* Gson Converter

### ⚙️ Build Tools

* Gradle (Kotlin DSL)
* Android Gradle Plugin 8+

### 🧪 Testing

* JUnit
* AndroidX JUnit
* Espresso

---

## 📊 Code Statistics

| Language | Files | Lines |
| -------- | ----: | ----: |
| Java     |    23 | 2,192 |
| XML      |    33 | 1,177 |
| Python   |     1 |   359 |

---

## 🔐 Security Mechanisms

### 1️⃣ Face Verification

* User face is captured during registration
* Face embeddings generated using **FaceNet**
* During transaction, live face is matched with stored embedding

### 2️⃣ Fraud Detection (3-Sigma Rule)

* Tracks previous transaction amounts
* Calculates:

  * Mean (μ)
  * Standard Deviation (σ)
* Flags transaction if:

```
Amount > μ + 3σ
```

---

## 🔄 API Endpoints (Sample)

| Method | Endpoint       | Description                  |
| ------ | -------------- | ---------------------------- |
| POST   | `/register`    | Register user with face data |
| POST   | `/login`       | User authentication          |
| POST   | `/transaction` | Perform transaction          |
| GET    | `/history`     | Get transaction history      |

---

## 📁 Project Structure

```
TrustPay/
│
├── app/                    # Android Application
│   ├── java/              # Java source files
│   ├── res/               # XML layouts & resources
│
├── backend/
│   └── app.py             # Flask backend server
│
├── build.gradle.kts       # Project build config
├── app/build.gradle.kts   # App-level config
```

---

## ⚙️ Setup Instructions

### 🔹 Backend Setup

```bash
cd backend
pip install flask flask-cors pymongo opencv-python numpy keras-facenet
python app.py
```

---

### 🔹 Android Setup

1. Open project in **Android Studio**
2. Sync Gradle
3. Connect device/emulator
4. Run the application

---

## 🧠 Key Concepts Used

* Face Embedding & Similarity Matching
* REST API Integration
* NoSQL Database Design
* Statistical Fraud Detection (3-Sigma Rule)
* Secure Transaction Flow Design

---

## 🚀 Future Enhancements

* 🔐 OTP / Multi-Factor Authentication
* ☁️ Cloud Deployment (AWS/GCP)
* 📈 AI-based Advanced Fraud Detection
* 🔔 Real-time Notifications
* 📊 Admin Dashboard

---

## 👨‍💻 Author

**Arunprakash S**
Master of Computer Applications (MCA)

---

## 📜 License

This project is for academic and learning purposes.

---
