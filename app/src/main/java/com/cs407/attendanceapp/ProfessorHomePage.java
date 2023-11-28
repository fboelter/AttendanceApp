package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfessorHomePage extends AppCompatActivity {

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
        setContentView(R.layout.activity_professor_home_page);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        Button addClass = findViewById(R.id.plusButton);
        addClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCourseDialog();
            }
        });


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.class_list);
        listViewAll = findViewById(R.id.class_list_all);
        classList = new ArrayList<>();
        classListAll = new ArrayList<>();
        adapter = new CourseAdapter(this, classList);
        adapter_all = new CourseAdapter(this, classListAll);
        listView.setAdapter(adapter);
        listViewAll.setAdapter(adapter_all);

        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            Date currentDate = Calendar.getInstance().getTime();
            CollectionReference classesRef = db.collection("Classes");

            // Query for classes where the professor's email matches the current user
            classesRef.whereEqualTo("professor", userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot classDocument : task.getResult()) {
                                String classDocumentId = classDocument.getId();
                                String className = classDocument.getString("course_name");
                                Timestamp timeStart = classDocument.getTimestamp("time_start");
                                Timestamp timeEnd = classDocument.getTimestamp("time_end");
                                String startTime = formatTime(timeStart);
                                String endTime = formatTime(timeEnd);
                                String timeRange = startTime + " - " + endTime;
                                List<String> daysOfWeek = (List<String>) classDocument.get("days_of_week");

                                Course course = new Course(className, timeRange, classDocumentId);
                                classListAll.add(course);
                                if (isCourseScheduledToday(currentDate, daysOfWeek, timeStart, timeEnd)) {
                                    classList.add(course);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            adapter_all.notifyDataSetChanged();
                        } else {
                            Log.e("FirestoreQuery", "Error getting documents: " + task.getException());
                        }
                    });
        }
        listViewAll.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected course from the list
                Course selectedCourse = classListAll.get(position);

                // Create an intent to open the gradebookPage
                Intent intent = new Intent(ProfessorHomePage.this, gradebookPage.class);

                // Pass the class document ID to the gradebookPage activity
                intent.putExtra("classDocumentId", selectedCourse.getId());

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
                    Intent intent = new Intent(ProfessorHomePage.this, LoginPage.class);
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

    public void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialogue_add_course, null);
        builder.setView(dialogView);

        // Find the buttons in the dialog layout
        Button buttonStartDate = dialogView.findViewById(R.id.buttonStartDate);
        Button buttonEndDate = dialogView.findViewById(R.id.buttonEndDate);

        // Initialize the calendars
        final Calendar startDateCalendar = Calendar.getInstance();
        final Calendar endDateCalendar = Calendar.getInstance();

        CheckBox checkBoxMonday = dialogView.findViewById(R.id.checkboxMonday);
        CheckBox checkBoxTuesday = dialogView.findViewById(R.id.checkboxTuesday);
        CheckBox checkBoxWednesday = dialogView.findViewById(R.id.checkboxWednesday);
        CheckBox checkBoxThursday = dialogView.findViewById(R.id.checkboxThursday);
        CheckBox checkBoxFriday = dialogView.findViewById(R.id.checkboxFriday);

        // Get email to set professor field
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = currentUser.getEmail();


        // Set the onClickListeners for the date buttons
        buttonStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(startDateCalendar, buttonStartDate);
            }
        });

        buttonEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(endDateCalendar, buttonEndDate);
            }
        });

        EditText courseNameEditText = dialogView.findViewById(R.id.editTextCourseName);
        // Set up checkboxes or toggle buttons for days of the week
        // Set up date pickers for start and end date

        builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Get course Name
                String courseName = courseNameEditText.getText().toString();

                // Get days met
                ArrayList<String> selectedDays = new ArrayList<>();
                if (checkBoxMonday.isChecked()) selectedDays.add("Monday");
                if (checkBoxTuesday.isChecked()) selectedDays.add("Tuesday");
                if (checkBoxWednesday.isChecked()) selectedDays.add("Wednesday");
                if (checkBoxThursday.isChecked()) selectedDays.add("Thursday");
                if (checkBoxFriday.isChecked()) selectedDays.add("Friday");


                // Validate the data
                if (courseName.isEmpty()) {
                    Toast.makeText(ProfessorHomePage.this, "Please enter a course name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedDays.isEmpty()) {
                    Toast.makeText(ProfessorHomePage.this, "Please select at least one day of the week.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startDateCalendar.getTime().after(endDateCalendar.getTime())) {
                    Toast.makeText(ProfessorHomePage.this, "The end date must be after the start date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert Calendar instances to Timestamp for Firestore
                Timestamp timestampStart = new Timestamp(startDateCalendar.getTime());
                Timestamp timestampEnd = new Timestamp(endDateCalendar.getTime());

                // Put data into an object
                Map<String, Object> classData = new HashMap<>();
                classData.put("course_name", courseName);
                classData.put("days_of_week", selectedDays);
                classData.put("time_start", timestampStart);
                classData.put("time_end", timestampEnd);
                classData.put("professor", userEmail);

                // Connect with firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference classesRef = db.collection("Classes");

                // Add the new class to Firestore, with a uniquely generated ID
                classesRef.add(classData)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                // Data push successful, you can get the unique ID like this:
                                String uniqueClassId = documentReference.getId();
                                Toast.makeText(ProfessorHomePage.this, "Class added successfully with ID: " + uniqueClassId, Toast.LENGTH_SHORT).show();

                                // Create an intent to start the CourseDetailsActivity
                                Intent intent = new Intent(ProfessorHomePage.this, CourseDetails.class);
                                intent.putExtra("classId", documentReference.getId());
                                startActivity(intent);

                                // Optionally, if you want to finish the current activity so the user can't return to it by pressing the back button
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Data push failed
                                Toast.makeText(ProfessorHomePage.this, "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDatePickerDialog(final Calendar calendar, final Button dateButton) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (timeView, selectedHour, selectedMinute) -> {
                                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth, selectedHour, selectedMinute);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                dateButton.setText(dateTimeFormat.format(calendar.getTime()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                    );
                    timePickerDialog.show();
                },
                year, month, day
        );
        datePickerDialog.show();
    }
}