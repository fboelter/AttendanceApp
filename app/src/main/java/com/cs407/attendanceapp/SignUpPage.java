package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.cs407.attendanceapp2.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class SignUpPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_page);
    }

    public void finishSignUpClick(View view) {
        EditText emailTextField = (EditText) findViewById(R.id.email);
        EditText firstNameTextField = (EditText) findViewById(R.id.firstName);
        EditText lastNameTextField = (EditText) findViewById(R.id.lastName);
        EditText passwordTextField = (EditText) findViewById(R.id.password);
        CheckBox student = (CheckBox) findViewById(R.id.studentCheck);
        CheckBox professor = (CheckBox) findViewById(R.id.professorCheck);

        String email = emailTextField.getText().toString();
        String firstName = firstNameTextField.getText().toString();
        String lastName = lastNameTextField.getText().toString();
        String password = passwordTextField.getText().toString();
        String userType = "";

        if ((student.isChecked() && professor.isChecked()) || (!student.isChecked() && !professor.isChecked())) {
            Log.e("Sign Up Error", "Please check either student or professor");
        } else if (student.isChecked()) {
            userType = "student";
        } else {
            userType = "professor";
        }

        if(email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Log.e("Login Error", "Invalid credentials");
        } else {
            checkIfEmailExists(email, firstName, lastName, password, userType);
        }
    }

    public void writeUser(String email, String firstName, String lastName, String password, String userType ){

        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentRef = db.collection("users")
                .document(email);

        // Create a map with the updates you want to apply
        Map<String, Object> data = new HashMap<>();

        data.put("first_name", firstName);
        data.put("last_name", lastName);
        data.put("password", password);
        data.put("user_type", userType);
        data.put("username", email);

        // Use the document reference to update the document
        documentRef.set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        goToHomePage(userType);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle errors here
                        Log.e("Firestore", "Error updating document", e);
                    }
                });
    }

    public void checkIfEmailExists(String email, String firstName, String lastName, String password, String userType) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.e("Signup Error", "Email already taken");
                            Toast.makeText(SignUpPage.this, "Email already associated with an account.", Toast.LENGTH_SHORT).show();
                        } else {
                            writeUser(email, firstName, lastName, password, userType);
                        }
                    } else {
                        Log.e("Firestore Error", "Error checking for email", task.getException());
                    }
                });
    }

    public void goToHomePage(String userType){
        if (userType.equals("student")) {
            Intent intent = new Intent(this, StudentHomePage.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ProfessorHomePage.class);
            startActivity(intent);
        }
    }
}