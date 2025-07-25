package com.example.nirogya.models;

public class User {
    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String linkedDoctorNmc;
    private String linkedDoctorId;
    private String nmcNumber; // For doctors

    public User() {
        // Required for Firestore
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLinkedDoctorNmc() {
        return linkedDoctorNmc;
    }

    public void setLinkedDoctorNmc(String linkedDoctorNmc) {
        this.linkedDoctorNmc = linkedDoctorNmc;
    }

    public String getLinkedDoctorId() {
        return linkedDoctorId;
    }

    public void setLinkedDoctorId(String linkedDoctorId) {
        this.linkedDoctorId = linkedDoctorId;
    }

    public String getNmcNumber() {
        return nmcNumber;
    }

    public void setNmcNumber(String nmcNumber) {
        this.nmcNumber = nmcNumber;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
