package com.example.trustpay.ui.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.ApiClient;
import com.example.trustpay.network.ApiService;
import com.example.trustpay.network.BackendConfig;
import com.example.trustpay.network.RegisterRequest;
import com.example.trustpay.network.RegisterResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final float MIN_EYE_OPEN_PROBABILITY = 0.6f;
    private static final float CENTER_TOLERANCE_RATIO = 0.18f;

    TextInputEditText etName, etEmail, etMobile, etPassword, etUpiPin, etBalance;
    Spinner spinnerRole;
    PreviewView previewView;
    ImageView ivFacePreview;
    TextView tvFaceStatus;
    MaterialButton btnCaptureFace;
    MaterialButton btnRegister;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private FaceDetector faceDetector;
    private boolean isValidFaceDetected = false;
    private String capturedFaceBase64 = null;

    String BASE_URL = BackendConfig.endpoint("register");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etUpiPin = findViewById(R.id.etUpiPin);
        etBalance = findViewById(R.id.etBalance); // ✅ NEW
        spinnerRole = findViewById(R.id.spinnerRole);

        btnRegister = findViewById(R.id.btnRegister);

        previewView = findViewById(R.id.previewView);
        ivFacePreview = findViewById(R.id.ivFacePreview);
        tvFaceStatus = findViewById(R.id.tvFaceStatus);
        btnCaptureFace = findViewById(R.id.btnCaptureFace);

        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        cameraExecutor = Executors.newSingleThreadExecutor();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);

        btnCaptureFace.setOnClickListener(v -> captureFaceImage());
        btnRegister.setOnClickListener(v -> openFaceRegistrationPage());

        if (System.currentTimeMillis() < 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_CODE
                );
            }
        }
    }

    private void openFaceRegistrationPage() {
        String username = getInputValue(etName);
        String email = getInputValue(etEmail);
        String mobile = getInputValue(etMobile);
        String password = getInputValue(etPassword);
        String upiPin = getInputValue(etUpiPin);
        String balanceText = getInputValue(etBalance);

        if (username.isEmpty() || email.isEmpty() || mobile.isEmpty()
                || password.isEmpty() || upiPin.isEmpty() || balanceText.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (upiPin.length() != 4) {
            Toast.makeText(this, "UPI PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Double.parseDouble(balanceText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid balance", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RegisterActivity.this, FaceRegistrationActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("email", email);
        intent.putExtra("mobile", mobile);
        intent.putExtra("password", password);
        intent.putExtra("upi_pin", upiPin);
        intent.putExtra("balance", balanceText);
        intent.putExtra("role", spinnerRole.getSelectedItem().toString().toLowerCase());
        startActivity(intent);
    }

    private void registerUser() {
        registerUserWithFace();
        if (capturedFaceBase64 == null || capturedFaceBase64 != null) {
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String upiPin = etUpiPin.getText().toString().trim();
        String balance = etBalance.getText().toString().trim(); // ✅ NEW

        // 🔴 Check empty fields
        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() ||
                password.isEmpty() || upiPin.isEmpty() || balance.isEmpty()) {

            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 PIN validation
        if (upiPin.length() != 4) {
            Toast.makeText(this, "UPI PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 Balance validation
        try {
            Double.parseDouble(balance);
        } catch (Exception e) {
            Toast.makeText(this, "Enter valid balance", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            JSONObject jsonBody = new JSONObject();

            jsonBody.put("name", name);
            jsonBody.put("email", email);
            jsonBody.put("mobile", mobile);
            jsonBody.put("password", password);
            jsonBody.put("upi_pin", upiPin);
            jsonBody.put("balance", balance); // ✅ NEW FIELD

            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL,
                    jsonBody,
                    response -> {

                        Toast.makeText(this,
                                "Registered Successfully\nUPI: " + response.optString("upi_id"),
                                Toast.LENGTH_LONG).show();

                    },
                    error -> {

                        // 🔥 Show backend error if available
                        String message = "Registration Failed";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorData = new String(error.networkResponse.data);
                                message = errorData;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }) {

                // ✅ IMPORTANT HEADER
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFaceFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                );
            } catch (Exception e) {
                Toast.makeText(this, "Unable to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFaceFrame(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        int imageWidth = inputImage.getWidth();
        int imageHeight = inputImage.getHeight();

        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> updateFaceValidation(faces, imageWidth, imageHeight))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    isValidFaceDetected = false;
                    tvFaceStatus.setText("Align your face properly");
                    tvFaceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                }))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void updateFaceValidation(List<Face> faces, int imageWidth, int imageHeight) {
        boolean validFace = false;

        if (faces != null && faces.size() == 1) {
            Face face = faces.get(0);
            Float leftEyeOpen = face.getLeftEyeOpenProbability();
            Float rightEyeOpen = face.getRightEyeOpenProbability();
            Rect box = face.getBoundingBox();

            boolean eyesOpen = leftEyeOpen != null
                    && rightEyeOpen != null
                    && leftEyeOpen >= MIN_EYE_OPEN_PROBABILITY
                    && rightEyeOpen >= MIN_EYE_OPEN_PROBABILITY;

            float boxCenterX = box.centerX();
            float boxCenterY = box.centerY();
            float frameCenterX = imageWidth / 2f;
            float frameCenterY = imageHeight / 2f;
            boolean centered = Math.abs(boxCenterX - frameCenterX) <= imageWidth * CENTER_TOLERANCE_RATIO
                    && Math.abs(boxCenterY - frameCenterY) <= imageHeight * CENTER_TOLERANCE_RATIO;

            validFace = eyesOpen && centered;
        }

        boolean finalValidFace = validFace;
        runOnUiThread(() -> {
            isValidFaceDetected = finalValidFace;
            if (finalValidFace) {
                tvFaceStatus.setText("Face aligned. Tap Capture Face");
                tvFaceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                tvFaceStatus.setText("Align your face properly");
                tvFaceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
        });
    }

    private void captureFaceImage() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidFaceDetected) {
            Toast.makeText(this, "Align your face properly", Toast.LENGTH_SHORT).show();
            return;
        }

        imageCapture.takePicture(
                cameraExecutor,
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bitmap = imageProxyToBitmap(image);
                        int rotationDegrees = image.getImageInfo().getRotationDegrees();
                        image.close();

                        if (bitmap == null) {
                            runOnUiThread(() -> Toast.makeText(
                                    RegisterActivity.this,
                                    "Face capture failed",
                                    Toast.LENGTH_SHORT
                            ).show());
                            return;
                        }

                        Bitmap rotatedBitmap = rotateAndMirrorBitmap(bitmap, rotationDegrees);
                        capturedFaceBase64 = encodeBitmapToBase64(rotatedBitmap);

                        runOnUiThread(() -> {
                            ivFacePreview.setImageBitmap(rotatedBitmap);
                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Face captured successfully",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() -> Toast.makeText(
                                RegisterActivity.this,
                                "Face capture failed",
                                Toast.LENGTH_SHORT
                        ).show());
                    }
                }
        );
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();

        if (imageProxy.getFormat() == ImageFormat.JPEG && planes.length >= 1) {
            ByteBuffer jpegBuffer = planes[0].getBuffer();
            byte[] jpegBytes = new byte[jpegBuffer.remaining()];
            jpegBuffer.get(jpegBytes);
            return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        }

        if (planes.length < 3) {
            return null;
        }

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                null
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                90,
                outputStream
        );

        byte[] imageBytes = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private Bitmap rotateAndMirrorBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        matrix.postScale(-1f, 1f, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private void registerUserWithFace() {
        String username = getInputValue(etName);
        String email = getInputValue(etEmail);
        String mobile = getInputValue(etMobile);
        String password = getInputValue(etPassword);
        String upiPin = getInputValue(etUpiPin);
        String balanceText = getInputValue(etBalance);

        if (username.isEmpty() || email.isEmpty() || mobile.isEmpty()
                || password.isEmpty() || upiPin.isEmpty() || balanceText.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (upiPin.length() != 4) {
            Toast.makeText(this, "UPI PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capturedFaceBase64 == null) {
            Toast.makeText(this, "Please capture your face before registration", Toast.LENGTH_SHORT).show();
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid balance", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        RegisterRequest request = new RegisterRequest(
                username,
                email,
                mobile,
                password,
                upiPin,
                balance,
                capturedFaceBase64,
                spinnerRole.getSelectedItem().toString().toLowerCase()
        );

        apiService.registerUser(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call,
                                   @NonNull retrofit2.Response<RegisterResponse> response) {
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse body = response.body();
                    String message = body.getMessage();

                    if (body.getUpiId() != null && !body.getUpiId().isEmpty()) {
                        message = message + "\nUPI: " + body.getUpiId();
                    }

                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(
                            RegisterActivity.this,
                            "Registration failed. Check backend and face data.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                btnRegister.setEnabled(true);
                Toast.makeText(
                        RegisterActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private String getInputValue(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for face registration", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
