package com.example.nirogya;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AppointmentService {

    // Return Task<DocumentReference> to allow caller to add listeners
    public static Task<DocumentReference> bookAppointment(String patientId, String doctorId, String dateTime) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("patientId", patientId);
        appointment.put("doctorId", doctorId);
        appointment.put("dateTime", dateTime);
        appointment.put("status", "pending");
        appointment.put("timestamp", System.currentTimeMillis());

        return db.collection("appointments")
                .add(appointment);
    }
}

