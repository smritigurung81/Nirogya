package com.example.nirogya;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
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
        data.put("oxygen", oxygen);
        data.put("timestamp", FieldValue.serverTimestamp());


        db.collection("vitals")
                .document(userId)
                .collection("entries")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Log.d("Vitals", "Uploaded successfully"))
                .addOnFailureListener(e ->
                        Log.e("Vitals", "Upload failed", e));
    }
}
