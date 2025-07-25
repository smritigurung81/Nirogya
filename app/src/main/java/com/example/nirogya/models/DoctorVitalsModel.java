package com.example.nirogya.models;

import java.util.Map;

public class DoctorVitalsModel {
    private Map<String, String> vitals;
    private Map<String, String> soap;
    private com.google.firebase.Timestamp timestamp;

    public DoctorVitalsModel() {
        // Required empty constructor for Firestore
    }

    public Map<String, String> getVitals() {
        return vitals;
    }

    public void setVitals(Map<String, String> vitals) {
        this.vitals = vitals;
    }

    public Map<String, String> getSoap() {
        return soap;
    }

    public void setSoap(Map<String, String> soap) {
        this.soap = soap;
    }

    public com.google.firebase.Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(com.google.firebase.Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
