package com.example.nirogya;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AppointmentService {
    public static void bookAppointment(String patientId, String doctorId, String dateTime) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("patientId", patientId);
        appointment.put("doctorId", doctorId);
        appointment.put("dateTime", dateTime);
        appointment.put("status", "pending");
        appointment.put("timestamp", System.currentTimeMillis());

        db.collection("appointments")
                .add(appointment)
                .addOnSuccessListener(documentReference ->
                        Log.d("Appointment", "Booked successfully"))
                .addOnFailureListener(e ->
                        Log.e("Appointment", "Booking failed", e));
    }
}

