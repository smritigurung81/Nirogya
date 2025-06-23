package com.example.nirogya.services;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MedicalHistoryService {
    private final Context context;
    private final FirebaseFirestore db;
    private final String uid;

    public MedicalHistoryService(Context context, FirebaseFirestore db, String uid) {
        this.context = context;
        this.db = db;
        this.uid = uid;
    }

    public void addMedicalHistory(String disease, String duration, String remarks) {
        Map<String, Object> history = new HashMap<>();
        history.put("disease", disease);
        history.put("duration", duration);
        history.put("remarks", remarks);
        history.put("date", new Date());

        db.collection("users").document(uid)
                .collection("medical_history")
                .add(history)
                .addOnSuccessListener(doc -> Toast.makeText(context, "Medical history added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
