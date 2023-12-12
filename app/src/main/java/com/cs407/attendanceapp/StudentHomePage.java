package com.cs407.attendanceapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cs407.attendanceapp.CourseAdapter.OnCourseClickListener;
import com.cs407.attendanceapp2.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class StudentHomePage extends AppCompatActivity implements OnCourseClickListener {

    private FirebaseAuth mAuth;
    private ListView listViewAll;
    private List<Course> classList;
    private List<Course> classListAll;
    private CourseAdapter adapter;
    private CourseAdapter adapter_all;
    private ImageButton addCourseStudentButton;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 101;
    LocationManager locationManager;
    LocationListener locationListener;
    boolean markedPresent;
    Location studentLocation;
    boolean locationObtained = false; // TODO: update setting of locationObtained and markedPresent so that you don't have to retake attendance when you sign out
    /*
    TODO:
    - double check the location checking logic (ensure that location is only taken once when location is requested,
        if requested a second time, take location again
    - test the reverse of attendance (prof on phone, student on computer)
    = test ranges
    - if a student doesn't attend after class ends, mark them as absent
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample);

        initializeUIComponents();
        initializeListView();
        setupAddCourseButtonListener();
        setupListViewItemClickListener();
        fetchAndDisplayCourses();
        FirebaseApp.initializeApp(this);

        // Retrieve extra values from the Intent
        String barcodeValue = getIntent().getStringExtra("barcodeValue");

        // Use the received value as needed
        if (barcodeValue != null) {
            // Task completed successfully
            Log.i("INFO", "Class Id is: " + barcodeValue);
            addStudentToFirebaseCourse(barcodeValue);
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Initialize the location listener
        locationListener = new MyLocationListener();

        // Now you can use locationListener to listen for location updates
    }

    // Your activity or service methods

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (!markedPresent) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.i("INFO", "Student location changed: Lat: " + latitude + ", Long: " + longitude);
                studentLocation = location;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    }

    private void initializeUIComponents() {
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);
        addCourseStudentButton = findViewById(R.id.plusButton);
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeListView() {
        ListView listView = findViewById(R.id.class_list);
        listViewAll = findViewById(R.id.class_list_all);
        classList = new ArrayList<>();
        classListAll = new ArrayList<>();
        adapter = new CourseAdapter(this, classList, this);
        adapter_all = new CourseAdapter(this, classListAll, this);
        listView.setAdapter(adapter);
        listViewAll.setAdapter(adapter_all);
    }

    private void setupAddCourseButtonListener() {
        addCourseStudentButton.setOnClickListener(v -> {
            Log.i("INFO", "Add course button clicked");
            navigateToCameraPreview();
        });
    }

    private void setupListViewItemClickListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        listViewAll.setOnItemClickListener((parent, view, position, id) -> {
            Course selectedCourse = classListAll.get(position);

            Intent intent = new Intent(StudentHomePage.this, StudentGradeBook.class);

            intent.putExtra("classDocumentId", selectedCourse.getId());
            assert currentUser != null;
            intent.putExtra("userEmail", Objects.requireNonNull(currentUser.getEmail()).toLowerCase());
            startActivity(intent);
        });
    }

    private void fetchAndDisplayCourses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = Objects.requireNonNull(currentUser.getEmail()).toLowerCase();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference classesRef = db.collection("Classes");

            classesRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot classDocument : task.getResult()) {
                        // Access the class data
                        String classDocumentId = classDocument.getId();
                        String className = classDocument.getString("course_name");
                        List<String> daysOfWeek = (List<String>) classDocument.get("days_of_week");
                        assert daysOfWeek != null;
                        Log.d("DAYS OF WEEK", daysOfWeek.toString());
                        Timestamp timeStart = classDocument.getTimestamp("time_start");
                        Timestamp timeEnd = classDocument.getTimestamp("time_end");
                        assert timeStart != null;
                        String startTime = formatTime(timeStart);
                        assert timeEnd != null;
                        String endTime = formatTime(timeEnd);
                        Date startDate = formatDate(timeStart);
                        Date endDate = formatDate(timeEnd);
                        String timeRange = startTime + " - " + endTime;

                        List<String> studentEmails = new ArrayList<>();
                        classDocument.getReference().collection("Students").get()
                                .addOnCompleteListener(studentTask -> {
                                    if (studentTask.isSuccessful()) {
                                        for (QueryDocumentSnapshot studentDocument : studentTask.getResult()) {
                                            studentEmails.add(studentDocument.getId());
                                        }
                                        Course course = new Course(className, timeRange, classDocumentId, daysOfWeek, startDate, endDate);
                                        if (studentEmails.contains(userEmail)) {
                                            classListAll.add(course);
                                            adapter_all.notifyDataSetChanged();

                                            // Check if the class is scheduled for the current day
                                            if (course.isCourseScheduledToday()) {
                                                classList.add(course);
                                                adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
                                            }
                                        }
                                    }
                                });
                    }
                } else {
                    // Handle errors
                    Log.e("FirestoreQuery", "Error getting documents: " + task.getException());
                }
            });
        }
    }

    private void showProfilePopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_signout) {
                // Handle the "Sign Out" action here using Firebase Authentication
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(StudentHomePage.this, LoginPage.class);
                startActivity(intent);
                // You can also navigate the user back to the login screen or perform other actions as needed.
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private String formatTime(Timestamp timestamp) {
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mma", Locale.US);
        return sdf.format(date);
    }

    private Date formatDate(Timestamp timestamp) {
        return timestamp.toDate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            handleLocationPermissionResult(grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void handleLocationPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted, proceed with location-related operations
            Log.i("INFO", "Location permission granted, starting location updates");
            startListening(); // Replace with your location-related operation
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCourseToStudentCourses(String classId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        classesRef.document(classId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document exists, access its fields
                        String courseName = documentSnapshot.getString("course_name");

                        Timestamp timeStart = documentSnapshot.getTimestamp("time_start");
                        Timestamp timeEnd = documentSnapshot.getTimestamp("time_end");
                        String startTime = null;
                        if (timeStart != null) {
                            startTime = formatTime(timeStart);
                        }
                        String endTime = null;
                        if (timeEnd != null) {
                            endTime = formatTime(timeEnd);
                        }
                        String timeRange = startTime + " - " + endTime;
                        Date startDate = null;
                        if (timeStart != null) {
                            startDate = formatDate(timeStart);
                        }
                        Date endDate = null;
                        if (timeEnd != null) {
                            endDate = formatDate(timeEnd);
                        }

                        List<String> daysOfWeek = (List<String>) documentSnapshot.get("days_of_week");

                        // Add course to student's course list
                        Log.i("INFO", "Adding course: " + classId + " , " + courseName + ", " + timeRange);
                        Course newCourse = new Course(courseName, timeRange, classId, daysOfWeek, startDate, endDate);
                        if (!classListAll.contains(newCourse)) {
                            classListAll.add(newCourse);
                            Log.i("INFO", "AdapterAll notified");
                        } else if (newCourse.isCourseScheduledToday() && !classList.contains(newCourse)) {
                            classList.add(newCourse);
                            Log.i("INFO", "Adapter notified");
                        }
                        Toast.makeText(StudentHomePage.this, "Class successfully added", Toast.LENGTH_SHORT).show();
                    } else {
                        // Document does not exist
                        Toast.makeText(StudentHomePage.this, "Document with ID " + classId + " does not exist.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Toast.makeText(StudentHomePage.this, "Error getting document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "Error getting document: " + e.getMessage());
                });
    }

    private void addStudentToFirebaseCourse(String classId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String studentEmail;
        if (currentUser != null) {
            studentEmail = currentUser.getEmail();
        } else {
            studentEmail = null;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        CollectionReference studentsRef = classesRef.document(classId).collection("Students");

        // Check that course exists
        DocumentReference classDocumentRef = classesRef.document(classId);
        classDocumentRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Map<String, Object> newData = new HashMap<>();
                    newData.put("days_attended", Collections.emptyList());
                    newData.put("days_missed", Collections.emptyList());
                    newData.put("grade", 0);

                    if (studentEmail != null) {
                        studentsRef.document(studentEmail).set(newData, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    Log.i("Info", "Student document set in Firebase successfully");
                                    addCourseToStudentCourses(classId);

                                    // if they both succeed, send Toast
                                    Toast.makeText(StudentHomePage.this, "Course added successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firebase", "Student document failed to update");
                                    Toast.makeText(StudentHomePage.this, "Student was not added to class. Try again.", Toast.LENGTH_LONG).show();
                                });
                    }
                } else {
                    Toast.makeText(StudentHomePage.this, "Course does not exist. Try again.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(StudentHomePage.this, "Error occurred while fetching the class. Try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /*
     * Obtains the professor's coordinates from Firebase and compares to the student's location.
     * Assumes that studentLocation and course are not null.
     * On success, continues on to mark student as present in Firebase
     * Otherwise, shows a Toast with an error message and markedPresent remains false.
     */
    public void compareStudentAndProfessorLocation(Course course, Location studentLocation) {
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

                                // Create an Android Location object
                                Location professorLocation = new Location("provider");
                                professorLocation.setLatitude(latitude);
                                professorLocation.setLongitude(longitude);

                                double rangeInMeters;

                                try {
                                    // Try to retrieve the value from the document
                                    Double rangeFromDocument = document.getDouble("location_range");

                                    // Check if the value is not null
                                    if (rangeFromDocument != null) {
                                        rangeInMeters = rangeFromDocument;
                                    } else {
                                        // If the value is null, set a default value of 10
                                        rangeInMeters = 10;
                                    }
                                } catch (NullPointerException npe) {
                                    // Handle any potential NullPointerException
                                    npe.printStackTrace();
                                    rangeInMeters = 10; // Set a default value if an exception occurs
                                }

                                float distance = studentLocation.distanceTo(professorLocation);
                                Log.i("INFO", "PROF LOCATION: " + professorLocation.toString() + " STUDENT LOCATION: " + studentLocation.toString() + "Distance: " + distance);

                                // Check if the distance is within the specified range
                                if (professorLocation.getLongitude() != 0.0 && professorLocation.getLongitude() != 0 && distance <= rangeInMeters) {
                                    // update the course's data in Firebase
                                    queryForCurrentCourse(course);
                                } else {
                                    markedPresent = false;
                                    Toast.makeText(StudentHomePage.this, "Not marked present. Consider moving closer to the front of the room", Toast.LENGTH_LONG).show();
                                    locationObtained = false; // reset location variable so we can check a new coordinate
                                }

                            } else {
                                // Handle the case when prof_location is not available
                                Toast.makeText(StudentHomePage.this, "Could not take attendance. Professor location not available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                    }
                });

    }

    /*
     * Responds to clicking on the button in the course object (either calendar or checkmark)
     * If attendance has not been taken, takes attendance.
     * Else, prints a log message.
     */
    @Override
    public void onCourseClick(Course course) {
        Log.i("INFO", "Course clicked. locationObtained: " + locationObtained);
        try {
        if ((!markedPresent || !locationObtained) && locationListener != null) {
            /*
             * Taking attendance happens in a series of steps, all of which must succeed
             * in order to be marked present. Since Firebase updates are asynchronous void functions,
             * the steps to take attendance are nested within each other.
             * listenForLocation(course) ->
             * compareStudentAndProfessorLocation(course, studentLocation) ->
             * queryForCurrentCourse(course) ->
             * updateStudentAttendance(final DocumentReference classDocRef, final String userEmail, Course course)
             * If updateStudentAttendance succeeds, markedPresent is set to *true*. updateStudentAttendance
             * handles updating the button and displaying a success message Toast on success.
             * Otherwise, there will be a toast in the erring function with the error message and
             * markedPresent remains false
             */
            listenForLocation(course);
        } } catch(Exception e) {
            Toast.makeText(StudentHomePage.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        // either marked present and have location, or locationListener is null
        Log.i("INFO", "After checking to listen, locationObtained: " + locationObtained + " Marked present: " + markedPresent);
    }

    private void listenForLocation(Course course) {
        if (!locationObtained) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                studentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (studentLocation != null & !locationObtained) {
                    updateLocationInfo(); // this will mark locationObtained = true on success
                    if (locationObtained){
                        compareStudentAndProfessorLocation(course, studentLocation);
                    } else {
                        Toast.makeText(StudentHomePage.this, "listenForLocation. updateLocationInfo failed", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            // location has been obtained
            if (studentLocation != null) {
                compareStudentAndProfessorLocation(course, studentLocation);
            }
        }
    }

    /*
     * Queries Firebase for the Course course (finds by classId)
     * On success, updates student attendance
     * Otherwise, shows an error Toast
     */
    private void queryForCurrentCourse(Course course) {
        Log.i("INFO", "Marking present in Firebase");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            // Reference to the "Classes" collection
            CollectionReference classesRef = db.collection("Classes");
            classesRef.whereEqualTo("course_name", course.getCourseName())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot classDocument : task.getResult()) {
                                // For each matching document, check if userEmail is in "Students" collection
                                updateStudentAttendance(classDocument.getReference(), userEmail, course);
                            }
                        }  else {
                            Toast.makeText(StudentHomePage.this, "Something went wrong when looking for current class in queryForCurrentCourse", Toast.LENGTH_LONG);
                        }

                    });
            // Task never completed
        }
        else {
            Toast.makeText(StudentHomePage.this, "Authentication error in queryForCurrentCourse: currentUser is null", Toast.LENGTH_LONG);
        }
    }

    /*
     * Given a classDocumentReference, this function queries Firebase for the course's Students collection,
     * finds the document whose id is the student's email, and adds the current date to
     * days_attended.
     * On success, markedPresent is set to true, the course button changes to a checkmark, and a success Toast
     * is shown and control returns to onCourseClick
     * On failure, an error message Toast appears
     */
    private void updateStudentAttendance(final DocumentReference classDocRef, final String userEmail, Course course) {
        // Reference to the "Students" collection
        CollectionReference studentsRef = classDocRef.collection("Students");
        Log.i("INFO", "Students collection: " + studentsRef.getId());

        DocumentReference userDocumentRef = studentsRef.document(userEmail);

        userDocumentRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {

                    // Update the array with the new value
                    userDocumentRef.update("days_attended", FieldValue.arrayUnion(new Timestamp(Calendar.getInstance().getTime())))
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    // Update successful
                                    Log.d("Firestore", "Days attended updated successfully");
                                    markedPresent = true;
                                    adapter.changeAttendanceButtonToCheckMark(course);
                                    Toast.makeText(StudentHomePage.this, "You have been marked as present", Toast.LENGTH_LONG).show();
                                } else {
                                    // Handle errors
                                    Exception exception = task1.getException();
                                    if (exception != null) {
                                        Log.e("Firestore", "Error updating days attended: " + exception.getMessage());
                                        Toast.makeText(StudentHomePage.this, "updateStudentAttendance: error getting days attended", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                } else {
                    // Handle the case where the document doesn't exists
                    Toast.makeText(StudentHomePage.this, "Student has not been enrolled in class", Toast.LENGTH_LONG).show();
                }
            } else {
                // Handle errors
                Exception exception = task.getException();
                if (exception != null) {
                    Log.e("Firestore", "Error getting document: " + exception.getMessage());
                    Toast.makeText(StudentHomePage.this, "Error getting document: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startListening() {
        Log.i("INFO", "Start listening. locationObtained: " + locationObtained);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void updateLocationInfo() {
        Log.i("INFO", "updateLocationInfo()");
        if (!locationObtained) {
            Log.i("INFO", "location has not been obtained. checking permissions");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            studentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (studentLocation != null){
                String latitude = String.valueOf(studentLocation.getLatitude());
                String longitude = String.valueOf(studentLocation.getLongitude());
                String accuracy = String.valueOf(studentLocation.getAccuracy());
                Log.i("INFO", "New Location? Lat: " + latitude + "\tLong: " + longitude + "\t Accuracy: " + accuracy);
                locationObtained = true;
            }
        }
        // location was already obtained
        Log.i("INFO", "updateLocationInfo: Location already obtained");
    }
    private void navigateToCameraPreview() {
        Intent intent = new Intent(this, ScanBarcodeActivity.class);
        startActivity(intent);
    }
}

