package com.example.nirogya;

public class LabReport {
    public String imageUrl;
    public String title;
    public String date;

    public LabReport() {
        // Required empty constructor
    }

    public LabReport(String imageUrl, String title, String date) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.date = date;
    }
}