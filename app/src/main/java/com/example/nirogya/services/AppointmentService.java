package com.example.nirogya.services;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.*;
import java.util.*;

public class AppointmentService {

    private final FirebaseFirestore db;
    private final Context context;
    private final String uid;

    public interface AppointmentCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface AppointmentFetchCallback {
        void onAppointmentsFetched(List<com.example.nirogya.Appointment> list);
        void onError(String errorMessage);
    }

    public AppointmentService(Context context, FirebaseFirestore db, String uid) {
        this.context = context;
        this.db = db;
        this.uid = uid;
    }

    // For Patients: Book appointment
    public void bookAppointment(String doctorId, String date, String time, AppointmentCallback callback) {
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("patientId", uid);
        appointment.put("doctorId", doctorId);
        appointment.put("date", date);
        appointment.put("time", time);
        appointment.put("status", "pending");

        db.collection("appointments")
                .add(appointment)
                .addOnSuccessListener(r -> {
                    Toast.makeText(context, "Appointment requested", Toast.LENGTH_SHORT).show();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onFailure(e.getMessage());
                });
    }

    // For Doctors: Fetch pending appointments
    public void fetchDoctorAppointments(String doctorId, AppointmentFetchCallback callback) {
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        callback.onError("Failed to fetch appointments");
                        return;
                    }

                    List<com.example.nirogya.Appointment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        com.example.nirogya.Appointment appointment = doc.toObject(com.example.nirogya.Appointment.class);
                        appointment.setId(doc.getId());
                        list.add(appointment);
                    }
                    callback.onAppointmentsFetched(list);
                });
    }

    // For Doctors: Update appointment status
    public void updateAppointmentStatus(String appointmentId, boolean isAccepted, AppointmentCallback callback) {
        String newStatus = isAccepted ? "accepted" : "declined";

        db.collection("appointments")
                .document(appointmentId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Appointment " + newStatus, Toast.LENGTH_SHORT).show();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onFailure(e.getMessage());
                });
    }
}
