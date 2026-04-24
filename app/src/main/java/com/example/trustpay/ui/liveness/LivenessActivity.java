package com.example.trustpay.ui.liveness;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.BackendConfig;
import com.example.trustpay.ui.result.DeclineActivity;
import com.example.trustpay.ui.verification.PinActivity;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LivenessActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvLivenessInstruction;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    String senderUpi;
    String receiverUpi;
    String amount;
    boolean isFaceVerificationRunning = false;

    String VERIFY_FACE_URL = BackendConfig.endpoint("verify-face");
    String FAILED_TRANSACTION_URL = BackendConfig.endpoint("failed-transaction");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveness);

        // ✅ Initialize FIRST
        previewView = findViewById(R.id.previewView);
        tvLivenessInstruction = findViewById(R.id.tvLivenessInstruction);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // ✅ Get data
        senderUpi = getIntent().getStringExtra("sender_upi");
        receiverUpi = getIntent().getStringExtra("receiver_upi");
        amount = getIntent().getStringExtra("amount");

        // ✅ Handle permission ONCE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private void onLivenessSuccess() {
        if (isFaceVerificationRunning) {
            return;
        }

        isFaceVerificationRunning = true;
        tvLivenessInstruction.setText("Checking your face identity...");

        if (imageCapture == null) {
            isFaceVerificationRunning = false;
            Toast.makeText(this, "Camera capture not ready", Toast.LENGTH_SHORT).show();
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
                            runOnUiThread(() -> {
                                isFaceVerificationRunning = false;
                                tvLivenessInstruction.setText("Face capture failed. Try again.");
                            });
                            return;
                        }

                        Bitmap rotatedBitmap = rotateAndMirrorBitmap(bitmap, rotationDegrees);
                        String faceBase64 = encodeBitmapToBase64(rotatedBitmap);
                        runOnUiThread(() -> verifyFaceWithBackend(faceBase64));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() -> {
                            isFaceVerificationRunning = false;
                            tvLivenessInstruction.setText("Face capture failed. Try again.");
                        });
                    }
                }
        );
    }

    private void verifyFaceWithBackend(String faceBase64) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("upi_id", senderUpi);
            jsonBody.put("face_image", faceBase64);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    VERIFY_FACE_URL,
                    jsonBody,
                    response -> {
                        isFaceVerificationRunning = false;
                        boolean verified = response.optBoolean("verified", false);

                        if (verified) {
                            Toast.makeText(this, "Face matched. Enter UPI PIN.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, PinActivity.class);
                            intent.putExtra("sender_upi", senderUpi);
                            intent.putExtra("receiver_upi", receiverUpi);
                            intent.putExtra("amount", amount);
                            startActivity(intent);
                            finish();
                        } else {
                            logFailedTransactionAndDecline("Face does not match registered user");
                        }
                    },
                    error -> {
                        isFaceVerificationRunning = false;
                        String message = "Face verification failed";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            message = new String(error.networkResponse.data);
                        }
                        logFailedTransactionAndDecline(message);
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(request);
        } catch (Exception e) {
            isFaceVerificationRunning = false;
            logFailedTransactionAndDecline("Face verification error");
        }
    }

    private void logFailedTransactionAndDecline(String reason) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("sender_upi", senderUpi);
            jsonBody.put("receiver_upi", receiverUpi);
            jsonBody.put("amount", amount);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    FAILED_TRANSACTION_URL,
                    jsonBody,
                    response -> goToDecline(reason),
                    error -> goToDecline(reason)
            );
            queue.add(request);
        } catch (Exception e) {
            goToDecline(reason);
        }
    }

    private void goToDecline(String reason) {
        Intent intent = new Intent(this, DeclineActivity.class);
        intent.putExtra("reason", reason);
        startActivity(intent);
        finish();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();  // ✅ start AFTER permission granted
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor,
                        new FaceAnalyzer(
                                message -> runOnUiThread(() -> tvLivenessInstruction.setText(message)),
                                () -> runOnUiThread(this::onLivenessSuccess)
                        )
                );

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, imageAnalysis
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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
    protected void onDestroy() {
        super.onDestroy();

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
