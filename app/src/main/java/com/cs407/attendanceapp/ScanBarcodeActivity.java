package com.cs407.attendanceapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScanBarcodeActivity extends AppCompatActivity implements BarcodeListener {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        previewView = findViewById(R.id.previewView);

        // Check and request camera permissions
        requestCameraPermission();
    }

    private void requestCameraPermission() {
        Log.i("INFO", "Requesting camera permission");
        // Check if the CAMERA permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("INFO", "Permission granted. Starting camera");
            // Camera permission is already granted, proceed with camera-related operations
            startCamera();
        } else {
            // Request CAMERA permission. The result will be received in the onRequestPermissionsResult callback.
            Log.i("INFO", "Requesting permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            handleCameraPermissionResult(grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void handleCameraPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Camera permission granted, proceed with camera-related operations
            Log.i("INFO", "Camera permission granted, starting camera");

        } else {
            // Camera permission denied. You may want to show a message or take alternative actions.
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();

                    // Preview
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    Log.i("INFO", "Preview built");

                    // ImageCapture
                    this.imageCapture = new ImageCapture.Builder().build();
                    Log.i("INFO", "Image Capture built");

                    // ImageAnalysis
                    BarcodeListener barcodeListener = this;

                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ThisAnalyzer(barcodeListener));

                    Log.i("INFO", "imageAnalyzer set");

                    // Select back camera as a default
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

                } catch (ExecutionException | InterruptedException e) {
                    // Handle exceptions, e.g., show an error message to the user
                    Log.e("ERROR", "Error initializing camera provider: " + e.getMessage(), e);
                    // Optionally, show an error message to the user
                    runOnUiThread(() -> Toast.makeText(this, "Error initializing camera", Toast.LENGTH_SHORT).show());
                }
            }, ContextCompat.getMainExecutor(this));
        }

    @Override
    public void onBarcodeScanned(String rawValue) {
        Log.i("INFO", "onBarcodeScanned");
        navigateToStudentHomeScreen(rawValue);
    }

    private void navigateToStudentHomeScreen(String rawValue) {
        // Stop the camera preview and navigate to another screen

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        Intent intent = new Intent(this, StudentHomePage.class);
        intent.putExtra("barcodeValue", rawValue);
        startActivity(intent);

        // Finish the current activity
        finish();
    }

    static class ThisAnalyzer implements ImageAnalysis.Analyzer {

        private final BarcodeScanner scanner;
        private final BarcodeListener barcodeListener;

        public ThisAnalyzer(BarcodeListener barcodeListener) {
            this.barcodeListener = barcodeListener;
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();
            this.scanner = BarcodeScanning.getClient(options);
            Log.i("INFO", "Scanner built");
        }

        @OptIn(markerClass = ExperimentalGetImage.class) @Override
        public void analyze(ImageProxy imageProxy) {
            Log.i("INFO", "Analyzing");
            Image mediaImage = imageProxy.getImage();
            Log.i("INFO","mediaImage: " + (mediaImage != null));
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                Task<List<Barcode>> result = scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            // Task completed successfully
                            for (Barcode barcode : barcodes) {

                                String rawValue = barcode.getRawValue();

                                int format = barcode.getFormat();
                                if (format == Barcode.FORMAT_QR_CODE) {
                                    // Handle QR code
                                    Log.i("INFO", "Value: " + rawValue);
                                    barcodeListener.onBarcodeScanned(rawValue);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Task failed with an exception
                            Log.e("ERROR", "Barcode scanning failed: " + e.getMessage(), e);
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            }
        }
    }

}

