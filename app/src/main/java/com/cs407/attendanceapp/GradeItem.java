package com.cs407.attendanceapp;

public class GradeItem {
    private String studentId;
    private double grade;

    // Constructor
    public GradeItem(String studentId, double grade) {
        this.studentId = studentId;
        this.grade = grade;
    }

    // Getter and setter for studentId
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    // Getter and setter for grade
    public double getGrade() {
        return grade;
    }

    public void setGrade(double grade) {
        this.grade = grade;
    }
}
