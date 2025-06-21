package com.example.nirogya;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class DoctorPatientService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Query patients where role == "patient" and linkedDoctorId == currentDoctorId
    public void getAssignedPatients(String doctorId, OnCompleteListener listener) {
        db.collection("users")
                .whereEqualTo("role", "patient")
                .whereEqualTo("linkedDoctorId", doctorId)
                .get()
                .addOnCompleteListener(listener);
    }
}
