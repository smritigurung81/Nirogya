package com.example.nirogya;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class AlertService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Query alerts for doctorId
    public void getAlertsForDoctor(String doctorId, OnCompleteListener listener) {
        db.collection("alerts")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Mark alert as seen by doctor
    public void markAlertAsSeen(String alertId, OnCompleteListener listener) {
        DocumentReference docRef = db.collection("alerts").document(alertId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("seenByDoctor", true);

        docRef.update(updates).addOnCompleteListener(listener);
    }
}
