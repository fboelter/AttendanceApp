package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;


import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentHomePage extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private ListView listView;
    private ListView listViewAll;
    private List<Course> classList;
    private List<Course> classListAll;
    private CourseAdapter adapter;
    private CourseAdapter adapter_all;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_page);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        mAuth = FirebaseAuth.getInstance();

        listView = findViewById(R.id.class_list);
        listViewAll = findViewById(R.id.class_list_all);
        classList = new ArrayList<>();
        classListAll = new ArrayList<>();
        adapter = new CourseAdapter(this, classList);
        adapter_all = new CourseAdapter(this, classListAll);
        listView.setAdapter(adapter);
        listViewAll.setAdapter(adapter_all);

        // Get the currently authenticated user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userEmail = currentUser.getEmail(); // The user's email
            Date currentDate = Calendar.getInstance().getTime();
            // Initialize Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference classesRef = db.collection("Classes");

            classesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot classDocument : task.getResult()) {
                            // Access the class data
                            String className = classDocument.getString("course_name");
                            List<String> daysOfWeek = (List<String>) classDocument.get("days_of_week");
                            Timestamp timeStart = classDocument.getTimestamp("time_start");
                            Timestamp timeEnd = classDocument.getTimestamp("time_end");
                            String startTime = formatTime(timeStart);
                            String endTime = formatTime(timeEnd);
                            String timeRange = startTime + " - " + endTime;
                            Query studentQuery = classesRef.whereArrayContains("student_emails", userEmail);

                            studentQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> studentTask) {
                                    if (studentTask.isSuccessful() && !studentTask.getResult().isEmpty()) {
                                        // The user's email is in the student_emails array for this class
                                        classListAll.add(new Course(className, timeRange));
                                        adapter_all.notifyDataSetChanged();

                                    }
                                }
                            });

                            if (isCourseScheduledToday(currentDate, daysOfWeek, timeStart, timeEnd)) {
                                // Check if today is within the schedule and user's email is in the student_emails
                            studentQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> studentTask) {
                                    if (studentTask.isSuccessful() && !studentTask.getResult().isEmpty()) {
                                        // The user's email is in the student_emails array for this class
                                        classList.add(new Course(className, timeRange));
                                        adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
                                    }
                                }
                            });
                            }
                        }
                    } else {
                        // Handle errors
                        Log.e("FirestoreQuery", "Error getting documents: " + task.getException());
                    }
                }
            });
        }
        listViewAll.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected course from the list
                Course selectedCourse = classListAll.get(position);

                // Create an intent to open the CourseDetailsActivity
                Intent intent = new Intent(StudentHomePage.this, gradebookPage.class);

                // Start the new activity
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
                    // Handle the "Sign Out" action here using Firebase Authentication
                    FirebaseAuth.getInstance().signOut();
                    // You can also navigate the user back to the login screen or perform other actions as needed.
                    finish();
                    return true;
                }
                return false;
            }
        });

        popupMenu.show();
    }

    private boolean isCourseScheduledToday(Date currentDate, List<String> daysOfWeek, Timestamp timeStart, Timestamp timeEnd) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDay = getDayOfWeek(currentDayOfWeek);

        if (daysOfWeek != null && daysOfWeek.contains(currentDay)) {
            Date startTime = timeStart.toDate();
            Date endTime = timeEnd.toDate();

            return currentDate.after(startTime) && currentDate.before(endTime);
        }

        return false;
    }

    private String getDayOfWeek(int dayOfWeek) {
        String[] days = new String[]{"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek];
    }

    private String formatTime(Timestamp timestamp) {
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mma", Locale.US);
        return sdf.format(date);
    }
}

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_profile) {
//            // Handle the profile icon click event
//            openUserProfile();
//            return true;
//        } else if (item.getItemId() == R.id.action_signout) {
//            // Handle the "Sign Out" action here using Firebase Authentication
//            FirebaseAuth.getInstance().signOut();
//            // You can also navigate the user back to the login screen or perform other actions as needed.
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
