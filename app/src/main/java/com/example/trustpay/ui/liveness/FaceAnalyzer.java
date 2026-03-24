package com.example.trustpay.ui.liveness;

import android.annotation.SuppressLint;
import com.google.mlkit.vision.face.Face;
import android.util.Log;
import com.google.mlkit.vision.face.FaceDetector;

import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnLivenessDetectedListener {
        void onLivenessDetected();
    }
    private OnLivenessDetectedListener listener;
    private boolean isCompleted = false;

    public FaceAnalyzer(OnLivenessDetectedListener listener) {
        this.listener = listener;
    }

    private boolean isBlinkDetected = false;
    private boolean isHeadTurnDetected = false;

    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    FaceDetector detector = FaceDetection.getClient(options);

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage,
                            imageProxy.getImageInfo().getRotationDegrees());

            detector.process(image)
                    .addOnSuccessListener(faces -> {

                        for (Face face : faces) {

                            // 👁️ Blink Detection
                            float leftEye = face.getLeftEyeOpenProbability();
                            float rightEye = face.getRightEyeOpenProbability();

                            if (leftEye < 0.4 && rightEye < 0.4) {
                                isBlinkDetected = true;
                            }

                            // 🔄 Head Turn Detection
                            float rotY = face.getHeadEulerAngleY();

                            if (Math.abs(rotY) > 15) {
                                isHeadTurnDetected = true;
                            }

                            // ✅ Liveness Check
                            if ((isBlinkDetected || isHeadTurnDetected) && !isCompleted) {
                                isCompleted = true;

                                Log.d("LIVENESS", "Liveness Verified ✅");

                                if (listener != null) {
                                    listener.onLivenessDetected();
                                }
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }
}