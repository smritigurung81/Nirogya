package com.example.nirogya.models;

import java.util.Date;

public class MedicalHistory {
    private String disease;
    private String duration;
    private String remarks;
    private Date date;

    public MedicalHistory() {
        // Required for Firestore deserialization
    }

    public MedicalHistory(String disease, String duration, String remarks, Date date) {
        this.disease = disease;
        this.duration = duration;
        this.remarks = remarks;
        this.date = date;
    }

    public String getDisease() {
        return disease;
    }

    public String getDuration() {
        return duration;
    }

    public String getRemarks() {
        return remarks;
    }

    public Date getDate() {
        return date;
    }
}
