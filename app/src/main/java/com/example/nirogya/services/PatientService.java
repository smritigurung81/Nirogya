package com.example.nirogya.services;

import android.util.Log;

import com.example.nirogya.adapters.PatientListAdapter;
import com.example.nirogya.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PatientService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void fetchAssignedPatients(String doctorNmc, PatientListAdapter adapter) {
        db.collection("users")
                .whereEqualTo("linkedDoctorNmc", doctorNmc)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> patients = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId()); // IMPORTANT: ensures UID is preserved
                            patients.add(user);
                        }
                    }
                    Log.d("PatientService", "Fetched assigned patients: " + patients.size());
                    adapter.updateList(patients);
                })
                .addOnFailureListener(e -> Log.e("PatientService", "Failed to fetch patients: " + e.getMessage()));
    }
}