package com.example.nirogya.services;

import android.content.Context;
import com.example.nirogya.models.Appointment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class AppointmentService {
    private Context context;
    private FirebaseFirestore db;
    private String doctorId;

    public AppointmentService(Context context, FirebaseFirestore db, String doctorId) {
        this.context = context;
        this.db = db;
        this.doctorId = doctorId;
    }

    public void fetchAppointmentsForDoctor(OnSuccessListener<List<Appointment>> onSuccess, OnFailureListener onFailure) {
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Appointment> appointments = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Appointment appointment = doc.toObject(Appointment.class);
                            appointments.add(appointment);
                        } catch (Exception ignored) {}
                    }
                    onSuccess.onSuccess(appointments);
                })
                .addOnFailureListener(onFailure);
    }
}
