package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
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
    Button btnLogout; // Added logout button reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        rvPatients = findViewById(R.id.rvAssignedPatients);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        btnLogout = findViewById(R.id.btnLogout); // Link to XML button

        // Handle logout
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // prevent back nav
            startActivity(intent);
            finish();
        });

        // Get NMC number from the currently logged-in doctor's document
        db.collection("users").document(auth.getCurrentUser().getUid())
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



