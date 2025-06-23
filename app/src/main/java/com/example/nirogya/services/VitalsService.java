package com.example.nirogya.services;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VitalsService {
    private final Context context;
    private final FirebaseFirestore db;
    private final String uid;

    public VitalsService(Context context, FirebaseFirestore db, String uid) {
        this.context = context;
        this.db = db;
        this.uid = uid;
    }

    public void addVitals(String pressure, String oxygen, String heartRate, String temperature) {
        Map<String, Object> vitals = new HashMap<>();
        vitals.put("pressure", pressure);
        vitals.put("oxygen", oxygen);
        vitals.put("heartRate", heartRate);
        vitals.put("temperature", temperature);
        vitals.put("date", new Date());

        db.collection("users").document(uid)
                .collection("vitals_user")
                .add(vitals)
                .addOnSuccessListener(doc -> Toast.makeText(context, "Vitals added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
