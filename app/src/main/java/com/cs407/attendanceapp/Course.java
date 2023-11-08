package com.cs407.attendanceapp;

public class Course {
    private String courseName;
    private String timeRange; // Time range (e.g., "1:00pm - 2:15pm")

    public Course(String courseName, String timeRange) {
        this.courseName = courseName;
        this.timeRange = timeRange;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTimeRange() {
        return timeRange;
    }
}
