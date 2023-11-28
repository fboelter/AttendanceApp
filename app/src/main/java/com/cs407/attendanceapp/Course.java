package com.cs407.attendanceapp;

public class Course {
    private String courseName;
    private String timeRange;
    private String classDocumentId;

    public Course(String courseName, String timeRange, String classDocumentId) {
        this.courseName = courseName;
        this.timeRange = timeRange;
        this.classDocumentId = classDocumentId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getId() { return classDocumentId; }
}
