package com.cs407.attendanceapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.cs407.attendanceapp2.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GradeReviewPage extends AppCompatActivity {
    private TextView tvGrade;
    private TextView tvNoMissedClasses;
    String classDocumentId;
    String studentId;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_review_page);

        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        classDocumentId = getIntent().getStringExtra("classDocumentId");
        studentId = getIntent().getStringExtra("studentId");


        recyclerView = findViewById(R.id.recyclerViewForItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tvGrade = findViewById(R.id.tvGrade);
        tvNoMissedClasses = findViewById(R.id.tvNoMissedClasses);

        fetchMissedDaysAndGrade(recyclerView);
    }

    private void fetchMissedDaysAndGrade(RecyclerView recyclerView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Classes")
                .document(classDocumentId)
                .collection("Students")
                .document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        List<Date> daysMissed = new ArrayList<>();
                        List<Timestamp> timestamps = (List<Timestamp>) document.get("days_missed");
                        if (timestamps != null) {
                            for (Timestamp timestamp : timestamps) {
                                daysMissed.add(timestamp.toDate());
                            }
                        }

                        Number grade = document.getDouble("grade");

                        // Update RecyclerView with missed days
                        if (!daysMissed.isEmpty()) {
                            MissedDayAdapter adapter = new MissedDayAdapter(daysMissed, date -> showConfirmationDialog(date));
                            recyclerView.setAdapter(adapter);
                        } else {
                            tvNoMissedClasses.setVisibility(View.VISIBLE);
                            recyclerView.setAdapter(null);
                        }

                        // Set the grade text
                        if (grade != null) {
                            tvGrade.setText(String.format("Grade: %.2f%%", grade.floatValue() * 100));
                            tvGrade.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void showConfirmationDialog(Date date) {
        new AlertDialog.Builder(GradeReviewPage.this)
                .setTitle("Confirm Attendance")
                .setMessage("Do you want to mark this day as present?")
                .setPositiveButton("Yes", (dialog, which) -> markAsPresent(date))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void markAsPresent(Date date) {
        DocumentReference studentDocRef = FirebaseFirestore.getInstance()
                .collection("Classes")
                .document(classDocumentId)
                .collection("Students")
                .document(studentId);

        Timestamp timestamp = new Timestamp(date);

        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(studentDocRef);
            List<Timestamp> daysMissed = (List<Timestamp>) snapshot.get("days_missed");
            List<Timestamp> daysAttended = (List<Timestamp>) snapshot.get("days_attended");

            daysMissed.remove(timestamp);
            daysAttended.add(timestamp);

            double newGrade = calculateGrade(daysMissed.size(), daysAttended.size());

            transaction.update(studentDocRef, "days_missed", daysMissed);
            transaction.update(studentDocRef, "days_attended", daysAttended);
            transaction.update(studentDocRef, "grade", newGrade);

            return null;
        }).addOnSuccessListener(aVoid -> {
            fetchMissedDaysAndGrade(recyclerView);
        }).addOnFailureListener(e -> {
            Toast.makeText(GradeReviewPage.this, "Could not change student attendance", Toast.LENGTH_LONG).show();
        });
    }

    private double calculateGrade(int daysMissed, int daysAttended) {
        if (daysMissed == 0) {
            return 1.0; // 100%
        } else {
            return (double) daysAttended / (daysMissed + daysAttended);
        }
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
                    Intent intent = new Intent(GradeReviewPage.this, LoginPage.class);
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