package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class PatientDashboardActivity extends AppCompatActivity {

    private static final String TAG = "PatientDashboard";

    private TextView tvWelcomeMessage, tvDoctorLoadingStatus;
    private LinearLayout vitalsDisplayLayout, medicalHistoryLayout;
    private EditText etHeartRate, etTemperature, etOxygen;
    private EditText etCondition, etMedication, etNote;
    private EditText etAppointmentTime;
    private Spinner spinnerDoctorName;

    private Button btnUploadVitals, btnSaveHistory, btnBookAppointment;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;

    // For doctor spinner
    private ArrayAdapter<String> doctorAdapter;
    private final List<String> doctorNames = new ArrayList<>();
    private final Map<String, String> doctorNameToUidMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.please_login_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = currentUser.getUid();
        Log.d(TAG, "User ID: " + userId);

        initUI();
        debugFirestoreConnection();
        loadDoctors();
        loadVitals();
        loadMedicalHistory();

        tvWelcomeMessage.setText(R.string.welcome_patient);

        setupClickListeners();
    }

    private void initUI() {
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        tvDoctorLoadingStatus = findViewById(R.id.tvDoctorLoadingStatus);
        vitalsDisplayLayout = findViewById(R.id.vitalsDisplayLayout);
        medicalHistoryLayout = findViewById(R.id.medicalHistoryLayout);

        etHeartRate = findViewById(R.id.etHeartRate);
        etTemperature = findViewById(R.id.etTemperature);
        etOxygen = findViewById(R.id.etOxygen);
        etCondition = findViewById(R.id.etCondition);
        etMedication = findViewById(R.id.etMedication);
        etNote = findViewById(R.id.etNote);
        etAppointmentTime = findViewById(R.id.etAppointmentDateTime);
        spinnerDoctorName = findViewById(R.id.spinnerDoctorName);

        btnUploadVitals = findViewById(R.id.btnUploadVitals);
        btnSaveHistory = findViewById(R.id.btnSaveHistory);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);

        // Initialize spinner
        doctorNames.add(getString(R.string.loading_doctors));
        doctorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctorNames);
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctorName.setAdapter(doctorAdapter);
        spinnerDoctorName.setEnabled(false);
    }

    private void setupClickListeners() {
        btnUploadVitals.setOnClickListener(v -> uploadVitals());
        btnSaveHistory.setOnClickListener(v -> saveHistory());
        btnBookAppointment.setOnClickListener(v -> bookAppointment());
        etAppointmentTime.setOnClickListener(v -> showDateTimePicker());
    }

    private void debugFirestoreConnection() {
        Log.d(TAG, "Testing Firestore connection...");

        db.collection("users")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        Log.d(TAG, "Firestore connection successful. Total users found: " + result.size());

                        // Test getting all users to see structure
                        db.collection("users")
                                .get()
                                .addOnSuccessListener(allUsers -> {
                                    Log.d(TAG, "All users count: " + allUsers.size());
                                    for (DocumentSnapshot doc : allUsers.getDocuments()) {
                                        String role = doc.getString("role");
                                        String fullName = doc.getString("fullName");
                                        Log.d(TAG, "User ID: " + doc.getId() + ", Role: " + role + ", Name: " + fullName);
                                    }
                                });
                    } else {
                        Log.e(TAG, "Firestore connection failed", task.getException());
                    }
                });
    }

    private void loadDoctors() {
        Log.d(TAG, "Loading doctors...");
        tvDoctorLoadingStatus.setText(R.string.loading_doctors);
        tvDoctorLoadingStatus.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("role", "doctor")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d(TAG, "Doctors query completed. Found " + querySnapshot.size() + " doctors");

                        doctorNames.clear();
                        doctorNameToUidMap.clear();
                        doctorNames.add(getString(R.string.select_doctor));

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String uid = doc.getId();
                            String fullName = doc.getString("fullName");

                            Log.d(TAG, "Doctor found - UID: " + uid + ", Name: " + fullName);

                            if (fullName != null && !fullName.isEmpty()) {
                                doctorNames.add(fullName);
                                doctorNameToUidMap.put(fullName, uid);
                            }
                        }

                        runOnUiThread(() -> {
                            doctorAdapter.notifyDataSetChanged();
                            spinnerDoctorName.setEnabled(true);

                            if (doctorNameToUidMap.isEmpty()) {
                                tvDoctorLoadingStatus.setText(R.string.no_doctors_available);
                                tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                                Toast.makeText(this, R.string.no_doctors_available, Toast.LENGTH_SHORT).show();
                            } else {
                                tvDoctorLoadingStatus.setText(getString(R.string.doctors_found, doctorNameToUidMap.size()));
                                tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                                tvDoctorLoadingStatus.postDelayed(() ->
                                        tvDoctorLoadingStatus.setVisibility(View.GONE), 2000);
                            }
                        });

                    } else {
                        Log.e(TAG, "Error loading doctors", task.getException());
                        runOnUiThread(() -> {
                            tvDoctorLoadingStatus.setText(R.string.failed_to_load_doctors);
                            tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                            if (task.getException() != null) {
                                Toast.makeText(this, getString(R.string.failed_to_load_doctors_error, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void uploadVitals() {
        try {
            String heartRateStr = etHeartRate.getText().toString().trim();
            String temperatureStr = etTemperature.getText().toString().trim();
            String oxygenStr = etOxygen.getText().toString().trim();

            if (heartRateStr.isEmpty() || temperatureStr.isEmpty() || oxygenStr.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_vital_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            double heartRate = Double.parseDouble(heartRateStr);
            double temperature = Double.parseDouble(temperatureStr);
            int oxygen = Integer.parseInt(oxygenStr);

            VitalsService.uploadVitals(heartRate, temperature, oxygen);
            Toast.makeText(this, R.string.vitals_uploaded, Toast.LENGTH_SHORT).show();

            etHeartRate.setText("");
            etTemperature.setText("");
            etOxygen.setText("");

            loadVitals();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.enter_valid_numbers, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_uploading_vitals, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveHistory() {
        String condition = etCondition.getText().toString().trim();
        String medication = etMedication.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (condition.isEmpty() && medication.isEmpty() && note.isEmpty()) {
            Toast.makeText(this, R.string.enter_at_least_one_field, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MedicalHistoryService.updateHistory(condition, medication, note);
            Toast.makeText(this, R.string.history_updated, Toast.LENGTH_SHORT).show();

            etCondition.setText("");
            etMedication.setText("");
            etNote.setText("");

            loadMedicalHistory();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_updating_history, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void bookAppointment() {
        String selectedDoctorName = (String) spinnerDoctorName.getSelectedItem();
        String time = etAppointmentTime.getText().toString().trim();

        if (selectedDoctorName == null || selectedDoctorName.equals(getString(R.string.select_doctor))) {
            Toast.makeText(this, R.string.please_select_doctor, Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, R.string.please_select_appointment_time, Toast.LENGTH_SHORT).show();
            return;
        }

        String doctorUid = doctorNameToUidMap.get(selectedDoctorName);
        if (doctorUid == null) {
            Toast.makeText(this, R.string.error_doctor_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        bookAppointmentAndLinkDoctor(doctorUid, time);
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
                        tv.setText(R.string.no_vitals_available);
                        tv.setTextSize(14);
                        tv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                        vitalsDisplayLayout.addView(tv);
                    } else {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Double heartRateObj = doc.getDouble("heartRate");
                            Double temperatureObj = doc.getDouble("temperature");
                            Long oxygenObj = doc.getLong("oxygen");
                            Long timestampObj = doc.getLong("timestamp");

                            double heartRate = heartRateObj != null ? heartRateObj : 0.0;
                            double temperature = temperatureObj != null ? temperatureObj : 0.0;
                            int oxygen = oxygenObj != null ? oxygenObj.intValue() : 0;

                            String formattedTime = getString(R.string.unknown_time);
                            if (timestampObj != null) {
                                Date date = new Date(timestampObj);
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
                                formattedTime = sdf.format(date);
                            }

                            TextView tv = new TextView(this);
                            tv.setText(getString(R.string.vitals_display, heartRate, temperature, oxygen, formattedTime));
                            tv.setTextSize(14);
                            vitalsDisplayLayout.addView(tv);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_load_vitals, e.getMessage()), Toast.LENGTH_SHORT).show());
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
                        tv.setText(getString(R.string.history_display,
                                (cond != null ? cond : getString(R.string.not_available)),
                                (med != null ? med : getString(R.string.not_available)),
                                (note != null ? note : getString(R.string.not_available))));
                        tv.setTextSize(14);
                        medicalHistoryLayout.addView(tv);
                    } else {
                        TextView tv = new TextView(this);
                        tv.setText(R.string.no_history_available);
                        tv.setTextSize(14);
                        tv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                        medicalHistoryLayout.addView(tv);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_load_medical_history, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
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
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true);

                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void bookAppointmentAndLinkDoctor(String doctorId, String time) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("linkedDoctorId", doctorId);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> AppointmentService.bookAppointment(userId, doctorId, time)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, R.string.appointment_booked_success, Toast.LENGTH_SHORT).show();
                            spinnerDoctorName.setSelection(0);
                            etAppointmentTime.setText("");
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_book_appointment, e.getMessage()), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_link_doctor, e.getMessage()), Toast.LENGTH_SHORT).show());
    }
}