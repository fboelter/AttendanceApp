package com.cs407.attendanceapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cs407.attendanceapp2.R;

import java.util.List;
import java.util.Locale;

public class GradesAdapter extends ArrayAdapter<GradeItem> {

    public GradesAdapter(Context context, List<GradeItem> gradeItems) {
        super(context, 0, gradeItems);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_grade, parent, false);
        }

        TextView tvStudentId = convertView.findViewById(R.id.tvStudentId);
        TextView tvGrade = convertView.findViewById(R.id.tvGrade);

        GradeItem item = getItem(position);

        if (item != null) {
            tvStudentId.setText(item.getStudentId());
            tvGrade.setText(String.format(Locale.getDefault(), "%.2f%%", item.getGrade()));
        }

        return convertView;
    }
}

