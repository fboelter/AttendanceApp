package com.cs407.attendanceapp;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
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

import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentHomePage extends AppCompatActivity implements CourseAdapter.OnCourseClickListener {

    private FirebaseAuth mAuth;
    private ListView listView;
    private ListView listViewAll;
    private List<Course> classList;
    private List<Course> classListAll;
    private CourseAdapter adapter;
    private CourseAdapter adapter_all;
    private ImageButton addCourseStudentButton;
    private static final String[] REQUIRED_PERMISSIONS = {permission.CAMERA};
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 101;

    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample);

        initializeUIComponents();
        initializeListView();
        setupAddCourseButtonListener();
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
            requestCameraPermission();
        });
    }

    private void setupListViewItemClickListener() {
        listViewAll.setOnItemClickListener((parent, view, position, id) -> {
            Course selectedCourse = classListAll.get(position);
            Intent intent = new Intent(StudentHomePage.this, gradebookPage.class);
            intent.putExtra("classDocumentId", selectedCourse.getId());
            startActivity(intent);
        });
    }

    private void fetchAndDisplayCourses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail().toLowerCase();
            Date currentDate = Calendar.getInstance().getTime();
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
                            Timestamp timeStart = classDocument.getTimestamp("time_start");
                            Timestamp timeEnd = classDocument.getTimestamp("time_end");
                            String startTime = formatTime(timeStart);
                            String endTime = formatTime(timeEnd);
                            Date startDate = formatDate(timeStart);
                            Date endDate = formatDate(timeEnd);
                            String timeRange = startTime + " - " + endTime;

                            List<String> studentEmails = null;
                            Object studentEmailsObject = classDocument.get("student_emails");
                            if (classDocument.exists() && className != null && studentEmailsObject != null)
                            {
                                try {
                                    Log.i("INFO", "Attempting to cast " + className + " studentEmailsObject to List<String>");
                                    studentEmails = (List<String>) studentEmailsObject;
                                    Log.i("INFO", "Cast of " + className + "studentEmailsObject to List<String>: " + studentEmails);
                                } catch (Exception e){
                                    Log.e("ERROR", "Error with studentEmails: " + e.getMessage());
                                    studentEmails = new ArrayList<String>();
                                    studentEmails.add(userEmail);
                                }
                            }

                            // Check if the user is enrolled in this class
                            if (studentEmails != null && studentEmails.contains(userEmail)) {
                                Course course = new Course(className, timeRange, classDocumentId, daysOfWeek, startDate, endDate);
                                classListAll.add(course);
                                adapter_all.notifyDataSetChanged();

                                // Check if the class is scheduled for the current day
                                if (course.isCourseScheduledToday()) {
                                    if (studentEmails != null && studentEmails.contains(userEmail)) {
                                        classList.add(new Course(className, timeRange, classDocumentId, daysOfWeek, startDate, endDate));
                                        adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
                                    }
                                }
                            }
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

    private void requestCameraPermission() {
        Log.i("INFO", "Requesting camera permission");
        // Check if the CAMERA permission has been granted
        if (ContextCompat.checkSelfPermission(this, permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("INFO", "Permission granted. Starting camera");
            // Camera permission is already granted, proceed with camera-related operations
            scanBarcode();
        } else {
            // Request CAMERA permission. The result will be received in the onRequestPermissionsResult callback.
            Log.i("INFO", "Requesting permission");
            ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            handleCameraPermissionResult(grantResults);
        } else if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            handleLocationPermissionResult(grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void handleCameraPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Camera permission granted, proceed with camera-related operations
            Log.i("INFO", "Camera permission granted, starting camera");
            scanBarcode(); // Replace with your camera-related operation
        } else {
            // Camera permission denied. You may want to show a message or take alternative actions.
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLocationPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted, proceed with location-related operations
            Log.i("INFO", "Location permission granted, starting location updates");
            startListening(); // Replace with your location-related operation
        } else {
            // Location permission denied. You may want to show a message or take alternative actions.
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanBarcode() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC)
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner
                .startScan()
                .addOnSuccessListener(
                        barcode -> {
                            // Task completed successfully
                            String rawValue = barcode.getRawValue();
                            String classId = rawValue.toString();
                            Log.i("INFO", "Class Id is: " + classId);
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            CollectionReference classesRef = db.collection("Classes");
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            // classesRef.document(classId).update("student_emails", FieldValue.arrayUnion(currentUser.getEmail()));
                            Log.i("INFO", "Update call made to Firebase with " + currentUser.getEmail());
                        })
                .addOnCanceledListener(
                        () -> {
                            Log.i("INFO", "Barcode task cancelled");
                            // Task canceled
                        })
                .addOnFailureListener(
                        e -> {
                            Log.e("ERROR", "Barcode task failed: " + e.getMessage());
                            // Task failed with an exception
                        });

    }

    @Override
    public void onCourseClick(Course course) {
        listenForLocation();
    }

    private void listenForLocation()
    {
        if (Build.VERSION.SDK_INT < 23) {
            startListening();
        } else {
            if (ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) updateLocationInfo(location);
            }
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void updateLocationInfo(Location location) {
        Log.i("LocationInfo", location.toString());

        /*
        TextView latitudeTextView = (TextView) findViewById(R.id.latitudeTextView);
        TextView longitudeTextView = (TextView) findViewById(R.id.longitudeTextView);
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeTextView);
        TextView accuracyTextView = (TextView) findViewById(R.id.accuracyTextView);
         */
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        String accuracy = String.valueOf(location.getAccuracy());
        Log.i("INFO", "Lat: " + latitude + "\tLong: " + longitude + "\t Accuracy: " + accuracy);

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {
            String address = "Could not find address";
            List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (listAddresses != null && listAddresses.size() > 0){
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

            // TextView addressTextView = (TextView) findViewById(R.id.addressTextView);
            Log.i("INFO", "Address: " + address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
