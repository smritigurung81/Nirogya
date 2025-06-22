package com.example.nirogya;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class DoctorVitalsService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Correctly query flat /vitals collection using patientId field
    public void getVitalsForPatient(String patientId, OnCompleteListener listener) {
        db.collection("vitals")
                .whereEqualTo("patientId", patientId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }
}
