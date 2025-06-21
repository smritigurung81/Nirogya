package com.example.nirogya;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class DoctorAppointmentService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Query appointments where doctorId == currentDoctorId
    public void getAppointmentsForDoctor(String doctorId, OnCompleteListener listener) {
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Update appointment status (approved, declined)
    public void updateAppointmentStatus(String appointmentId, String status, OnCompleteListener listener) {
        DocumentReference docRef = db.collection("appointments").document(appointmentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        docRef.update(updates).addOnCompleteListener(listener);
    }
}
