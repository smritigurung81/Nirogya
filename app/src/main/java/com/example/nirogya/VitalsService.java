package com.example.nirogya;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class VitalsService {

    public static void uploadVitals(double heartRate, double temperature, int oxygen) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("VitalsService", "No authenticated user");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("heartRate", heartRate);
        data.put("temperature", temperature);
        data.put("oxygenLevel", oxygen);
        data.put("timestamp", FieldValue.serverTimestamp()); // Use server timestamp
        data.put("patientId", userId);

        db.collection("vitals")
                .add(data)
                .addOnSuccessListener(documentReference -> Log.d("VitalsService", "Uploaded successfully"))
                .addOnFailureListener(e -> Log.e("VitalsService", "Upload failed", e));
    }
}
