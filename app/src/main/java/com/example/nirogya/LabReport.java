package com.example.nirogya;

public class LabReport {
    private String imageUrl;
    private String title;
    private String date;

    // No-arg constructor (required for Firestore)
    public LabReport() {
    }

    public LabReport(String imageUrl, String title, String date) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.date = date;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

