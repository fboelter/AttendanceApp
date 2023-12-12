package com.cs407.attendanceapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
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

public class ProfessorHomePage extends AppCompatActivity implements CourseAdapter.OnCourseClickListener {

    private FirebaseAuth mAuth;
    private ListView listView;
    private ListView listViewAll;
    private List<Course> classList;
    private List<Course> classListAll;
    private CourseAdapter adapter;
    private CourseAdapter adapter_all;
    LocationManager locationManager;
    LocationListener locationListener;
    boolean locationObtained = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample);

        initializeUIComponents();
        initializeListView();
        setupListViewItemClickListener();
        fetchAndDisplayCourses();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateLocationInfo(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }
            @Override
            public void onProviderEnabled(String s){

            }
            @Override
            public void onProviderDisabled(String s){

            }
        };

    }

    private void checkIfAttendanceHasAlreadyBeenOpened(Course course) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        classesRef.whereEqualTo("course_name", course.getCourseName())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Access the GeoPoint from the Firestore document
                            GeoPoint geoPoint = document.getGeoPoint("prof_location");

                            if (geoPoint != null) {
                                // Extract latitude and longitude
                                double latitude = geoPoint.getLatitude();
                                double longitude = geoPoint.getLongitude();
                                if ((latitude != 0) && (longitude !=0))
                                {
                                    adapter.changeAttendanceButtonToCheckMark(course);
                                    locationObtained = true;
                                }
                            }
                        }
                    }
                });
    }

    private void initializeUIComponents() {
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);
        ImageButton addClassButton = findViewById(R.id.plusButton);
        addClassButton.setOnClickListener(v -> showAddCourseDialog());
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeListView() {
        listView = findViewById(R.id.class_list);
        listViewAll = findViewById(R.id.class_list_all);
        classList = new ArrayList<>();
        classListAll = new ArrayList<>();
        adapter = new CourseAdapter(this, classList, this::onCourseClick);
        adapter_all = new CourseAdapter(this, classListAll, this::onCourseClick);
        listView.setAdapter(adapter);
        listViewAll.setAdapter(adapter_all);
    }

    private void setupListViewItemClickListener() {
        listViewAll.setOnItemClickListener((parent, view, position, id) -> {
            Course selectedCourse = classListAll.get(position);
            Intent intent = new Intent(ProfessorHomePage.this, CourseDetails.class);
            intent.putExtra("classId", selectedCourse.getId());
            startActivity(intent);
        });
    }

    private void fetchAndDisplayCourses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference classesRef = db.collection("Classes");

            classesRef.whereEqualTo("professor", userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot classDocument : task.getResult()) {
                                String classDocumentId = classDocument.getId();
                                String className = classDocument.getString("course_name");
                                Timestamp timeStart = classDocument.getTimestamp("time_start");
                                Timestamp timeEnd = classDocument.getTimestamp("time_end");
                                Log.i("INFO", "timeEnd Timestamp: " + timeEnd);
                                String startTime = formatTime(timeStart);
                                String endTime = formatTime(timeEnd);
                                String timeRange = startTime + " - " + endTime;
                                Date startDate = formatDate(timeStart);
                                Date endDate = formatDate(timeEnd);
                                Log.i("INFO", "timeRange in professor course creation: " + timeRange);
                                List<String> daysOfWeek = (List<String>) classDocument.get("days_of_week");

                                Course course = new Course(className, timeRange, classDocumentId, daysOfWeek, startDate, endDate);
                                classListAll.add(course);
                                if (course.isCourseScheduledToday()) {
                                    classList.add(course);
                                    if (course.isClassHappeningNow()) {
                                        checkIfAttendanceHasAlreadyBeenOpened(course);
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                            adapter_all.notifyDataSetChanged();
                        } else {
                            Log.e("FirestoreQuery", "Error getting documents: " + task.getException());
                        }
                    });
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

    private Date formatDate(Timestamp timestamp) {
        return timestamp.toDate();
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

        // Initialize the calendars
        final Calendar startDateCalendar = Calendar.getInstance();
        final Calendar endDateCalendar = Calendar.getInstance();

        // Find the buttons in the dialog layout
        Button buttonStartDate = dialogView.findViewById(R.id.buttonStartDate);
        Button buttonEndDate = dialogView.findViewById(R.id.buttonEndDate);
        Button buttonStartTime = dialogView.findViewById(R.id.buttonStartTime);
        Button buttonEndTime = dialogView.findViewById(R.id.buttonEndTime);

        buttonStartDate.setOnClickListener(v -> showDatePickerDialog(startDateCalendar, buttonStartDate));
        buttonEndDate.setOnClickListener(v -> showDatePickerDialog(endDateCalendar, buttonEndDate));
        buttonStartTime.setOnClickListener(v -> showTimePickerDialog(startDateCalendar, buttonStartTime));
        buttonEndTime.setOnClickListener(v -> showTimePickerDialog(endDateCalendar, buttonEndTime));

        CheckBox checkBoxMonday = dialogView.findViewById(R.id.checkboxMonday);
        CheckBox checkBoxTuesday = dialogView.findViewById(R.id.checkboxTuesday);
        CheckBox checkBoxWednesday = dialogView.findViewById(R.id.checkboxWednesday);
        CheckBox checkBoxThursday = dialogView.findViewById(R.id.checkboxThursday);
        CheckBox checkBoxFriday = dialogView.findViewById(R.id.checkboxFriday);

        // Get email to set professor field
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = currentUser.getEmail();

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

                // Add location range in meters
                EditText locationRangeInput = dialogView.findViewById(R.id.locationRangeInput);
                String locationRangeText = locationRangeInput.getText().toString();
                double locationRange = -1;

                try {
                    locationRange = Double.parseDouble(locationRangeText);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }


                // Put data into an object
                Map<String, Object> classData = new HashMap<>();
                classData.put("course_name", courseName);
                classData.put("days_of_week", selectedDays);
                classData.put("time_start", timestampStart);
                classData.put("time_end", timestampEnd);
                classData.put("professor", userEmail);
                classData.put("location_range", locationRange);

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
                    // Set only the year, month, and day on the calendar instance
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth);

                    // Update the text on the button to reflect the chosen date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    String dateString = dateFormat.format(calendar.getTime());
                    dateButton.setText(getDateWithOrdinal(dateString)); // Assuming getDateWithOrdinal() adds the ordinal indicator to the date
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog(final Calendar calendar, final Button timeButton) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    // Set only the hour and minute on the calendar instance
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    // Update the text on the button to reflect the chosen time
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    String timeString = timeFormat.format(calendar.getTime());
                    timeButton.setText(timeString);
                },
                hour, minute, false
        );
        timePickerDialog.show();
    }

    private String getDateWithOrdinal(String dateString) {
        String[] splitDate = dateString.split(" ");
        int day = Integer.parseInt(splitDate[1].replaceAll(",", ""));
        return splitDate[0] + " " + day + getDayOfMonthSuffix(day) + ", " + splitDate[2];
    }

    private String getDayOfMonthSuffix(final int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }

    @Override
    public void onCourseClick(Course course) {
        Log.i("INFO", "onCourseClick: locationObtained: " + locationObtained);
        checkIfAttendanceHasAlreadyBeenOpened(course);
        try {
            if (!locationObtained) {
                listenForLocation(course);
            } else {
                showCloseAttendanceDialog(course);
            }
        } catch (Exception e){
            Toast.makeText(ProfessorHomePage.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForLocation(Course course) {
        Log.i("INFO", "listenForLocation");
        if (!locationObtained) {
            if (Build.VERSION.SDK_INT < 23) {
                startListening();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        updateLocationInfo(location);
                        updateProfLocationInFireBase(course, location);
                        locationObtained = true; // Set the flag to true after obtaining the location
                    }
                }
            }
        }
    }

    private void updateProfLocationInFireBase(Course course, Location location)
    {
        Log.i("INFO", "UpdateProfLocationinFireBase location: " + location);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        classesRef.whereEqualTo("course_name", course.getCourseName())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Access the document ID
                                String documentId = document.getId();

                                // Convert the Java Location to a GeoPoint
                                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                                // Update the prof_location field in the Firestore document
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("prof_location", geoPoint);

                                classesRef.document(documentId)
                                        .update(updates)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i("Firestore", "Prof location updated successfully for course: " + course.getCourseName());
                                                // Stop listening for location updates as we only want it once
                                                locationManager.removeUpdates(locationListener);
                                                // change icon to check mark
                                                Log.i("INFO", "location: " + location.getLatitude() + ", " + location.getLongitude());
                                                if (location.getLatitude() == 0 && location.getLongitude() == 0)
                                                {
                                                    adapter.changeAttendanceButtonToAttendance(course);
                                                    Toast.makeText(ProfessorHomePage.this, "Attendance successfully closed", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    adapter.changeAttendanceButtonToCheckMark(course);
                                                    Toast.makeText(ProfessorHomePage.this, "Attendance is open!", Toast.LENGTH_SHORT).show();
                                                }

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e("Firestore", "Error updating prof location", e);
                                            }
                                        });
                            }
                        } else {
                            Log.e("Firestore", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void updateLocationInfo(Location location) {
        Log.i("updateLocationInfo", location.toString());

        if (location != null){
            String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            String accuracy = String.valueOf(location.getAccuracy());
            Log.i("INFO", "Lat: " + latitude + "\tLong: " + longitude + "\t Accuracy: " + accuracy);
        }
    }

    private void showCloseAttendanceDialog(Course course) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to close attendance?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Handle "Yes" button click
                        closeAttendance(course);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing, attendance is still open
                    }
                })
                .show();
    }

    private void closeAttendance(Course course) {
        Log.i("INFO", "closeAttendance() called");
        Location loc = new Location("");
        loc.setLatitude(0.0);
        loc.setLongitude(0.0);

        updateProfLocationInFireBase(course, loc);
        locationObtained = false;
    }
}