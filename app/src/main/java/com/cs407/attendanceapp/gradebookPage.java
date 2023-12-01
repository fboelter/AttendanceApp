package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class gradebookPage extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String classDocumentId;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gradebook_page);

        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        classDocumentId = getIntent().getStringExtra("classDocumentId");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        ImageButton download = findViewById(R.id.downloadButton);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAndShareGradebook();
            }
        });

        // Display grade list view
        fetchAndDisplayStudents();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void fetchAndShareGradebook() {

        db.collection("Classes")
                .document(classDocumentId) // Replace with your class document ID
                .collection("Students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String csvData = toCsv(queryDocumentSnapshots.getDocuments());
                    File csvFile = createCsvFile(csvData);
                    if (csvFile != null) {
                        shareCsvFile(csvFile);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(gradebookPage.this, "Error fetching data", Toast.LENGTH_SHORT).show());
    }

    private String toCsv(List<DocumentSnapshot> documents) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Email, Attendance\n");
        for (DocumentSnapshot document : documents) {
            String email = document.getId(); // Assuming the ID is the email
            Double attendanceDouble = document.getDouble("grade");
            float attendance = attendanceDouble != null ? attendanceDouble.floatValue() : 0.0f; // Handle null
            csvBuilder.append(email).append(", ").append(attendance).append("\n");
        }
        return csvBuilder.toString();
    }

    private File createCsvFile(String csvData) {
        File file = null;
        try {
            file = new File(Environment.getExternalStorageDirectory(), "attendance.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            writer.append(csvData);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            Log.e("CreateCsvFile", "Error: " + e.getMessage());
        }
        return file;
    }


    private void shareCsvFile(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Gradebook");

        Uri uri = FileProvider.getUriForFile(this, "com.cs407.attendanceapp2.fileprovider", file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    private void fetchAndDisplayStudents() {
        db.collection("Classes").document(classDocumentId).collection("Students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    LinearLayout studentListContainer = findViewById(R.id.studentListContainer);
                    studentListContainer.removeAllViews(); // Clear previous views if any
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String email = document.getId();
                        Number gradeNumber = document.getDouble("grade"); // Try to get the grade as a Double
                        if (gradeNumber == null) {
                            gradeNumber = document.getLong("grade"); // If Double is null, try Long
                        }

                        double grade = gradeNumber != null ? gradeNumber.doubleValue() : 0.0; // Convert to double or default to 0.0
                        addStudentView(studentListContainer, email, grade);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    private void addStudentView(LinearLayout container, String email, double grade) {
        // Create a new horizontal LinearLayout for each item
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.list_item_background)); // Assuming you have list_item_background.xml

        TextView emailView = new TextView(this);
        emailView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        emailView.setText(email);
        emailView.setTextSize(18); // Set your desired size
        emailView.setTypeface(null, Typeface.BOLD);
        emailView.setGravity(Gravity.START);

        TextView gradeView = new TextView(this);
        gradeView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        gradeView.setText(String.format(Locale.getDefault(), "%.2f%%", grade * 100));
        gradeView.setTextSize(18); // Set your desired size
        gradeView.setTypeface(null, Typeface.BOLD);
        gradeView.setGravity(Gravity.END);

        itemLayout.addView(emailView);
        itemLayout.addView(gradeView);

        container.addView(itemLayout);
    }

    private void showProfilePopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_signout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(gradebookPage.this, LoginPage.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        popupMenu.show();
    }
}
