package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.example.nirogya.services.VitalsService;
import com.example.nirogya.services.MedicalHistoryService;
import com.example.nirogya.MedicalHistoryInputDialog;
import com.example.nirogya.adapters.MedicalHistoryAdapter;
import com.example.nirogya.models.MedicalHistory;
import com.example.nirogya.Vital;
import com.example.nirogya.VitalsAdapter;
import com.example.nirogya.services.AppointmentService;
import com.example.nirogya.services.LabReportService;



import java.util.*;

public class PatientDashboardActivity extends AppCompatActivity {

    FirebaseFirestore db;
    VitalsService vitalsService;
    LabReportService labReportService;

    FirebaseAuth mAuth;
    String uid;
    AppointmentService appointmentService;
    AppointmentAdapter appointmentAdapter;

    MedicalHistoryService medicalHistoryService;
    MedicalHistoryAdapter medicalHistoryAdapter;
    Button btnAddVitals, btnBookAppointment;
    Button btnAddMedicalHistory;

    RecyclerView rvVitals, rvMedicalHistory, rvDoctorVitals, rvLabReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        // Firebase setup
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish(); // or redirect to LoginActivity if preferred
            return;
        }

        // Initialize Services
        vitalsService = new VitalsService(this, db, uid);
        medicalHistoryService = new MedicalHistoryService(this, db, uid);
        appointmentService = new AppointmentService(this, db, uid);
        labReportService = new LabReportService(this, FirebaseFirestore.getInstance());


        // UI setup
        btnAddVitals = findViewById(R.id.btnAddVitals);
        btnAddMedicalHistory = findViewById(R.id.btnAddMedicalHistory);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);

        rvVitals = findViewById(R.id.rvVitals);
        rvMedicalHistory = findViewById(R.id.rvMedicalHistory);
        rvDoctorVitals = findViewById(R.id.rvDoctorVitals);
        rvLabReports = findViewById(R.id.rvLabReports);

        rvVitals.setLayoutManager(new LinearLayoutManager(this));
        rvMedicalHistory.setLayoutManager(new LinearLayoutManager(this));
        rvDoctorVitals.setLayoutManager(new LinearLayoutManager(this));
        rvLabReports.setLayoutManager(new LinearLayoutManager(this));

        // Handle +Add Vitals button
        btnAddVitals.setOnClickListener(v -> PatientVitalsInputDialog.show(this, (systolic, diastolic, hr, oxygen, temp) -> {
            String bp = systolic + "/" + diastolic;
            vitalsService.addVitals(bp, oxygen, hr, temp);
        }));

        // Add medical history
        btnAddMedicalHistory.setOnClickListener(v -> MedicalHistoryInputDialog.show(this, (disease, duration, remarks) -> medicalHistoryService.addMedicalHistory(disease, duration, remarks)));


        // Button Listeners
        btnBookAppointment.setOnClickListener(v -> openAppointmentDialog());

        // Load data
        loadVitals();
        loadMedicalHistory();
        loadDoctorVitals();
        loadLabReports();
    }

    private void openAppointmentDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            String selectedDate = day + "/" + (month + 1) + "/" + year;

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (v, hour, minute) -> {
                String selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute);
                // Fetch the linked doctor ID before booking
                db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                    String doctorId = doc.getString("linkedDoctorId");
                    if (doctorId == null || doctorId.isEmpty()) {
                        Toast.makeText(this, "No doctor linked.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    appointmentService.bookAppointment(doctorId, selectedDate, selectedTime, new AppointmentService.AppointmentCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(PatientDashboardActivity.this, "Appointment requested", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(PatientDashboardActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });

                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);

            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
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
                        break; // only show latest
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
                    List<MedicalHistory> historyList = new ArrayList<>();
                    assert value != null;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        MedicalHistory history = doc.toObject(MedicalHistory.class);
                        historyList.add(history);
                    }
                    medicalHistoryAdapter = new MedicalHistoryAdapter(historyList);
                    rvMedicalHistory.setAdapter(medicalHistoryAdapter);

                });
    }

    private void loadDoctorVitals() {
        db.collection("users").document(uid).collection("doctor_vitals")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<Vital> doctorVitals = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String systolic = doc.getString("systolic");
                        String diastolic = doc.getString("diastolic");
                        String bp = systolic + "/" + diastolic;
                        String oxygen = doc.getString("oxygen");
                        String hr = doc.getString("heartRate");
                        String temp = doc.getString("temperature");
                        String note = doc.getString("note");

                        doctorVitals.add(new Vital("Blood Pressure", bp, R.drawable.ic_blood_pressure));
                        doctorVitals.add(new Vital("Oxygen", oxygen, R.drawable.ic_oxygen));
                        doctorVitals.add(new Vital("Temperature", temp, R.drawable.ic_temperature));
                        doctorVitals.add(new Vital("Heart Rate", hr, R.drawable.ic_heart_rate));
                        doctorVitals.add(new Vital("Doctor Note", note, R.drawable.ic_note));
                        break; // show only latest set
                    }

                    VitalsAdapter adapter = new VitalsAdapter(doctorVitals);
                    rvDoctorVitals.setAdapter(adapter);
                });
    }


    private void loadLabReports() {
        labReportService.fetchLabReports(uid, new LabReportService.LabReportCallback() {
            @Override
            public void onReportsFetched(List<LabReport> reports) {
                LabReportAdapter adapter = new LabReportAdapter(PatientDashboardActivity.this, reports);
                rvLabReports.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PatientDashboardActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }


}




