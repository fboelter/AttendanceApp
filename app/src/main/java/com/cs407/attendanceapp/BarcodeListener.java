package com.cs407.attendanceapp;

public interface BarcodeListener {
    void onBarcodeScanned(String rawValue);
}
