package com.example.nirogya.services;

import android.content.Context;

import com.example.nirogya.models.Vital;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VitalsService {
    private Context context;
    private FirebaseFirestore db;

    public VitalsService(Context context, FirebaseFirestore db) {
        this.context = context;
        this.db = db;
    }

    public interface OnVitalsFetchedListener {
        void onVitalsFetched(List<Vital> vitals);
        void onError(String errorMessage);
    }

    public void fetchVitalsForPatient(String patientId, OnVitalsFetchedListener listener) {
        db.collection("vitals")
                .whereEqualTo("patientId", patientId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Vital> vitals = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Vital vital = document.toObject(Vital.class);
                                vitals.add(vital);
                            } catch (Exception e) {
                                // Handle deserialization error gracefully
                            }
                        }
                        listener.onVitalsFetched(vitals);
                    } else {
                        listener.onError("Failed to fetch vitals: " + task.getException().getMessage());
                    }
                });
    }
}
