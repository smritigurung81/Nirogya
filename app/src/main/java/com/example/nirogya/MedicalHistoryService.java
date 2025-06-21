package com.example.nirogya;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MedicalHistoryService {
    public static void updateHistory(String condition, String medication, String note) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> history = new HashMap<>();
        history.put("condition", condition);
        history.put("medication", medication);
        history.put("note", note);
        history.put("lastUpdated", System.currentTimeMillis());

        db.collection("medicalHistories")
                .document(userId)
                .set(history)
                .addOnSuccessListener(aVoid ->
                        Log.d("History", "Saved successfully"))
                .addOnFailureListener(e ->
                        Log.e("History", "Save failed", e));
    }
}

