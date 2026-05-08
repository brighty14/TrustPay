package com.example.trustpay.ui.transaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
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
import com.example.trustpay.network.BackendConfig;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 301;

    PreviewView previewView;
    TextView tvScannerStatus;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean scanHandled = false;
    private String senderUpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        previewView = findViewById(R.id.previewViewQrScanner);
        tvScannerStatus = findViewById(R.id.tvScannerStatus);
        senderUpi = getIntent().getStringExtra("sender_upi");

        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        cameraExecutor = Executors.newSingleThreadExecutor();

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

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::scanQrFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
            } catch (Exception e) {
                Toast.makeText(this, "Unable to start scanner", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void scanQrFrame(@NonNull ImageProxy imageProxy) {
        if (scanHandled || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        BarcodeScanning.getClient()
                .process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (scanHandled) {
                        return;
                    }

                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.trim().isEmpty()) {
                            scanHandled = true;
                            runOnUiThread(() -> {
                                tvScannerStatus.setText("QR found. Opening payment...");
                                lookupScannedReceiver(rawValue.trim());
                            });
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> runOnUiThread(() ->
                        tvScannerStatus.setText("Unable to read QR. Try again.")))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void lookupScannedReceiver(String qrValue) {
        String receiverInput = getReceiverInputFromQr(qrValue);
        if (receiverInput.isEmpty()) {
            scanHandled = false;
            tvScannerStatus.setText("Invalid TrustPay QR");
            return;
        }

        try {
            String encodedInput = URLEncoder.encode(receiverInput, StandardCharsets.UTF_8.name());
            String url = BackendConfig.endpoint("receiver/" + encodedInput);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> openPaymentPage(response),
                    error -> {
                        scanHandled = false;
                        tvScannerStatus.setText("Receiver not found");
                    }
            );

            queue.add(request);
        } catch (Exception e) {
            scanHandled = false;
            tvScannerStatus.setText("Scanner error. Try again.");
        }
    }

    private String getReceiverInputFromQr(String qrValue) {
        try {
            JSONObject payload = new JSONObject(qrValue);
            String upi = payload.optString("upi");
            if (!upi.isEmpty()) {
                return upi;
            }
            return payload.optString("mobile");
        } catch (Exception ignored) {
        }

        return qrValue;
    }

    private void openPaymentPage(JSONObject response) {
        if (!response.optBoolean("success", false)) {
            scanHandled = false;
            tvScannerStatus.setText("Receiver not found");
            return;
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        Intent intent = new Intent(QrScannerActivity.this, PaymentActivity.class);
        intent.putExtra("sender_upi", senderUpi);
        intent.putExtra("receiver_name", response.optString("name"));
        intent.putExtra("receiver_mobile", response.optString("mobile"));
        intent.putExtra("receiver_upi", response.optString("upi"));
        startActivity(intent);
        finish();
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
                Toast.makeText(this, "Camera permission is required to scan QR", Toast.LENGTH_LONG).show();
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
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
