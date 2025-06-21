package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeMessage;
    private LinearLayout vitalsDisplayLayout, medicalHistoryLayout;
    private EditText etHeartRate, etTemperature, etOxygen;
    private EditText etCondition, etMedication, etNote;
    private EditText etAppointmentTime;
    private Spinner doctorNameSpinner;

    private Button btnUploadVitals, btnSaveHistory, btnBookAppointment;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private Map<String, String> doctorNameToIdMap = new HashMap<>();
    private List<String> doctorNameList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = currentUser.getUid();
        initUI();
        loadVitals();
        loadMedicalHistory();
        fetchDoctors();

        tvWelcomeMessage.setText("Welcome, Patient");

        btnUploadVitals.setOnClickListener(v -> uploadVitals());
        btnSaveHistory.setOnClickListener(v -> saveMedicalHistory());
        btnBookAppointment.setOnClickListener(v -> bookAppointment());

        etAppointmentTime.setOnClickListener(v -> showDateTimePicker());
    }

    private void initUI() {
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        vitalsDisplayLayout = findViewById(R.id.vitalsDisplayLayout);
        medicalHistoryLayout = findViewById(R.id.medicalHistoryLayout);

        etHeartRate = findViewById(R.id.etHeartRate);
        etTemperature = findViewById(R.id.etTemperature);
        etOxygen = findViewById(R.id.etOxygen);
        etCondition = findViewById(R.id.etCondition);
        etMedication = findViewById(R.id.etMedication);
        etNote = findViewById(R.id.etNote);
        etAppointmentTime = findViewById(R.id.etAppointmentDateTime);
        doctorNameSpinner = findViewById(R.id.spinnerDoctorName);

        btnUploadVitals = findViewById(R.id.btnUploadVitals);
        btnSaveHistory = findViewById(R.id.btnSaveHistory);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
    }

    private void fetchDoctors() {
        db.collection("users")
                .whereEqualTo("role", "doctor")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    doctorNameList.clear();
                    doctorNameToIdMap.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("fullName");
                        if (name != null) {
                            doctorNameList.add(name);
                            doctorNameToIdMap.put(name, doc.getId());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_dropdown_item, doctorNameList);
                    doctorNameSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load doctors", Toast.LENGTH_SHORT).show());
    }

    private void uploadVitals() {
        String heartRateStr = etHeartRate.getText().toString().trim();
        String temperatureStr = etTemperature.getText().toString().trim();
        String oxygenStr = etOxygen.getText().toString().trim();

        if (heartRateStr.isEmpty() || temperatureStr.isEmpty() || oxygenStr.isEmpty()) {
            Toast.makeText(this, "Please fill all vital fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double heartRate = Double.parseDouble(heartRateStr);
            double temperature = Double.parseDouble(temperatureStr);
            int oxygen = Integer.parseInt(oxygenStr);

            VitalsService.uploadVitals(heartRate, temperature, oxygen);
            Toast.makeText(this, "Vitals uploaded", Toast.LENGTH_SHORT).show();

            etHeartRate.setText("");
            etTemperature.setText("");
            etOxygen.setText("");

            loadVitals();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input format", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMedicalHistory() {
        String condition = etCondition.getText().toString().trim();
        String medication = etMedication.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (condition.isEmpty() && medication.isEmpty() && note.isEmpty()) {
            Toast.makeText(this, "Please enter at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MedicalHistoryService.updateHistory(condition, medication, note);
            Toast.makeText(this, "History updated", Toast.LENGTH_SHORT).show();

            etCondition.setText("");
            etMedication.setText("");
            etNote.setText("");

            loadMedicalHistory();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bookAppointment() {
        String selectedDoctorName = (String) doctorNameSpinner.getSelectedItem();
        String doctorId = doctorNameToIdMap.get(selectedDoctorName);
        String time = etAppointmentTime.getText().toString().trim();

        if (doctorId == null || time.isEmpty()) {
            Toast.makeText(this, "Select doctor and appointment time", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("linkedDoctorId", doctorId);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    AppointmentService.bookAppointment(userId, doctorId, time)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Appointment booked", Toast.LENGTH_SHORT).show();
                                etAppointmentTime.setText("");
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to link doctor", Toast.LENGTH_SHORT).show());
    }

    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                String formatted = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d %02d:%02d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE));

                etAppointmentTime.setText(formatted);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadVitals() {
        db.collection("vitals")
                .document(userId)
                .collection("entries")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    vitalsDisplayLayout.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("No vitals data available");
                        vitalsDisplayLayout.addView(tv);
                    } else {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Double heartRate = doc.getDouble("heartRate");
                            Double temperature = doc.getDouble("temperature");
                            Long oxygen = doc.getLong("oxygen");
                            Long timestamp = doc.getLong("timestamp");

                            String formattedTime = "Unknown";
                            if (timestamp != null) {
                                Date date = new Date(timestamp);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
                                formattedTime = sdf.format(date);
                            }

                            TextView tv = new TextView(this);
                            tv.setText("HR: " + heartRate + " bpm\nTemp: " + temperature + "°C\nO₂: " + oxygen + "%\nRecorded: " + formattedTime);
                            vitalsDisplayLayout.addView(tv);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading vitals", Toast.LENGTH_SHORT).show());
    }

    private void loadMedicalHistory() {
        db.collection("medicalHistories")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    medicalHistoryLayout.removeAllViews();

                    if (doc.exists()) {
                        String cond = doc.getString("condition");
                        String med = doc.getString("medication");
                        String note = doc.getString("note");

                        TextView tv = new TextView(this);
                        tv.setText("Condition: " + (cond != null ? cond : "N/A") +
                                "\nMedication: " + (med != null ? med : "N/A") +
                                "\nNote: " + (note != null ? note : "N/A"));
                        medicalHistoryLayout.addView(tv);
                    } else {
                        TextView tv = new TextView(this);
                        tv.setText("No medical history available");
                        medicalHistoryLayout.addView(tv);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show());
    }
}
