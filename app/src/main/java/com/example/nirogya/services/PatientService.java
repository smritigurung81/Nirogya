package com.example.nirogya.services;

import android.content.Context;
import android.widget.Toast;
import com.example.nirogya.Patient;
import com.google.firebase.firestore.*;

import java.util.*;

public class PatientService {
    private final FirebaseFirestore db;
    private final Context context;

    public interface PatientFetchCallback {
        void onFetched(List<Patient> patients);
        void onError(String error);
    }

    public PatientService(Context context, FirebaseFirestore db) {
        this.context = context;
        this.db = db;
    }

    public void getAssignedPatients(String doctorNmc, PatientFetchCallback callback) {
        db.collection("users")
                .whereEqualTo("linkedDoctorNmc", doctorNmc)
                .get()
                .addOnSuccessListener(query -> {
                    List<Patient> patientList = new ArrayList<>();
                    for (DocumentSnapshot snapshot : query.getDocuments()) {
                        Patient patient = snapshot.toObject(Patient.class);
                        if (patient != null) {
                            patient.setUid(snapshot.getId());
                            patientList.add(patient);
                        }
                    }
                    callback.onFetched(patientList);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                    Toast.makeText(context, "Failed to fetch patients", Toast.LENGTH_SHORT).show();
                });
    }
}

