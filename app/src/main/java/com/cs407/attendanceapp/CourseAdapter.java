package com.cs407.attendanceapp;

import android.content.Context;
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

    public CourseAdapter(Context context, List<Course> classList) {
        super(context, 0, classList);
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
            } else {
                attendanceButton.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}