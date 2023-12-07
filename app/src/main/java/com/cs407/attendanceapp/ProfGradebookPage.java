package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

public class ProfGradebookPage extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String classDocumentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gradebook_page);

        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        classDocumentId = getIntent().getStringExtra("classDocumentId");

        ImageButton download = findViewById(R.id.downloadButton);
        download.setOnClickListener(v -> fetchAndShareGradebook());

        // Display grade list view
        fetchAndDisplayStudents();
    }

    private void fetchAndShareGradebook() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                .addOnFailureListener(e -> Toast.makeText(ProfGradebookPage.this, "Error fetching data", Toast.LENGTH_SHORT).show());
    }

    private String toCsv(List<DocumentSnapshot> documents) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Email, Attendance\n");
        for (DocumentSnapshot document : documents) {
            String email = document.getId(); // Assuming the ID is the email
            Double attendanceDouble = document.getDouble("grade");
            float attendance = attendanceDouble != null ? attendanceDouble.floatValue() * 100 : 0.0f; // Handle null
            csvBuilder.append(email).append(", ").append(String.format("%.2f%%", attendance)).append("\n");
        }
        return csvBuilder.toString();
    }

    private File createCsvFile(String csvData) {
        File file = null;
        try {
            file = new File(getExternalFilesDir(null), "attendance.csv");
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
                        String studentId = document.getId();
                        Number gradeNumber = document.getDouble("grade"); // Try to get the grade as a Double
                        if (gradeNumber == null) {
                            gradeNumber = document.getLong("grade"); // If Double is null, try Long
                        }

                        double grade = gradeNumber != null ? gradeNumber.doubleValue() : 0.0; // Convert to double or default to 0.0
                        addStudentView(studentListContainer, studentId, grade);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    private void addStudentView(LinearLayout container, String studentId, double grade) {
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
        emailView.setText(studentId);
        emailView.setTextSize(18); // Set your desired size
        emailView.setTypeface(null, Typeface.BOLD);
        emailView.setGravity(Gravity.START);

        TextView gradeView = new TextView(this);
        gradeView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        gradeView.setText(String.format(Locale.getDefault(), "%.2f%%", grade * 100));
        gradeView.setTextSize(18); // Set your desired size
        gradeView.setTypeface(null, Typeface.BOLD);
        gradeView.setGravity(Gravity.END);

        itemLayout.addView(emailView);
        itemLayout.addView(gradeView);

        container.addView(itemLayout);

        itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfGradebookPage.this, GradeReviewPage.class);
                intent.putExtra("studentId", studentId);
                intent.putExtra("classDocumentId", classDocumentId);
                startActivity(intent);
            }
        });
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
                    Intent intent = new Intent(ProfGradebookPage.this, LoginPage.class);
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
