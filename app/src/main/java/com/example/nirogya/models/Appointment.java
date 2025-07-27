package com.example.nirogya.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Appointment {
    private String id;
    private String appointmentId; // Additional field for document ID
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String patientName;
    private String date;
    private String time;
    private String status; // pending, accepted, declined
    private Timestamp timestamp;

    public Appointment() {
        // Needed for Firestore deserialization
    }

    // Constructor with all essential fields
    public Appointment(String patientId, String doctorId, String patientName, String doctorName,
                       String date, String time, String status, Timestamp timestamp) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    @PropertyName("id")
    public String getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(String id) {
        this.id = id;
    }

    // Additional field for document ID (used by adapter)
    public String getAppointmentId() {
        return appointmentId != null ? appointmentId : id;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    @PropertyName("doctorId")
    public String getDoctorId() {
        return doctorId;
    }

    @PropertyName("doctorId")
    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    @PropertyName("doctorName")
    public String getDoctorName() {
        return doctorName;
    }

    @PropertyName("doctorName")
    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    @PropertyName("patientId")
    public String getPatientId() {
        return patientId;
    }

    @PropertyName("patientId")
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    @PropertyName("patientName")
    public String getPatientName() {
        return patientName;
    }

    @PropertyName("patientName")
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    @PropertyName("date")
    public String getDate() {
        return date;
    }

    @PropertyName("date")
    public void setDate(String date) {
        this.date = date;
    }

    @PropertyName("time")
    public String getTime() {
        return time;
    }

    @PropertyName("time")
    public void setTime(String time) {
        this.time = time;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    // Helper methods for status checking
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isAccepted() {
        return "accepted".equalsIgnoreCase(status);
    }

    public boolean isDeclined() {
        return "declined".equalsIgnoreCase(status);
    }

    // Get formatted date and time
    public String getFormattedDateTime() {
        if (date != null && time != null) {
            return date + " at " + time;
        } else if (date != null) {
            return date;
        } else if (time != null) {
            return time;
        } else {
            return "No date/time set";
        }
    }

    // Check if appointment is today (you'll need to implement DateHelper if not available)
    public boolean isToday() {
        if (timestamp == null) return false;

        // Simple check - you might want to use DateHelper.getStartOfTodayMillis() etc.
        long appointmentTime = timestamp.toDate().getTime();
        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000;

        return Math.abs(currentTime - appointmentTime) < oneDayInMillis;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id='" + id + '\'' +
                ", appointmentId='" + appointmentId + '\'' +
                ", doctorId='" + doctorId + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", patientId='" + patientId + '\'' +
                ", patientName='" + patientName + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Appointment that = (Appointment) obj;

        String thisId = getAppointmentId();
        String thatId = that.getAppointmentId();

        return thisId != null ? thisId.equals(thatId) : thatId == null;
    }

    @Override
    public int hashCode() {
        String appointmentId = getAppointmentId();
        return appointmentId != null ? appointmentId.hashCode() : 0;
    }
}