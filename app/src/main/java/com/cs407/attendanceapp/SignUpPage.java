package com.cs407.attendanceapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_page);
        mAuth = FirebaseAuth.getInstance();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpPage.this, LoginPage.class);
                startActivity(intent);
            }
        });
    }

    public void swapUserType(View view) {
        CheckBox studentCheck = findViewById(R.id.studentCheck);
        CheckBox professorCheck = findViewById(R.id.professorCheck);

        if(view.getId() == R.id.studentCheck) {
            professorCheck.setChecked(false);
        } else if(view.getId() == R.id.professorCheck) {
            studentCheck.setChecked(false);
        }
    }


    public void finishSignUpClick(View view) {
        EditText emailTextField = findViewById(R.id.email);
        EditText firstNameTextField = findViewById(R.id.firstName);
        EditText lastNameTextField = findViewById(R.id.lastName);
        EditText passwordTextField = findViewById(R.id.password);
        CheckBox student = findViewById(R.id.studentCheck);
        CheckBox professor = findViewById(R.id.professorCheck);

        String email = emailTextField.getText().toString();
        String firstName = firstNameTextField.getText().toString();
        String lastName = lastNameTextField.getText().toString();
        String password = passwordTextField.getText().toString();
        String userType = "";

        if ((student.isChecked() && professor.isChecked()) || (!student.isChecked() && !professor.isChecked())) {
            Toast.makeText(this, "Please check either student or professor", Toast.LENGTH_SHORT).show();
        } else if (student.isChecked()) {
            userType = "student";
        } else {
            userType = "professor";
        }

        if(email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        } else {
            registerUser(email, password, firstName, lastName, userType);
        }
    }

    private void registerUser(String email, String password, String firstName, String lastName, String userType) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User registration successful, store additional user data in Firestore
                        writeUser(email, firstName, lastName, userType);
                    } else {
                        // Registration failed
                        Toast.makeText(SignUpPage.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void writeUser(String email, String firstName, String lastName, String userType) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("first_name", firstName);
        data.put("last_name", lastName);
        data.put("user_type", userType);
        data.put("email", email);

        db.collection("users").document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> goToHomePage(userType))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding document", e));
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