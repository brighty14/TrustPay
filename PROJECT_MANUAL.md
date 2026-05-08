# TrustPay Project Manual

TrustPay is an Android payment demo project with a Python Flask backend. The app supports user registration, login, face verification, UPI-style transactions, transaction history, fraud/anomaly logging, and an admin dashboard.

## Project Structure

- `app/` - Android application source code.
- `backend/` - Flask API server used by the Android app.
- `gradle/`, `gradlew`, `gradlew.bat` - Gradle wrapper files for building the Android project.
- `PROJECT_MANUAL.md` - Manual for setting up, running, and reviewing the project from GitHub.

## Requirements

Install these before running the project:

- Android Studio
- JDK 11 or newer
- Python 3.10 or newer
- MongoDB Community Server
- Git
- Android phone or emulator

## How to Download from GitHub

1. Open the GitHub repository:
   `https://github.com/arunprakash-3001/TrustPay`
2. Click the green `Code` button.
3. Copy the HTTPS URL.
4. Run this command in a terminal:

```bash
git clone https://github.com/arunprakash-3001/TrustPay.git
cd TrustPay
```

You can also download it directly from GitHub by clicking `Code` and then `Download ZIP`.

## Backend Setup

1. Start MongoDB on your system.
2. Open a terminal inside the project folder.
3. Create and activate a Python virtual environment:

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate
```

4. Install backend packages:

```bash
pip install -r requirements.txt
```

5. Run the Flask backend:

```bash
python app.py
```

6. Confirm the backend is running by opening:

```text
http://localhost:5000
```

It should show:

```text
TrustPay Backend Running
```

## Android App Setup

1. Open Android Studio.
2. Select `Open` and choose the cloned `TrustPay` folder.
3. Wait for Gradle sync to complete.
4. Open this file:

```text
app/src/main/java/com/example/trustpay/network/BackendConfig.java
```

5. Update the backend host IP if needed:

```java
private static final String HOST = "192.168.1.5";
private static final String PORT = "5000";
```

Use your computer LAN IP address when testing on a physical Android phone. Use `10.0.2.2` when testing from the default Android emulator.

6. Connect an Android phone or start an emulator.
7. Click `Run` in Android Studio.

## Build from Command Line

From the root project folder, run:

```bash
gradlew.bat assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Main App Flow

1. Register a new user with name, email, mobile number, password, UPI PIN, balance, and face capture.
2. Login using the registered email and password.
3. Send payments using receiver UPI ID or mobile number.
4. Verify payments using face verification and UPI PIN.
5. View transaction history and balance.
6. Use the admin dashboard to inspect transactions, risk score, anomaly count, and user profiles.

## Backend API Summary

- `GET /` - Backend health check.
- `POST /register` - Register user and face data.
- `POST /login` - Login user.
- `GET /receiver/<receiver_input>` - Fetch receiver by UPI ID or mobile.
- `POST /verify-face` - Verify face before payment.
- `POST /transaction` - Complete transaction.
- `GET /history?upi=<upi_id>` - Get chart/history data.
- `GET /transactions/<upi_id>` - Get transaction list for a user.
- `GET /balance/<upi>` - Get user balance.
- `GET /admin/dashboard-data` - Get admin dashboard analytics.
- `POST /failed-transaction` - Log failed suspicious transaction.
- `GET /admin/user-profile/<upi_id>` - Get admin user profile details.

## Notes for GitHub Portal

After pushing this project to GitHub, anyone can run it by reading this manual file in the repository root. On GitHub, open `PROJECT_MANUAL.md` to see the setup and execution steps directly in the browser.

Do not upload local machine files such as `local.properties`, `.gradle/`, `.idea/`, `build/`, or Python `__pycache__/` folders. These are ignored by `.gitignore`.
