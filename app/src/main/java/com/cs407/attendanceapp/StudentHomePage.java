package com.cs407.attendanceapp;

import android.Manifest.permission;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.view.PreviewView;

import com.cs407.attendanceapp.CourseAdapter.OnCourseClickListener;
import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentHomePage extends AppCompatActivity implements OnCourseClickListener {

    private FirebaseAuth mAuth;
    private ListView listView;
    private ListView listViewAll;
    private List<Course> classList;
    private List<Course> classListAll;
    private CourseAdapter adapter;
    private CourseAdapter adapter_all;
    private ImageButton addCourseStudentButton;
    public static final String[] REQUIRED_PERMISSIONS = {permission.CAMERA};
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 101;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageView imageView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private String barcodeValue;

    // LocationManager locationManager;
    // LocationListener locationListener;
    // boolean markedPresent;
    // boolean locationObtained = false; // TODO: update setting of locationObtained and markedPresent so that you don't have to retake attendance when you sign out

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
        barcodeValue = getIntent().getStringExtra("barcodeValue");

        // Use the received value as needed
        if (barcodeValue != null) {
            // Task completed successfully
            String classId = barcodeValue;
            Log.i("INFO", "Class Id is: " + classId);
            addStudentToFirebaseCourse(classId);
        }

        /*
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!locationObtained)
                {
                    updateLocationInfo(location);
                }
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
         */
    }

    private void initializeUIComponents() {
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);
        addCourseStudentButton = findViewById(R.id.plusButton);
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
            intent.putExtra("userEmail", currentUser.getEmail().toLowerCase());
            startActivity(intent);
        });
    }

    private void fetchAndDisplayCourses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail().toLowerCase();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference classesRef = db.collection("Classes");

            classesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot classDocument : task.getResult()) {
                            // Access the class data
                            String classDocumentId = classDocument.getId();
                            String className = classDocument.getString("course_name");
                            List<String> daysOfWeek = (List<String>) classDocument.get("days_of_week");
                            Log.d("DAYS OF WEEK", daysOfWeek.toString());
                            Timestamp timeStart = classDocument.getTimestamp("time_start");
                            Timestamp timeEnd = classDocument.getTimestamp("time_end");
                            String startTime = formatTime(timeStart);
                            String endTime = formatTime(timeEnd);
                            Date startDate = formatDate(timeStart);
                            Date endDate = formatDate(timeEnd);
                            String timeRange = startTime + " - " + endTime;

                            List<String> studentEmails = new ArrayList<>();
                            classDocument.getReference().collection("Students").get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> studentTask) {
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
                                        }
                                    });
                        }
                    } else {
                        // Handle errors
                        Log.e("FirestoreQuery", "Error getting documents: " + task.getException());
                    }
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
                    Intent intent = new Intent(StudentHomePage.this, LoginPage.class);
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
            // handleLocationPermissionResult(grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*
    private void handleLocationPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted, proceed with location-related operations
            Log.i("INFO", "Location permission granted, starting location updates");
            startListening(); // Replace with your location-related operation
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    */

    private void addCourseToStudentCourses(String classId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        classesRef.document(classId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Document exists, access its fields
                            String courseName = documentSnapshot.getString("course_name");

                            Timestamp timeStart = documentSnapshot.getTimestamp("time_start");
                            Timestamp timeEnd = documentSnapshot.getTimestamp("time_end");
                            String startTime = formatTime(timeStart);
                            String endTime = formatTime(timeEnd);
                            String timeRange = startTime + " - " + endTime;
                            Date startDate = formatDate(timeStart);
                            Date endDate = formatDate(timeEnd);

                            List<String> daysOfWeek = (List<String>) documentSnapshot.get("days_of_week");

                            // Add course to student's course list
                            Log.i("INFO", "Adding course: " + classId + " , " + courseName + ", " + timeRange);
                            Course newCourse = new Course(courseName, timeRange, classId, daysOfWeek, startDate, endDate);
                            if (!classListAll.contains(newCourse)){
                                classListAll.add(newCourse);
                                adapter_all.notifyDataSetChanged();
                            } else if (newCourse.isCourseScheduledToday() && !classList.contains(newCourse)) {
                                classList.add(newCourse);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            // Document does not exist
                            Log.d("Firestore", "Document with ID " + classId + " does not exist.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle errors
                        Log.e("Firestore", "Error getting document: " + e.getMessage());
                    }
                });
    }

    private void addStudentToFirebaseCourse(String classId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String studentEmail = currentUser.getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        CollectionReference studentsRef = classesRef.document(classId).collection("Students");

        Map<String, Object> newData = new HashMap<>();
        newData.put("days_attended", Arrays.asList());
        newData.put("days_missed", Arrays.asList());
        newData.put("grade", 0);

        studentsRef.document(studentEmail).set(newData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i("Info", "Student document set in Firebase successfully");
                        addCourseToStudentCourses(classId);

                        // if they both succeed, send Toast
                        Toast.makeText(getApplicationContext(), "Course added successfully", Toast.LENGTH_SHORT);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Firebase", "Student document failed to update");
                    }
                });
    }

        @Override
        public void onCourseClick (Course course){

        }

    /*

    public void takeAttendance(Course course, Location studentLocation)
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference classesRef = db.collection("Classes");
        classesRef.whereEqualTo("course_name", course.getCourseName())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
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
                                    Log.i("INFO", "Distance: " + distance);

                                    // Check if the distance is within the specified range
                                    if (professorLocation.getLongitude() != 0.0 && professorLocation.getLongitude() != 0 && distance <= rangeInMeters) {
                                        // TODO: update student gradebook with 1 to indicate attendance
                                        markPresentInFirebase(course);
                                    } else {
                                        markedPresent = false;
                                        Toast.makeText(StudentHomePage.this, "Not marked present. Consider moving closer to the front of the room", Toast.LENGTH_LONG).show();
                                    }

                                } else {
                                    // Handle the case when prof_location is not available
                                    Toast.makeText(StudentHomePage.this, "Could not take attendance. Professor location not available", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Log.e("Firestore", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    @Override
    public void onCourseClick(Course course) {
        Log.i("INFO", "Course clicked. locationObtained: " + locationObtained);
        if (!locationObtained) {
            listenForLocation(course);
        }
    }

    private void listenForLocation(Course course) {
        if (!locationObtained) {
            if (Build.VERSION.SDK_INT < 23) {
                startListening();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null & !locationObtained) {
                        updateLocationInfo(location);
                        locationObtained = true; // Set the flag to true after obtaining the location
                        takeAttendance(course, location);
                    }
                }
            }
        }
    }

    private void markPresentInFirebase(Course course) {
        Log.i("INFO", "Marking present in Firebase");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String formattedDate = dateFormat.format(currentDate);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = currentUser.getEmail();

        // Reference to the "Classes" collection
        CollectionReference classesRef = db.collection("Classes");
        classesRef.whereEqualTo("course_name", course.getCourseName())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot classDocument : task.getResult()) {
                                // For each matching document, check if userEmail is in "Students" collection
                                updateStudentAttendance(classDocument.getReference(), userEmail, formattedDate, course);
                            }
                        } else {
                            // Handle errors
                        }
                    }
                });
    }

    private void updateStudentAttendance(final DocumentReference classDocRef, final String userEmail, final String currentDate, Course course) {
        // Reference to the "Students" collection
        CollectionReference studentsRef = classDocRef.collection("Students");
        Log.i("INFO", "Students collection: " + studentsRef.getId());

        DocumentReference userDocumentRef = studentsRef.document(userEmail);

        userDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        // Update the array with the new value
                        userDocumentRef.update("days_attended", FieldValue.arrayUnion(new Timestamp(Calendar.getInstance().getTime())))
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            // Update successful
                                            Log.d("Firestore", "Days attended updated successfully");
                                            markedPresent = true;
                                            adapter.changeAttendanceButtonToCheckMark(course);
                                            Toast.makeText(getApplicationContext(), "You have been marked as present", Toast.LENGTH_LONG);
                                        } else {
                                            // Handle errors
                                            Exception exception = task.getException();
                                            if (exception != null) {
                                                Log.e("Firestore", "Error updating days attended: " + exception.getMessage());
                                            }
                                        }
                                    }
                                });
                    } else {
                        // Handle the case where the document doesn't exist
                    }
                } else {
                    // Handle errors
                    Exception exception = task.getException();
                    if (exception != null) {
                        Log.e("Firestore", "Error getting document: " + exception.getMessage());
                    }
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

    private void updateLocationInfo(Location location) {
        if (!locationObtained) {
            Log.i("INFO", "loctationObtained: " + locationObtained);

            String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            String accuracy = String.valueOf(location.getAccuracy());
            Log.i("INFO", "Lat: " + latitude + "\tLong: " + longitude + "\t Accuracy: " + accuracy);

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

            try {
                String address = "Could not find address";
                List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (listAddresses != null && listAddresses.size() > 0) {
                    Log.i("PlaceInfo", listAddresses.get(0).toString());
                    address = "Address: \n";
                    if (listAddresses.get(0).getSubThoroughfare() != null) {
                        address += listAddresses.get(0).getSubThoroughfare() + " ";
                    }
                    if (listAddresses.get(0).getThoroughfare() != null) {
                        address += listAddresses.get(0).getThoroughfare() + " ";
                    }
                    if (listAddresses.get(0).getLocality() != null) {
                        address += listAddresses.get(0).getLocality() + " ";
                    }
                    if (listAddresses.get(0).getPostalCode() != null) {
                        address += listAddresses.get(0).getPostalCode() + " ";
                    }
                    if (listAddresses.get(0).getCountryName() != null) {
                        address += listAddresses.get(0).getCountryName() + " ";
                    }
                }
                Log.i("INFO", "Address: " + address);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    */

    private void navigateToCameraPreview() {
        // Create an Intent to start the CameraPreviewActivity
        Intent intent = new Intent(this, ScanBarcodeActivity.class);
        startActivity(intent);
    }
}

