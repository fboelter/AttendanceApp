package com.cs407.attendanceapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cs407.attendanceapp2.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

public class LoginPage extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView incorrectLoginText;
    private Button loginButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_login);
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        incorrectLoginText = findViewById(R.id.incorrectLoginText);
        loginButton = findViewById(R.id.loginButton);

        emailEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeIncorrectLoginMessage();
            }
        });
        passwordEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeIncorrectLoginMessage();
            }
        });

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
    public void loginClick(View view) {
        // Disable login button while login process is running
        loginButton.setEnabled(false);

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if(email.isEmpty() || password.isEmpty()) {
            Log.e("Login Error", "Invalid username or password");
            displayIncorrectLoginMessage();
            Toast.makeText(LoginPage.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
        } else {
            validateLoginWithFirebaseAuth(email, password);
        }
    }

    public void validateLoginWithFirebaseAuth(String email, String password) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() != null) {
                            fetchUserTypeFromFirestore(mAuth.getCurrentUser().getUid());
                        } else {
                            Log.e("Login", "Current user is null after successful login");
                            displayIncorrectLoginMessage();
                        }
                    } else {
                        Log.e("Login", "Error signing in", task.getException());
                        displayIncorrectLoginMessage();
                    }
                    // Enable Login button when process has completed
                    loginButton.setEnabled(true);
                });
    }

    public void fetchUserTypeFromFirestore(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userType = document.getString("user_type");
                            goToHomePage(userType);
                        } else {
                            Log.e("Firestore Data", "No such document");
                            displayIncorrectLoginMessage();
                            Toast.makeText(LoginPage.this, "No account associated with this email.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Firestore Error", "Error getting document", task.getException());
                        displayIncorrectLoginMessage();
                        Toast.makeText(LoginPage.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void goToHomePage(String userType) {
        if (userType.equals("student")) {
            Intent intent = new Intent(this, StudentHomePage.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ProfessorHomePage.class);
            startActivity(intent);
        }
    }
    public void signUpClick(View view) {
        Intent intent = new Intent(this, SignUpPage.class);
        startActivity(intent);
    }

    public void displayIncorrectLoginMessage() {
        incorrectLoginText.setText("Invalid email or password");
    }

    public void removeIncorrectLoginMessage() {
        incorrectLoginText.setText("");
    }
}