package com.cs407.attendanceapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cs407.attendanceapp2.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

public class LoginPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_login);
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        EditText emailEditText = findViewById(R.id.emailEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);

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
        EditText emailTextField = (EditText) findViewById(R.id.emailEditText);
        String email = emailTextField.getText().toString();
        EditText passwordTextField = (EditText) findViewById(R.id.passwordEditText);
        String password = passwordTextField.getText().toString();

        if(email == null || password == null) {
            Log.e("Login Error", "Invalid username or password");
            displayIncorrectLoginMessage();
        } else {
            checkLogin(email, password);
        }
    }
    public void checkLogin(String email, String password) {
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String passwordValue = document.getString("password");
                            String userType = document.getString("user_type");
                            if (passwordValue.equals(password)) {
                                Log.i("Info", "Made it here");
                                goToHomePage(userType);
                            } else {
                                Log.i("Info", "Invalid email or password");
                                displayIncorrectLoginMessage();
                            }
                        } else {
                            Log.d("Firestore Data", "No such document");
                            displayIncorrectLoginMessage(); // email wasn't found?
                        }
                    } else {
                        Log.e("Firestore Error", "Error getting document", task.getException());
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
        TextView incorrectLoginText = findViewById(R.id.incorrectLoginText);
        incorrectLoginText.setText("Invalid email or password");
    }

    public void removeIncorrectLoginMessage() {
        TextView incorrectLoginText = findViewById(R.id.incorrectLoginText);
        incorrectLoginText.setText("");
    }
}