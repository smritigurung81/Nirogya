package com.example.nirogya;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public class DoctorMedicalHistoryService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Read medical history for a given patientId
    public void getMedicalHistory(String patientId, OnCompleteListener listener) {
        DocumentReference docRef = db.collection("medicalHistories").document(patientId);
        docRef.get().addOnCompleteListener(listener);
    }
}

