package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

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
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvPatients = findViewById(R.id.rvAssignedPatients);
        btnLogout = findViewById(R.id.btnLogout);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));

        // Logout functionality
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Check for valid session
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Get NMC number from the current doctor's document in the 'users' collection
        db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    doctorNMC = doc.getString("nmcNumber");
                    if (doctorNMC != null && !doctorNMC.isEmpty()) {
                        Log.d("DoctorDashboard", "Doctor NMC: " + doctorNMC);
                        loadAssignedPatients();
                    } else {
                        Toast.makeText(this, "NMC number not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load doctor info", Toast.LENGTH_SHORT).show();
                    Log.e("DoctorDashboard", "Error fetching doctor data", e);
                });
    }

    private void loadAssignedPatients() {
        Log.d("DoctorDashboard", "Loading patients for NMC: " + doctorNMC);

        db.collection("users")
                .whereEqualTo("linkedDoctorNmc", doctorNMC)
                .get()
                .addOnSuccessListener(query -> {
                    List<Patient> patientList = new ArrayList<>();
                    for (DocumentSnapshot snapshot : query.getDocuments()) {
                        Patient patient = snapshot.toObject(Patient.class);
                        if (patient != null) {
                            patient.setUid(snapshot.getId()); // add UID manually
                            patientList.add(patient);
                        }
                    }

                    if (patientList.isEmpty()) {
                        Toast.makeText(this, "No patients assigned yet.", Toast.LENGTH_SHORT).show();
                    }

                    adapter = new PatientListAdapter(this, patientList);
                    rvPatients.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load patients", Toast.LENGTH_SHORT).show();
                    Log.e("DoctorDashboard", "Error loading patient list", e);
                });
    }
}
