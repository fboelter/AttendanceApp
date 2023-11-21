package com.cs407.attendanceapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cs407.attendanceapp2.R;

import java.util.Map;

public class ScanBarcodeActivity extends AppCompatActivity {

    /*
    private final ActivityResultLauncher<String[]> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::handlePermissionsResult);
     */


    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Button launchCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        launchCameraButton = findViewById(R.id.launchCameraButton);

        launchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("INFO", "Clicked launch camera button");
                requestCameraPermission();
            }
        });
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
            ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    // Handle the result of the permission request
    private void handlePermissionsResult(Map<String, Boolean> permissions) {
        // Handle Permission granted/rejected
        boolean permissionGranted = true;
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (entry.getKey().equals(Manifest.permission.CAMERA) && !entry.getValue()) {
                permissionGranted = false;
                break;
            }
        }

        if (!permissionGranted) {
            Toast.makeText(this,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show();
        } else {
            startCamera();
        }
    }

    // Handle the result of the permission request for older Android versions
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed with camera-related operations
                Log.i("INFO", "Permission request granted after request, starting camera");
                startCamera();
            } else {
                // Camera permission denied. You may want to show a message or take alternative actions.
                Toast.makeText(this,
                        "Permission request denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        // Add your logic to start the camera here
        Log.i("INFO", "Starting camera now!");
    }
}
