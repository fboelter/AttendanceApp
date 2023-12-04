package com.cs407.attendanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import com.cs407.attendanceapp2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseDetails extends AppCompatActivity {

    TextView textViewCourseName;
    TextView textViewClassDays;
    TextView textViewClassStart;
    TextView textViewClassEnd;
    List<String> days;
    String daysJoined;
    String courseName;
    Date endDate;
    Date startDate;
    String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_details);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(this::showProfilePopupMenu);

        classId = getIntent().getStringExtra("classId");

        // Initialize TextViews + ImageView
        textViewCourseName = findViewById(R.id.textView21);
        textViewClassDays = findViewById(R.id.classDaysButton);
        textViewClassStart = findViewById(R.id.classStartText);
        textViewClassEnd = findViewById(R.id.classEndText);
        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CourseDetails.this, ProfessorHomePage.class);
                startActivity(intent);
            }
        });

        Button seeGradebook = findViewById(R.id.seeGradeBookButton);
        seeGradebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CourseDetails.this, gradebookPage.class);
                intent.putExtra("classDocumentId", classId);
                startActivity(intent);
            }
        });

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

        Button buttonEditCourse = findViewById(R.id.editCourseButton);
        buttonEditCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditCourseDialog(courseName, days, startDate, endDate);
            }
        });

        Button buttonDeleteCourse = findViewById(R.id.deleteCourseButton);
        buttonDeleteCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteCourseDialog(courseName, classId);
            }
        });

        Button saveQrCodeButton = findViewById(R.id.shareQrButton);
        saveQrCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQrCodeToGallery();
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
                    courseName = documentSnapshot.getString("course_name");
                    textViewCourseName.setText(courseName);

                    // Order days correctly
                    List<String> daysOfWeekOrder = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
                    days = (List<String>) documentSnapshot.get("days_of_week");
                    Collections.sort(days, Comparator.comparingInt(daysOfWeekOrder::indexOf));
                    daysJoined = TextUtils.join(", ", days);

                    SpannableString daysSpannable = new SpannableString("Meets On: " + daysJoined);
                    daysSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, "Meets On:".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textViewClassDays.setText(daysSpannable);

                    startDate = documentSnapshot.getDate("time_start");
                    endDate = documentSnapshot.getDate("time_end");

                    // Use SimpleDateFormat to format the dates and times
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

                    // Format the dates and times using your existing methods
                    String startDateFormatted = startDate != null ? getDateWithOrdinal(dateFormat.format(startDate)) : "N/A";
                    String endDateFormatted = endDate != null ? getDateWithOrdinal(dateFormat.format(endDate)) : "N/A";
                    String startTimeFormatted = startDate != null ? timeFormat.format(startDate) : "N/A";
                    String endTimeFormatted = endDate != null ? timeFormat.format(endDate) : "N/A";

                    // Construct the full strings for date range and time range
                    String dateRangeText = "Class Dates: " + startDateFormatted + " - " + endDateFormatted;
                    String timeRangeText = "Class Time: " + startTimeFormatted + " - " + endTimeFormatted;

                    // Apply styling for the bold part
                    SpannableString dateRangeSpannable = new SpannableString(dateRangeText);
                    dateRangeSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, "Class Dates:".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    SpannableString timeRangeSpannable = new SpannableString(timeRangeText);
                    timeRangeSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, "Class Time:".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Set the text to TextViews
                    textViewClassStart.setText(dateRangeSpannable);
                    textViewClassEnd.setText(timeRangeSpannable);
                } else {
                    // Document does not exist
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CourseDetails.this, "Error fetching class details", Toast.LENGTH_LONG).show();
                Log.e("FireStoreQuery Error: ", e.getMessage());
            }
        });
    }

    public void showEditCourseDialog(String courseName, List<String> daysOfWeek, Date startDate, Date endDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogue_add_course, null);
        builder.setView(dialogView);

        // Setting variables to previous data
        EditText courseNameEditText = dialogView.findViewById(R.id.editTextCourseName);
        courseNameEditText.setText(courseName);

        Button buttonStartDate = dialogView.findViewById(R.id.buttonStartDate);
        Button buttonEndDate = dialogView.findViewById(R.id.buttonEndDate);
        Button buttonStartTime = dialogView.findViewById(R.id.buttonStartTime);
        Button buttonEndTime = dialogView.findViewById(R.id.buttonEndTime);

        final Calendar startCalendar = Calendar.getInstance();
        final Calendar endCalendar = Calendar.getInstance();

        // Set the calendars to the start and end dates (which include time)
        if (startDate != null) {
            startCalendar.setTime(startDate);
        }
        if (endDate != null) {
            endCalendar.setTime(endDate);
        }

        // Set onClickListeners for the date buttons
        buttonStartDate.setOnClickListener(v -> showDatePickerDialog(startCalendar, buttonStartDate));
        buttonEndDate.setOnClickListener(v -> showDatePickerDialog(endCalendar, buttonEndDate));

        // Set onClickListeners for the time buttons, using the same start and end calendars
        buttonStartTime.setOnClickListener(v -> showTimePickerDialog(startCalendar, buttonStartTime));
        buttonEndTime.setOnClickListener(v -> showTimePickerDialog(endCalendar, buttonEndTime));

        // Format and set the button texts for dates and times
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        buttonStartDate.setText(startDate != null ? getDateWithOrdinal(dateFormat.format(startDate)) : "Select a Start Date for your class!");
        buttonEndDate.setText(endDate != null ? getDateWithOrdinal(dateFormat.format(endDate)) : "Select an End Date for your class!");
        buttonStartTime.setText(startDate != null ? timeFormat.format(startDate) : "Select Start Time");
        buttonEndTime.setText(endDate != null ? timeFormat.format(endDate) : "Select End Time");

        // Pre-check the CheckBoxes based on the daysOfWeek list
        Map<String, CheckBox> checkBoxMap = new HashMap<>();
        checkBoxMap.put("Monday", (CheckBox) dialogView.findViewById(R.id.checkboxMonday));
        checkBoxMap.put("Tuesday", (CheckBox) dialogView.findViewById(R.id.checkboxTuesday));
        checkBoxMap.put("Wednesday", (CheckBox) dialogView.findViewById(R.id.checkboxWednesday));
        checkBoxMap.put("Thursday", (CheckBox) dialogView.findViewById(R.id.checkboxThursday));
        checkBoxMap.put("Friday", (CheckBox) dialogView.findViewById(R.id.checkboxFriday));

        for (String day : daysOfWeek) {
            CheckBox checkBox = checkBoxMap.get(day);
            if (checkBox != null) {
                checkBox.setChecked(true);
            }
        }

        builder.setPositiveButton("Save New Details", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Get the updated course name
                String updatedCourseName = courseNameEditText.getText().toString();

                // Get the updated days of the week
                ArrayList<String> updatedSelectedDays = new ArrayList<>();
                for (Map.Entry<String, CheckBox> entry : checkBoxMap.entrySet()) {
                    if (entry.getValue().isChecked()) {
                        updatedSelectedDays.add(entry.getKey());
                    }
                }

                // Validate the updated data
                if (updatedCourseName.isEmpty()) {
                    Toast.makeText(CourseDetails.this, "Please enter a course name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (updatedSelectedDays.isEmpty()) {
                    Toast.makeText(CourseDetails.this, "Please select at least one day of the week.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startCalendar.getTime().after(endCalendar.getTime())) {
                    Toast.makeText(CourseDetails.this, "The end date must be after the start date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert Calendar instances to Timestamp for Firestore
                Timestamp timestampStart = new Timestamp(startCalendar.getTime());
                Timestamp timestampEnd = new Timestamp(endCalendar.getTime());

                // Prepare the updated data map
                Map<String, Object> updatedClassData = new HashMap<>();
                updatedClassData.put("course_name", updatedCourseName);
                updatedClassData.put("days_of_week", updatedSelectedDays);
                updatedClassData.put("time_start", timestampStart);
                updatedClassData.put("time_end", timestampEnd);

                // Update Firestore with the new class data
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference classRef = db.collection("Classes").document(classId);

                classRef.update(updatedClassData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CourseDetails.this, "Class updated successfully", Toast.LENGTH_SHORT).show();
                                // Refresh the class details on the screen
                                getClassDetails(classId);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CourseDetails.this, "Failed to update class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showDatePickerDialog(final Calendar calendar, final Button dateButton) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    String dateString = dateFormat.format(calendar.getTime());
                    dateButton.setText(getDateWithOrdinal(dateString));
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog(final Calendar calendar, final Button timeButton) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    String timeString = timeFormat.format(calendar.getTime());
                    timeButton.setText(timeString);
                },
                hour, minute, false
        );
        timePickerDialog.show();
    }

    private String getDateWithOrdinal(String dateString) {
        String[] splitDate = dateString.split(" ");
        int day = Integer.parseInt(splitDate[1].replaceAll(",", ""));
        return splitDate[0] + " " + day + getDayOfMonthSuffix(day) + ", " + splitDate[2];
    }

    private String getDayOfMonthSuffix(final int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }

    private void showDeleteCourseDialog(final String courseName, final String classId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(CourseDetails.this);
        builder.setTitle("Delete Course");
        builder.setMessage("Are you sure you want to delete this course? This action is irreversible. Type in the course name to confirm deletion.");

        final EditText input = new EditText(CourseDetails.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String confirmation = input.getText().toString();
                if (confirmation.equals(courseName)) {
                    deleteCourse(classId);
                } else {
                    Toast.makeText(CourseDetails.this, "Course name does not match. Deletion cancelled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteCourse(String classId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference classRef = db.collection("Classes").document(classId);

        classRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(CourseDetails.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CourseDetails.this, ProfessorHomePage.class);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CourseDetails.this, "Failed to delete course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveQrCodeToGallery() {
        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);
        Bitmap qrCodeBitmap = ((BitmapDrawable) qrCodeImageView.getDrawable()).getBitmap();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "QRCode_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                boolean saved = qrCodeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                if (!saved) {
                    throw new IOException("Failed to save bitmap.");
                }
                Toast.makeText(this, "QR Code saved to Photos!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                if (uri != null) {
                    getContentResolver().delete(uri, null, null);
                }
                e.printStackTrace();
                Toast.makeText(this, "Error saving QR Code", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error saving QR Code", Toast.LENGTH_SHORT).show();
        }
    }
}