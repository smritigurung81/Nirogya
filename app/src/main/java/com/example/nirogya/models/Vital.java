package com.example.nirogya.models;

public class Vital {
    private String temperature;
    private String oxygen;
    private String heartRate;
    private String timestamp;
    private String soapNotes;

    private String type;     // e.g. "Oxygen", "Blood_Pressure"
    private String value;    // e.g. "120/80" or "98%"
    private String note;     // optional

    public Vital() {
        // Required for Firestore
    }

    // For simple display-type vitals like ("Oxygen", "98%", "timestamp")
    public Vital(String type, String value, String timestamp) {
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
    }

    // For SOAP note vitals: ("Doctor_Notes", "Feeling tired", "timestamp", "note")
    public Vital(String type, String value, String timestamp, String note) {
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
        this.note = note;
    }

    // Full constructor for manual vitals
    public Vital(String temperature, String oxygen, String heartRate, String timestamp, String soapNotes) {
        this.temperature = temperature;
        this.oxygen = oxygen;
        this.heartRate = heartRate;
        this.timestamp = timestamp;
        this.soapNotes = soapNotes;
    }

    // Manual vitals getters
    public String getTemperature() {
        return temperature;
    }

    public String getOxygen() {
        return oxygen;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSoapNotes() {
        return soapNotes;
    }

    // For DoctorVitalsService-style entries
    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getNote() {
        return note;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public void setOxygen(String oxygen) {
        this.oxygen = oxygen;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setSoapNotes(String soapNotes) {
        this.soapNotes = soapNotes;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
