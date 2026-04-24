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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
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

import com.example.trustpay.R;
import com.example.trustpay.network.ApiClient;
import com.example.trustpay.network.ApiService;
import com.example.trustpay.network.RegisterRequest;
import com.example.trustpay.network.RegisterResponse;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FaceRegistrationActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 201;
    private static final float MIN_EYE_OPEN_PROBABILITY = 0.45f;
    private static final float CENTER_TOLERANCE_RATIO = 0.18f;
    private static final int STEP_FRONT_FACE = 0;
    private static final int STEP_LEFT_FACE = 1;
    private static final int STEP_RIGHT_FACE = 2;
    private static final int STEP_DONE = 3;

    private PreviewView previewView;
    private TextView tvFaceStatus;
    private ImageView ivCapturedFace;
    private MaterialButton btnCaptureFace;
    private MaterialButton btnSubmitRegister;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private FaceDetector faceDetector;

    private boolean isValidFaceDetected = false;
    private int currentCaptureStep = STEP_FRONT_FACE;
    private String frontFaceBase64 = null;
    private String leftFaceBase64 = null;
    private String rightFaceBase64 = null;

    private String username;
    private String email;
    private String mobile;
    private String password;
    private String upiPin;
    private double balance;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        previewView = findViewById(R.id.previewViewFaceRegister);
        tvFaceStatus = findViewById(R.id.tvFaceRegisterStatus);
        ivCapturedFace = findViewById(R.id.ivCapturedFaceRegister);
        btnCaptureFace = findViewById(R.id.btnCaptureFaceRegister);
        btnSubmitRegister = findViewById(R.id.btnSubmitFaceRegister);

        username = getIntent().getStringExtra("username");
        email = getIntent().getStringExtra("email");
        mobile = getIntent().getStringExtra("mobile");
        password = getIntent().getStringExtra("password");
        upiPin = getIntent().getStringExtra("upi_pin");
        balance = Double.parseDouble(getIntent().getStringExtra("balance"));
        role = getIntent().getStringExtra("role");

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
        btnSubmitRegister.setOnClickListener(v -> submitRegistration());

        btnSubmitRegister.setEnabled(false);
        showPosePopup("Step 1", "Look straight at the camera and capture your face.");
        refreshCaptureUi();

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
            float headY = face.getHeadEulerAngleY();

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

            boolean poseOk;
            if (currentCaptureStep == STEP_FRONT_FACE) {
                poseOk = Math.abs(headY) <= 10;
            } else {
                poseOk = Math.abs(headY) >= 12;
            }

            validFace = eyesOpen && centered && poseOk;
        }

        boolean finalValidFace = validFace;
        runOnUiThread(() -> {
            isValidFaceDetected = finalValidFace;
            if (finalValidFace) {
                tvFaceStatus.setText(getCurrentPoseReadyMessage());
                tvFaceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                tvFaceStatus.setText(getCurrentPoseInstruction());
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
            Toast.makeText(this, getCurrentPoseInstruction(), Toast.LENGTH_SHORT).show();
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
                                    FaceRegistrationActivity.this,
                                    "Face capture failed",
                                    Toast.LENGTH_SHORT
                            ).show());
                            return;
                        }

                        Bitmap rotatedBitmap = rotateAndMirrorBitmap(bitmap, rotationDegrees);
                        String faceBase64 = encodeBitmapToBase64(rotatedBitmap);

                        runOnUiThread(() -> {
                            ivCapturedFace.setImageBitmap(rotatedBitmap);
                            saveCurrentPoseCapture(faceBase64);
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() -> Toast.makeText(
                                FaceRegistrationActivity.this,
                                "Face capture failed",
                                Toast.LENGTH_SHORT
                        ).show());
                    }
                }
        );
    }

    private void submitRegistration() {
        if (frontFaceBase64 == null || leftFaceBase64 == null || rightFaceBase64 == null) {
            Toast.makeText(this, "Please capture front, left and right face poses", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitRegister.setEnabled(false);

        RegisterRequest request = new RegisterRequest(
                username,
                email,
                mobile,
                password,
                upiPin,
                balance,
                frontFaceBase64,
                leftFaceBase64,
                rightFaceBase64,
                role
        );

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.registerUser(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call,
                                   @NonNull Response<RegisterResponse> response) {
                btnSubmitRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equalsIgnoreCase(response.body().getStatus())) {
                    showRegistrationSuccessDialog(response.body());
                } else {
                    String errorMessage = "Face not registered. Please recapture your face.";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = getBackendErrorMessage(response.errorBody().string());
                        }
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(
                            FaceRegistrationActivity.this,
                            "Face not registered: " + errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                btnSubmitRegister.setEnabled(true);
                Toast.makeText(
                        FaceRegistrationActivity.this,
                        "Face not registered. Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void showRegistrationSuccessDialog(RegisterResponse response) {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        String message = "All poses get registered successfully";
        if (response.getUpiId() != null && !response.getUpiId().isEmpty()) {
            message = message + "\nUPI: " + response.getUpiId();
        }

        new AlertDialog.Builder(this)
                .setTitle("Registration Successful")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Back to Login Page", (dialog, which) -> {
                    getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(FaceRegistrationActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private String getBackendErrorMessage(String errorBody) {
        if (errorBody == null || errorBody.trim().isEmpty()) {
            return "Please recapture your face.";
        }

        try {
            JSONObject jsonObject = new JSONObject(errorBody);
            String message = jsonObject.optString("message");
            if (!message.isEmpty()) {
                return message;
            }
        } catch (Exception ignored) {
        }

        return errorBody;
    }

    private void saveCurrentPoseCapture(String faceBase64) {
        if (currentCaptureStep == STEP_FRONT_FACE) {
            frontFaceBase64 = faceBase64;
            currentCaptureStep = STEP_LEFT_FACE;
            showPosePopup("Step 2", "Turn your face LEFT and tap Capture Face.");
            Toast.makeText(this, "Front face captured", Toast.LENGTH_SHORT).show();
        } else if (currentCaptureStep == STEP_LEFT_FACE) {
            leftFaceBase64 = faceBase64;
            currentCaptureStep = STEP_RIGHT_FACE;
            showPosePopup("Step 3", "Turn your face RIGHT and tap Capture Face.");
            Toast.makeText(this, "Left face captured", Toast.LENGTH_SHORT).show();
        } else {
            rightFaceBase64 = faceBase64;
            currentCaptureStep = STEP_DONE;
            btnCaptureFace.setEnabled(false);
            btnSubmitRegister.setEnabled(true);
            showPosePopup("Done", "All face poses captured. Tap Verify & Register.");
            Toast.makeText(this, "Right face captured", Toast.LENGTH_SHORT).show();
        }

        isValidFaceDetected = false;
        refreshCaptureUi();
    }

    private void refreshCaptureUi() {
        if (currentCaptureStep == STEP_FRONT_FACE) {
            btnCaptureFace.setText("Capture Front Face");
        } else if (currentCaptureStep == STEP_LEFT_FACE) {
            btnCaptureFace.setText("Capture Left Face");
        } else if (currentCaptureStep == STEP_RIGHT_FACE) {
            btnCaptureFace.setText("Capture Right Face");
        } else {
            btnCaptureFace.setText("All Face Poses Captured");
        }
        tvFaceStatus.setText(getCurrentPoseInstruction());
        if (currentCaptureStep == STEP_DONE) {
            tvFaceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvFaceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }

    private String getCurrentPoseInstruction() {
        if (currentCaptureStep == STEP_FRONT_FACE) {
            return "Look straight at camera";
        }
        if (currentCaptureStep == STEP_LEFT_FACE) {
            return "Turn left and align your face";
        }
        if (currentCaptureStep == STEP_RIGHT_FACE) {
            return "Turn right and align your face";
        }
        return "All poses captured. Tap Verify & Register";
    }

    private String getCurrentPoseReadyMessage() {
        if (currentCaptureStep == STEP_FRONT_FACE) {
            return "Straight face aligned. Tap Capture Front Face";
        }
        if (currentCaptureStep == STEP_LEFT_FACE) {
            return "Left pose ready. Tap Capture Left Face";
        }
        if (currentCaptureStep == STEP_RIGHT_FACE) {
            return "Right pose ready. Tap Capture Right Face";
        }
        return "All poses captured. Tap Verify & Register";
    }

    private void showPosePopup(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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
