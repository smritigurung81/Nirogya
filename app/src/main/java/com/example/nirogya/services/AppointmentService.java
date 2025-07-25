package com.example.nirogya.services;

import android.util.Log;
import com.example.nirogya.DateHelper;
import com.example.nirogya.adapters.AppointmentAdapter;
import com.example.nirogya.models.Appointment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AppointmentService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void fetchAppointmentsForDoctor(String doctorId, String status, AppointmentAdapter adapter) {
        Log.d("AppointmentService", "Fetching " + status + " appointments for UID: " + doctorId);

        Query query;

        if (status.equals("today")) {
            long startOfDay = DateHelper.getStartOfTodayMillis();
            long endOfDay = DateHelper.getEndOfTodayMillis();

            query = db.collection("appointments")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("status", "accepted")
                    .whereGreaterThanOrEqualTo("timestamp", new Timestamp(new java.util.Date(startOfDay)))
                    .whereLessThanOrEqualTo("timestamp", new Timestamp(new java.util.Date(endOfDay)));
        } else {
            query = db.collection("appointments")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("status", status);
        }

        query.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> list = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Appointment appointment = doc.toObject(Appointment.class);
                        if (appointment != null) {
                            list.add(appointment);
                            Log.d("AppointmentService", "Loaded: " + appointment.getPatientName() + ", status=" + appointment.getStatus());
                        }
                    }

                    Log.d("AppointmentService", "Final list size for status " + status + ": " + list.size());
                    adapter.updateList(list);
                })
                .addOnFailureListener(e -> {
                    Log.e("AppointmentService", "Failed to load appointments for status " + status + ": " + e.getMessage());
                });
    }
}
