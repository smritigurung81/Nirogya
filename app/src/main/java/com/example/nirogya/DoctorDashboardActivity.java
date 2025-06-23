package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class DoctorDashboardActivity extends AppCompatActivity {

    FirebaseFirestore db;
    FirebaseAuth auth;
    String doctorNMC;
    RecyclerView rvPatients;
    PatientListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        rvPatients = findViewById(R.id.rvAssignedPatients);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));

        // Get NMC number from the currently logged-in doctor's document
        db.collection("doctors").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    doctorNMC = doc.getString("nmcNumber");
                    if (doctorNMC != null) {
                        loadAssignedPatients();
                    } else {
                        Toast.makeText(this, "NMC number not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load doctor info", Toast.LENGTH_SHORT).show());
    }

    private void loadAssignedPatients() {
        db.collection("users")
                .whereEqualTo("linkedDoctorNmc", doctorNMC)
                .get()
                .addOnSuccessListener(query -> {
                    List<Patient> patientList = new ArrayList<>();
                    for (DocumentSnapshot snapshot : query.getDocuments()) {
                        Patient patient = snapshot.toObject(Patient.class);
                        if (patient != null) {
                            // Firestore doesn't include the document ID in the object by default
                            patient.setUid(snapshot.getId());
                            patientList.add(patient);
                        }
                    }
                    adapter = new PatientListAdapter(this, patientList);
                    rvPatients.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load patients", Toast.LENGTH_SHORT).show());
    }
}


