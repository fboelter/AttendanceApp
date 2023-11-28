package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.BarcodeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseDetails extends AppCompatActivity {

    TextView textViewCourseName;
    TextView textViewClassDays;
    TextView textViewClassStart;
    TextView textViewClassEnd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_details);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        String classId = getIntent().getStringExtra("classId");

        // Initialize TextViews + ImageView
        textViewCourseName = findViewById(R.id.textView21);
        textViewClassDays = findViewById(R.id.classDaysButton);
        textViewClassStart = findViewById(R.id.classStartText);
        textViewClassEnd = findViewById(R.id.classEndText);
        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);


        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(classId, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (classId != null) {
            getClassDetails(classId);
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
                    // You can also navigate the user back to the login screen or perform other actions as needed.
                    finish();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void getClassDetails(String classId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("Classes").document(classId);

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    textViewCourseName.setText(documentSnapshot.getString("course_name"));

                    List<String> days = (List<String>) documentSnapshot.get("days_of_week");
                    textViewClassDays.setText("Meets On: " + TextUtils.join(", ", days));

                    Date startDate = documentSnapshot.getDate("time_start");
                    Date endDate = documentSnapshot.getDate("time_end");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

                    textViewClassStart.setText("Class Start Date: " + (startDate != null ? dateFormat.format(startDate) : "N/A"));
                    textViewClassEnd.setText("Class End Date: " + (endDate != null ? dateFormat.format(endDate) : "N/A"));
                } else {
                    // Document does not exist
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CourseDetails.this, "Error fetching class details", Toast.LENGTH_LONG).show();
                Log.e("FireStoreQuery Error: ",e.getMessage());
            }
        });
    }

}