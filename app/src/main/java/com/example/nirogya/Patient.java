package com.example.nirogya;

public class Patient {
    private String uid;
    private String name;
    private String linkedDoctorNmc;

    // Default constructor required for Firestore deserialization
    public Patient() {
    }

    // Constructor to quickly create patient objects (optional)
    public Patient(String uid, String name, String linkedDoctorNmc) {
        this.uid = uid;
        this.name = name;
        this.linkedDoctorNmc = linkedDoctorNmc;
    }

    // Getters and setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLinkedDoctorNmc() {
        return linkedDoctorNmc;
    }

    public void setLinkedDoctorNmc(String linkedDoctorNmc) {
        this.linkedDoctorNmc = linkedDoctorNmc;
    }
}



