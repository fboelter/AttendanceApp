package com.cs407.attendanceapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        ImageButton downloadButton = findViewById(R.id.downloadButton);
        TextView noGradesTextView = findViewById(R.id.tvNoGrades);

        db.collection("Classes").document(classDocumentId).collection("Students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<GradeItem> gradeItems = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String studentId = document.getId();
                        double grade = document.getDouble("grade") != null ? document.getDouble("grade") * 100 : 0.0;
                        gradeItems.add(new GradeItem(studentId, grade));
                    }

                    if (gradeItems.isEmpty()) {
                        downloadButton.setVisibility(View.GONE);
                        noGradesTextView.setVisibility(View.VISIBLE);

                    } else {
                        downloadButton.setVisibility(View.VISIBLE);
                        noGradesTextView.setVisibility(View.GONE);
                    }

                    GradesAdapter adapter = new GradesAdapter(ProfGradebookPage.this, gradeItems);
                    ListView lvGrades = findViewById(R.id.lvGrades);
                    lvGrades.setAdapter(adapter);

                    lvGrades.setOnItemClickListener((parent, view, position, id) -> {
                        GradeItem item = gradeItems.get(position);
                        Intent intent = new Intent(ProfGradebookPage.this, GradeReviewPage.class);
                        intent.putExtra("studentId", item.getStudentId());
                        intent.putExtra("classDocumentId", classDocumentId);
                        startActivity(intent);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfGradebookPage.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
