package com.cs407.attendanceapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class StudentGradeBook extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String doc_id;
    private String userEmail;

    private int done;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_gradebook_page);
        Intent receivedIntent = getIntent();
        this.doc_id = receivedIntent.getStringExtra("classDocumentId");
        this.userEmail = receivedIntent.getStringExtra("userEmail");
        TextView noGrade = findViewById(R.id.textView);
        noGrade.setVisibility(View.GONE);

        initializeUIComponents();
        progressBar();

    }

    private void progressBar(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("doc_id", doc_id);
        Log.d("userEmail", userEmail);
        List<String> daysAttended = new ArrayList<>();
        List<String> daysMissed = new ArrayList<>();
        CollectionReference classesRef = db.collection("Classes");


        classesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot classDocument : task.getResult()) {
                        String documentId = classDocument.getId();
                        if(documentId.equals(doc_id)) {
                            CollectionReference studentsRef = classDocument.getReference().collection("Students");
                            studentsRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> studentTask) {
                                    if (studentTask.isSuccessful()) {
                                        String studentDocumentId = classDocument.getId();
                                        for (QueryDocumentSnapshot studentDocument : studentTask.getResult()) {
                                            // Access and process data from the 'Students' subcollection here
                                            String studentEmail = studentDocument.getId();
                                            Log.d("Before Email", studentEmail);

                                            if (studentEmail.equals(userEmail)) {
                                                Log.d("email", studentEmail);
                                                Object daysAttendedObject = studentDocument.get("days_attended");
                                                Object daysMissedObject = studentDocument.get("days_missed");
                                                List<Object> attendTimestampList = (List<Object>) daysAttendedObject;
                                                List<Object> missedTimestampList = (List<Object>) daysMissedObject;
                                                List<String> formattedAttendDates = new ArrayList<>();
                                                List<String> formattedMissedDates = new ArrayList<>();
                                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                for (Object timestamp : attendTimestampList) {
                                                    if (timestamp instanceof com.google.firebase.Timestamp) {
                                                        com.google.firebase.Timestamp firebaseTimestamp = (com.google.firebase.Timestamp) timestamp;
                                                        Date date = firebaseTimestamp.toDate();
                                                        String formattedDate = dateFormat.format(date);
                                                        formattedAttendDates.add(formattedDate);
                                                        // Format 'date' as needed or perform actions
                                                        Log.d("Timestamp", "Date: " + date);
                                                    }
                                                }
                                                assert missedTimestampList != null;
                                                for (Object timestamp : missedTimestampList) {
                                                    if (timestamp instanceof com.google.firebase.Timestamp) {
                                                        com.google.firebase.Timestamp firebaseTimestamp = (com.google.firebase.Timestamp) timestamp;
                                                        Date date = firebaseTimestamp.toDate();
                                                        String formattedDate = dateFormat.format(date);
                                                        formattedMissedDates.add(formattedDate);
                                                        // Format 'date' as needed or perform actions
                                                        Log.d("Timestamp", "Date: " + date);

                                                    }
                                                }
                                                double attend_count = attendTimestampList.size();
                                                double missed_count = missedTimestampList.size();
                                                Log.d("attend_count", String.valueOf(attend_count));
                                                Log.d("missed_count", String.valueOf(missed_count));
                                                Log.d("attend", String.valueOf(attendTimestampList.isEmpty()));
                                                Log.d("missed", String.valueOf(missedTimestampList.isEmpty()));
                                                if (attendTimestampList.isEmpty() && missedTimestampList.isEmpty()) {
                                                    ListView missed = findViewById(R.id.days_missed);
                                                    ListView attend = findViewById(R.id.days_attended);
                                                    TextView text3 = findViewById(R.id.textView3);
                                                    TextView text4 = findViewById(R.id.textView4);
                                                    TextView text5 = findViewById(R.id.textView5);
                                                    TextView text = findViewById(R.id.textView);
                                                    missed.setVisibility(View.GONE);
                                                    attend.setVisibility(View.GONE);
                                                    text3.setVisibility(View.GONE);
                                                    text4.setVisibility(View.GONE);
                                                    text5.setVisibility(View.GONE);
                                                    text.setVisibility(View.VISIBLE);
                                                    Toolbar toolbar = findViewById(R.id.toolbar);
                                                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) toolbar.getLayoutParams();
                                                    layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID; // To constraint to the top of the parent
                                                    layoutParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET; // Remove the bottom constraint if needed
                                                    toolbar.setLayoutParams(layoutParams);
                                                } else {
                                                    double total = attend_count + missed_count;
                                                    double percentage = (attend_count / total) * 100;
                                                    this.displayListView(formattedAttendDates, formattedMissedDates);
                                                    TextView grade = findViewById(R.id.textView3);
                                                    @SuppressLint("DefaultLocale") String result = "Grade " + ": " + attend_count + "/" + total + " (" + String.format("%.2f", percentage) + "%" + ")";
                                                    grade.setText(result);
                                                }

                                            }
                                        }

                                    }

                                    // You may need to perform actions here after processing 'Students' subcollection
                                    // This block runs after fetching all 'Students' documents for a specific 'Class'
                                    else {
                                        // Handle errors in fetching 'Students' documents
                                        Log.e("FirestoreQuery", "Error getting 'Students' documents: " + studentTask.getException());
                                    }
                                }

                                private void displayListView(List<String> formattedAttendDates, List<String> formattedMissedDates) {
                                    Collections.sort(formattedAttendDates);
                                    Collections.sort(formattedMissedDates);
                                    ListView listViewDaysAttended = findViewById(R.id.days_attended);
                                    ListView listViewDaysMissed = findViewById(R.id.days_missed);
                                    ArrayAdapter<String> adapterDaysAttended = new ArrayAdapter<>(StudentGradeBook.this, android.R.layout.simple_list_item_1, formattedAttendDates);
                                    ArrayAdapter<String> adapterDaysMissed = new ArrayAdapter<>(StudentGradeBook.this, android.R.layout.simple_list_item_1, formattedMissedDates);
                                    listViewDaysAttended.setAdapter(adapterDaysAttended);
                                    listViewDaysMissed.setAdapter(adapterDaysMissed);
                                }
                            });
                        }
                    }
                } else {
                    // Handle errors in fetching 'Classes' documents
                    Log.e("FirestoreQuery", "Error getting 'Classes' documents: " + task.getException());
                }
            }
        });
    }

    private void initializeUIComponents() {
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);
        mAuth = FirebaseAuth.getInstance();
    }


    private void showProfilePopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_signout) {
                    // Handle the "Sign Out" action here using Firebase Authentication
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(StudentGradeBook.this, LoginPage.class);
                    startActivity(intent);
                    // You can also navigate the user back to the login screen or perform other actions as needed.
                    finish();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }


    }
