package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class PatientDashboardActivity extends AppCompatActivity {

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String uid;

    Button btnAddVitals, btnBookAppointment, btnLogout;
    RecyclerView rvVitals, rvMedicalHistory, rvDoctorVitals, rvLabReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize buttons
        btnAddVitals = findViewById(R.id.btnAddVitals);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnLogout = findViewById(R.id.btnLogout);

        // Set logout listener
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PatientDashboardActivity.this, MainActivity.class));
            finish();
        });

        // Initialize RecyclerViews
        rvVitals = findViewById(R.id.rvVitals);
        rvMedicalHistory = findViewById(R.id.rvMedicalHistory);
        rvDoctorVitals = findViewById(R.id.rvDoctorVitals);
        rvLabReports = findViewById(R.id.rvLabReports);

        rvVitals.setLayoutManager(new LinearLayoutManager(this));
        rvMedicalHistory.setLayoutManager(new LinearLayoutManager(this));
        rvDoctorVitals.setLayoutManager(new LinearLayoutManager(this));
        rvLabReports.setLayoutManager(new LinearLayoutManager(this));

        // Button Listeners
        btnAddVitals.setOnClickListener(v -> addVitals());
        btnBookAppointment.setOnClickListener(v -> openAppointmentDialog());

        // Load Data
        loadVitals();
        loadMedicalHistory();
        loadDoctorVitals();
        loadLabReports();
    }

    private void addVitals() {
        Map<String, Object> vitals = new HashMap<>();
        vitals.put("pressure", "100 bpm");
        vitals.put("oxygen", "96%");
        vitals.put("heartRate", "88 bpm");
        vitals.put("temperature", "37.5°C");
        vitals.put("date", new Date());

        db.collection("users").document(uid)
                .collection("vitals_user")
                .add(vitals)
                .addOnSuccessListener(doc -> Toast.makeText(this, "Vitals added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openAppointmentDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            String selectedDate = day + "/" + (month + 1) + "/" + year;

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (v, hour, minute) -> {
                String selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute);
                saveAppointment(selectedDate, selectedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);

            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void saveAppointment(String date, String time) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String doctorId = doc.getString("linkedDoctorId");
            if (doctorId == null) {
                Toast.makeText(this, "No doctor linked.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> appointment = new HashMap<>();
            appointment.put("patientId", uid);
            appointment.put("doctorId", doctorId);
            appointment.put("date", date);
            appointment.put("time", time);
            appointment.put("status", "pending");

            db.collection("appointments")
                    .add(appointment)
                    .addOnSuccessListener(r -> Toast.makeText(this, "Appointment requested", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void loadVitals() {
        db.collection("users").document(uid).collection("vitals_user")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<Vital> vitals = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String bp = doc.getString("pressure");
                        String oxygen = doc.getString("oxygen");
                        String hr = doc.getString("heartRate");
                        String temp = doc.getString("temperature");

                        vitals.add(new Vital("Blood Pressure", bp, R.drawable.ic_blood_pressure));
                        vitals.add(new Vital("Oxygen", oxygen, R.drawable.ic_oxygen));
                        vitals.add(new Vital("Temperature", temp, R.drawable.ic_temperature));
                        vitals.add(new Vital("Heart Rate", hr, R.drawable.ic_heart_rate));
                        break; // only latest vitals
                    }
                    VitalsAdapter adapter = new VitalsAdapter(vitals);
                    rvVitals.setAdapter(adapter);
                });
    }

    private void loadMedicalHistory() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date monthAgo = cal.getTime();

        db.collection("users").document(uid).collection("medical_history")
                .whereGreaterThanOrEqualTo("date", monthAgo)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    // TODO: bind to adapter
                });
    }

    private void loadDoctorVitals() {
        db.collection("users").document(uid).collection("doctor_vitals")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    // TODO: bind to adapter
                });
    }

    private void loadLabReports() {
        db.collection("users").document(uid).collection("lab_reports")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<LabReport> reportList = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        LabReport report = doc.toObject(LabReport.class);
                        reportList.add(report);
                    }

                    LabReportAdapter adapter = new LabReportAdapter(this, reportList);
                    rvLabReports.setAdapter(adapter);
                });
    }
}





