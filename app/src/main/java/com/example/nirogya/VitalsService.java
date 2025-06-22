package com.example.nirogya;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VitalsService {
    public static void uploadVitals(double heartRate, double temperature, int oxygen) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("heartRate", heartRate);
        data.put("temperature", temperature);
        data.put("oxygenLevel", oxygen); // Changed from "oxygen" to "oxygenLevel"
        data.put("timestamp", System.currentTimeMillis()); // Changed to match PatientDashboard
        data.put("patientId", userId); // Added patientId field

        db.collection("vitals") // Changed to flat structure
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Log.d("Vitals", "Uploaded successfully"))
                .addOnFailureListener(e ->
                        Log.e("Vitals", "Upload failed", e));
    }
}