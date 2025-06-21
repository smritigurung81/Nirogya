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
        setupClickListeners();

        // Set welcome message
        tvWelcomeMessage.setText(R.string.welcome_patient);

        // Load data after UI is set up
        loadDoctors();
        loadVitals();
        loadMedicalHistory();
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

        // Initialize spinner properly
        setupSpinner();
    }

    private void setupSpinner() {
        // Clear and initialize doctor names
        doctorNames.clear();
        doctorNames.add(getString(R.string.loading_doctors));

        // Create adapter with proper layout
        doctorAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                doctorNames
        );
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter to spinner
        spinnerDoctorName.setAdapter(doctorAdapter);
        spinnerDoctorName.setEnabled(false);

        Log.d(TAG, "Spinner initialized with adapter, items count: " + doctorNames.size());
    }

    private void setupClickListeners() {
        btnUploadVitals.setOnClickListener(v -> uploadVitals());
        btnSaveHistory.setOnClickListener(v -> saveHistory());
        btnBookAppointment.setOnClickListener(v -> bookAppointment());
        etAppointmentTime.setOnClickListener(v -> showDateTimePicker());
    }

    private void loadDoctors() {
        Log.d(TAG, "Starting to load doctors...");

        // Update UI to show loading
        runOnUiThread(() -> {
            tvDoctorLoadingStatus.setText(R.string.loading_doctors);
            tvDoctorLoadingStatus.setVisibility(View.VISIBLE);
            tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        });

        // First, let's test if we can access the users collection at all
        db.collection("users")
                .limit(1)
                .get()
                .addOnCompleteListener(testTask -> {
                    if (testTask.isSuccessful()) {
                        Log.d(TAG, "Can access users collection, found " + testTask.getResult().size() + " documents");

                        // Now try to get doctors specifically
                        queryDoctors();
                    } else {
                        Log.e(TAG, "Cannot access users collection", testTask.getException());
                        handleDoctorLoadFailure("Cannot access database: " + testTask.getException().getMessage());
                    }
                });
    }

    private void queryDoctors() {
        Log.d(TAG, "Querying for doctors...");

        db.collection("users")
                .whereEqualTo("role", "doctor")
                .get()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "Doctor query completed, success: " + task.isSuccessful());

                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d(TAG, "Found " + querySnapshot.size() + " doctors");

                        // Process results on main thread
                        runOnUiThread(() -> processDoctorResults(querySnapshot));

                    } else {
                        Log.e(TAG, "Error loading doctors", task.getException());
                        handleDoctorLoadFailure(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    private void processDoctorResults(QuerySnapshot querySnapshot) {
        // Clear existing data
        doctorNames.clear();
        doctorNameToUidMap.clear();

        // Add default selection
        doctorNames.add(getString(R.string.select_doctor));

        // Process each doctor document
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            String uid = doc.getId();
            String fullName = doc.getString("fullName");
            String email = doc.getString("email");
            String role = doc.getString("role");

            Log.d(TAG, "Processing doctor - UID: " + uid + ", Name: " + fullName +
                    ", Email: " + email + ", Role: " + role);

            // Use fullName if available, otherwise use email, otherwise use UID
            String displayName = fullName;
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = email;
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Doctor " + uid.substring(0, Math.min(8, uid.length()));
            }

            if (displayName != null && !displayName.trim().isEmpty()) {
                doctorNames.add(displayName);
                doctorNameToUidMap.put(displayName, uid);
                Log.d(TAG, "Added doctor: " + displayName + " -> " + uid);
            }
        }

        // Update the spinner
        updateSpinnerWithDoctors();
    }

    private void updateSpinnerWithDoctors() {
        Log.d(TAG, "Updating spinner with " + doctorNames.size() + " items");

        // Notify adapter of data change
        doctorAdapter.notifyDataSetChanged();

        // Enable spinner
        spinnerDoctorName.setEnabled(true);

        Log.d(TAG, "Spinner visibility: " + spinnerDoctorName.getVisibility()); // 0 = VISIBLE
        Log.d(TAG, "Spinner enabled: " + spinnerDoctorName.isEnabled());       // true = enabled

        // Update status message
        if (doctorNameToUidMap.isEmpty()) {
            tvDoctorLoadingStatus.setText(R.string.no_doctors_available);
            tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            Toast.makeText(this, R.string.no_doctors_available, Toast.LENGTH_SHORT).show();
        } else {
            tvDoctorLoadingStatus.setText(getString(R.string.doctors_found, doctorNameToUidMap.size()));
            tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));

            // Hide the status message after 3 seconds
            tvDoctorLoadingStatus.postDelayed(() -> {
                if (tvDoctorLoadingStatus != null) {
                    tvDoctorLoadingStatus.setVisibility(View.GONE);
                }
            }, 3000);
        }

        Log.d(TAG, "Spinner update complete. Enabled: " + spinnerDoctorName.isEnabled());
    }

    private void handleDoctorLoadFailure(String errorMessage) {
        runOnUiThread(() -> {
            tvDoctorLoadingStatus.setText(R.string.failed_to_load_doctors);
            tvDoctorLoadingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

            Toast.makeText(this, getString(R.string.failed_to_load_doctors_error, errorMessage),
                    Toast.LENGTH_LONG).show();

            // Add a retry option
            doctorNames.clear();
            doctorNames.add("Failed to load - Tap to retry");
            doctorAdapter.notifyDataSetChanged();

            spinnerDoctorName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0 && doctorNames.get(0).equals("Failed to load - Tap to retry")) {
                        loadDoctors(); // Retry loading
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
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

            // Create vitals data
            Map<String, Object> vitalsData = new HashMap<>();
            vitalsData.put("heartRate", heartRate);
            vitalsData.put("temperature", temperature);
            vitalsData.put("oxygen", oxygen);
            vitalsData.put("timestamp", System.currentTimeMillis());

            // Upload to Firestore
            db.collection("vitals")
                    .document(userId)
                    .collection("entries")
                    .add(vitalsData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, R.string.vitals_uploaded, Toast.LENGTH_SHORT).show();
                        etHeartRate.setText("");
                        etTemperature.setText("");
                        etOxygen.setText("");
                        loadVitals(); // Refresh the display
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error_uploading_vitals, e.getMessage()),
                            Toast.LENGTH_SHORT).show());

        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.enter_valid_numbers, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_uploading_vitals, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
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
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("condition", condition);
            historyData.put("medication", medication);
            historyData.put("note", note);
            historyData.put("timestamp", System.currentTimeMillis());

            db.collection("medicalHistories")
                    .document(userId)
                    .set(historyData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.history_updated, Toast.LENGTH_SHORT).show();
                        etCondition.setText("");
                        etMedication.setText("");
                        etNote.setText("");
                        loadMedicalHistory(); // Refresh the display
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error_updating_history, e.getMessage()),
                            Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_updating_history, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
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
                                (cond != null && !cond.isEmpty() ? cond : getString(R.string.not_available)),
                                (med != null && !med.isEmpty() ? med : getString(R.string.not_available)),
                                (note != null && !note.isEmpty() ? note : getString(R.string.not_available))));
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
        // First, link the doctor to the patient
        Map<String, Object> updates = new HashMap<>();
        updates.put("linkedDoctorId", doctorId);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Then book the appointment
                    Map<String, Object> appointmentData = new HashMap<>();
                    appointmentData.put("patientId", userId);
                    appointmentData.put("doctorId", doctorId);
                    appointmentData.put("appointmentTime", time);
                    appointmentData.put("status", "scheduled");
                    appointmentData.put("timestamp", System.currentTimeMillis());

                    db.collection("appointments")
                            .add(appointmentData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, R.string.appointment_booked_success, Toast.LENGTH_SHORT).show();
                                spinnerDoctorName.setSelection(0);
                                etAppointmentTime.setText("");
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_book_appointment, e.getMessage()),
                                    Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_link_doctor, e.getMessage()),
                        Toast.LENGTH_SHORT).show());
    }
}