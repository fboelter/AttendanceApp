package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // PULL FROM FIRESTORE
//        db.collection("Test")
//                .document("Test_Pull")
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        DocumentSnapshot document = task.getResult();
//                        if (document.exists()) {
//                            String variableValue = document.getString("Username");
//                            TextView text = findViewById(R.id.Text);
//                            text.setText(variableValue);
//
//                            // Use the variableValue as needed.
//                            Log.d("Firestore Data", "Value is: " + variableValue);
//                        } else {
//                            Log.d("Firestore Data", "No such document");
//                        }
//                    } else {
//                        Log.e("Firestore Error", "Error getting document", task.getException());
//                    }
//                });



        // WRITE TO FIRESTORE
        // Initialize Firestore
//        DocumentReference documentRef = db.collection("Test")
//                .document("Test_Pull");
//
//        // Create a map with the updates you want to apply
//        Map<String, Object> updates = new HashMap<>();
//        updates.put("Username", "UPDATE SUCCESS!"); // Replace "exampleVariable" with the field you want to update
//
//        // Use the document reference to update the document
//        documentRef.update(updates)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        // Data has been successfully updated in Firestore
//                        // Handle success here
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // Handle errors here
//                        Log.e("Firestore", "Error updating document", e);
//                    }
//                });


    }
}