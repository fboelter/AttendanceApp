package com.cs407.attendanceapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

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
    private Button addCourseStudentButton;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_page);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);
        addCourseStudentButton = findViewById(R.id.addCourseStudentButton);

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
                            String classDocumentId = classDocument.getId();
                            String className = classDocument.getString("course_name");
                            List<String> daysOfWeek = (List<String>) classDocument.get("days_of_week");
                            Timestamp timeStart = classDocument.getTimestamp("time_start");
                            Timestamp timeEnd = classDocument.getTimestamp("time_end");
                            String startTime = formatTime(timeStart);
                            String endTime = formatTime(timeEnd);
                            String timeRange = startTime + " - " + endTime;
                            // Check if the user is enrolled in this class
                            List<String> studentEmails = (List<String>) classDocument.get("student_emails");
                            if (studentEmails != null && studentEmails.contains(userEmail)) {
                                classListAll.add(new Course(className, timeRange, classDocumentId));
                                adapter_all.notifyDataSetChanged();
                            }

                            // Check if the class is scheduled for the current day
                            if (isCourseScheduledToday(currentDate, daysOfWeek, timeStart, timeEnd)) {
                                if (studentEmails != null && studentEmails.contains(userEmail)) {
                                    classList.add(new Course(className, timeRange, classDocumentId));
                                    adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
                                }
                            }
                        }
                    } else {
                        // Handle errors
                        Log.e("FirestoreQuery", "Error getting documents: " + task.getException());
                    }
                }
            });

            addCourseStudentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("INFO", "Add course button clicked");
                    // Intent intent = new Intent(StudentHomePage.this, ScanBarcodeActivity.class);
                    // startActivity(intent);
                    requestCameraPermission();

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

    private void requestCameraPermission() {
        Log.i("INFO", "Requesting camera permission");
        // Check if the CAMERA permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
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

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed with camera-related operations
                Log.i("INFO", "Permission request granted after request, starting camera");
                scanBarcode();
            } else {
                // Camera permission denied. You may want to show a message or take alternative actions.
                Toast.makeText(this,
                        "Permission request denied",
                        Toast.LENGTH_SHORT).show();
            }
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
                            classesRef.document(classId).update("student_emails", FieldValue.arrayUnion(currentUser.getEmail()));
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
}
