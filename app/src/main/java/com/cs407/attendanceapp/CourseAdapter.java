package com.cs407.attendanceapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cs407.attendanceapp2.R;

import java.util.List;

public class CourseAdapter extends ArrayAdapter<Course> {

    private OnCourseClickListener listener;
    List<Course> classList;
    View classesTodayConvertView;
    ViewGroup classesTodayParent;

    public CourseAdapter(Context context, List<Course> classList, OnCourseClickListener clickListener) {
        super(context, 0, classList);
        this.listener = clickListener;
        this.classList = classList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_course, parent, false);
        }

        TextView classNameTextView = convertView.findViewById(R.id.class_name);
        TextView timeRange = convertView.findViewById(R.id.time_range);
        Course classModel = getItem(position);

        if (classModel != null) {
            classNameTextView.setText(classModel.getCourseName());
            timeRange.setText(classModel.getTimeRange());

            ImageButton attendanceButton = convertView.findViewById(R.id.attendanceButton);
            Log.i("INFO", classModel.getCourseName() + " happening now? " + classModel.isClassHappeningNow());
            Log.i("INFO", classModel.getCourseName() + " today? " + classModel.isCourseScheduledToday());
            Log.i("INFO", "parentToString() " + parent.getResources().getResourceName(parent.getId()));
            String parentViewResourceName = parent.getResources().getResourceName(parent.getId());
            if (classModel.isClassHappeningNow() && classModel.isCourseScheduledToday() && !parentViewResourceName.contains("all"))
            {
                Log.i("INFO", "Setting " + classModel.getCourseName() + " button to visible");
                convertView.findViewById(R.id.attendanceButton).setVisibility(View.VISIBLE);
                attendanceButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i("INFO", "Button clicked!");

                        // Call the interface method when the item is clicked
                        if (listener != null) {
                            listener.onCourseClick(getItem(position));
                        }
                    }
                });
                this.classesTodayConvertView = convertView;
                this.classesTodayParent = parent;
            } else {
                attendanceButton.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    public void changeAttendanceButtonToCheckMark(Course course) {
        int position = classList.indexOf(course);
        if (position != -1) {
            if (this.classesTodayConvertView != null) {
                View view = getView(position, this.classesTodayConvertView, this.classesTodayParent);  // Get the view for the specified position

                if (view != null) {
                    // Update the attendanceButton with the new icon
                    ImageButton attendanceButton = view.findViewById(R.id.attendanceButton);
                    int tint = Color.parseColor("#363537");
                    attendanceButton.setBackgroundTintList(ColorStateList.valueOf(tint));
                    attendanceButton.setImageResource(R.drawable.checkmark);
                }
            }
        }
    }

    public void changeAttendanceButtonToAttendance(Course course) {
        int position = classList.indexOf(course);
        if (position != -1) {
            if (this.classesTodayConvertView != null) {
                View view = getView(position, this.classesTodayConvertView, this.classesTodayParent);  // Get the view for the specified position

                if (view != null) {
                    // Update the attendanceButton with the new icon
                    ImageButton attendanceButton = view.findViewById(R.id.attendanceButton);
                    int tint = Color.parseColor("#A61A1A");
                    attendanceButton.setBackgroundTintList(ColorStateList.valueOf(tint));
                    attendanceButton.setImageResource(android.R.drawable.ic_menu_my_calendar);
                }
            }
        }
    }

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }
}