package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.adapters.VitalsAdapter;
import com.example.nirogya.models.Vital;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PatientDashboardActivity extends AppCompatActivity {

    private RecyclerView rvVitals, rvDoctorVitals, rvLabReports, rvAppointmentsToday;
    private VitalsAdapter vitalsAdapter, doctorVitalsAdapter;
    private Button btnAddVitals, btnBookAppointment, btnLogout;
    private TextView tvReminder, tvTodayAppointmentsLabel;

    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            // Redirect to login if user not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views and setup
        initializeViews();
        setupRecyclerViews();
        setupButtonListeners();
        loadPatientData();
    }

    private void initializeViews() {
        rvVitals = findViewById(R.id.rvVitals);
        rvDoctorVitals = findViewById(R.id.rvDoctorVitals);
        rvLabReports = findViewById(R.id.rvLabReports);
        rvAppointmentsToday = findViewById(R.id.rvAppointmentsToday);

        btnAddVitals = findViewById(R.id.btnAddVitals);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnLogout = findViewById(R.id.btnLogout);

        tvReminder = findViewById(R.id.tvReminder);
        tvTodayAppointmentsLabel = findViewById(R.id.tvTodayAppointmentsLabel);
    }

    private void setupRecyclerViews() {
        rvVitals.setLayoutManager(new LinearLayoutManager(this));
        rvDoctorVitals.setLayoutManager(new LinearLayoutManager(this));
        rvLabReports.setLayoutManager(new LinearLayoutManager(this));
        rvAppointmentsToday.setLayoutManager(new LinearLayoutManager(this));

        vitalsAdapter = new VitalsAdapter(new ArrayList<>());
        doctorVitalsAdapter = new VitalsAdapter(new ArrayList<>());

        rvVitals.setAdapter(vitalsAdapter);
        rvDoctorVitals.setAdapter(doctorVitalsAdapter);

        rvLabReports.setVisibility(View.GONE);
        rvAppointmentsToday.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        btnAddVitals.setOnClickListener(v -> {
            Toast.makeText(this, "Add Vitals feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnBookAppointment.setOnClickListener(v -> {
            Toast.makeText(this, "Book Appointment feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadPatientData() {
        loadPatientVitals();
        loadDoctorVitals();
        // loadLabReports(); // Enable once LabReportAdapter is ready
        // loadTodaysAppointments(); // Enable once AppointmentAdapter is ready
    }

    private void loadPatientVitals() {
        db.collection("vitals")
                .whereEqualTo("patientId", currentUserId)
                .whereEqualTo("enteredBy", "patient")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Vital> vitals = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Vital vital = document.toObject(Vital.class);
                            vitals.add(vital);
                        } catch (Exception ignored) {}
                    }
                    vitalsAdapter.updateVitals(vitals);

                    if (vitals.isEmpty()) {
                        showEmptyVitalsMessage(rvVitals, "No vitals recorded yet. Tap 'Add Vitals' to get started.");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load your vitals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadDoctorVitals() {
        db.collection("vitals")
                .whereEqualTo("patientId", currentUserId)
                .whereEqualTo("enteredBy", "doctor")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Vital> vitals = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Vital vital = document.toObject(Vital.class);
                            vitals.add(vital);
                        } catch (Exception ignored) {}
                    }
                    doctorVitalsAdapter.updateVitals(vitals);

                    if (vitals.isEmpty()) {
                        showEmptyVitalsMessage(rvDoctorVitals, "No vitals from doctors yet.");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load doctor vitals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEmptyVitalsMessage(RecyclerView recyclerView, String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPatientData();
    }
}
