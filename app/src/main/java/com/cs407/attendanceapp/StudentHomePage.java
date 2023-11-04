package com.cs407.attendanceapp;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.widget.Toolbar;

import com.cs407.attendanceapp2.R;

public class StudentHomePage extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_page);
        mAuth = FirebaseAuth.getInstance();
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

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


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_profile) {
//            // Handle the profile icon click event
//            openUserProfile();
//            return true;
//        } else if (item.getItemId() == R.id.action_signout) {
//            // Handle the "Sign Out" action here using Firebase Authentication
//            FirebaseAuth.getInstance().signOut();
//            // You can also navigate the user back to the login screen or perform other actions as needed.
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}