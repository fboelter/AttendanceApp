package com.cs407.attendanceapp;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cs407.attendanceapp2.R;

import java.util.Map;

public class ScanBarcodeActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
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
                    });

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        // Request camera permission
        activityResultLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private void startCamera() {
        // Add your logic to start the camera here
        Log.i("INFO", "startCamera()");
    }
}
