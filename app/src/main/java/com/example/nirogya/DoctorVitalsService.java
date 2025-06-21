package com.example.nirogya;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class DoctorVitalsService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Query vitals for a patient sorted by timestamp descending
    public void getVitalsForPatient(String patientId, OnCompleteListener listener) {
        db.collection("vitals")
                .document(patientId)
                .collection("entries")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }
}
