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

import java.util.*;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeMessage;
    private LinearLayout vitalsDisplayLayout, medicalHistoryLayout;
    private EditText etHeartRate, etTemperature, etOxygen;
    private EditText etCondition, etMedication, etNote;
    private EditText etAppointmentTime;
    private EditText etDoctorId;

    private Button btnUploadVitals, btnSaveHistory, btnBookAppointment;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId; // Remove final and don't initialize here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish(); // Close this activity
            return;
        }

        userId = currentUser.getUid(); // Set userId here after null check

        initUI();
        loadVitals();
        loadMedicalHistory();

        tvWelcomeMessage.setText("Welcome, Patient");

        btnUploadVitals.setOnClickListener(v -> {
            try {
                String heartRateStr = etHeartRate.getText().toString().trim();
                String temperatureStr = etTemperature.getText().toString().trim();
                String oxygenStr = etOxygen.getText().toString().trim();

                if (heartRateStr.isEmpty() || temperatureStr.isEmpty() || oxygenStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all vital fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double heartRate = Double.parseDouble(heartRateStr);
                double temperature = Double.parseDouble(temperatureStr);
                int oxygen = Integer.parseInt(oxygenStr);

                VitalsService.uploadVitals(heartRate, temperature, oxygen);
                Toast.makeText(this, "Vitals uploaded", Toast.LENGTH_SHORT).show();

                // Clear the input fields after successful upload
                etHeartRate.setText("");
                etTemperature.setText("");
                etOxygen.setText("");

                // Refresh vitals display
                loadVitals();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error uploading vitals: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveHistory.setOnClickListener(v -> {
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

                // Clear the input fields after successful save
                etCondition.setText("");
                etMedication.setText("");
                etNote.setText("");

                // Refresh medical history display
                loadMedicalHistory();
            } catch (Exception e) {
                Toast.makeText(this, "Error updating history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnBookAppointment.setOnClickListener(v -> {
            String doctorId = etDoctorId.getText().toString().trim();
            String time = etAppointmentTime.getText().toString().trim();

            if (doctorId.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please enter doctor ID and select appointment time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate doctor ID exists
            validateAndBookAppointmentAndLinkDoctor(doctorId, time);

        });

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
        etDoctorId = findViewById(R.id.etDoctorId);

        btnUploadVitals = findViewById(R.id.btnUploadVitals);
        btnSaveHistory = findViewById(R.id.btnSaveHistory);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
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
                        tv.setTextSize(14);
                        tv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                        vitalsDisplayLayout.addView(tv);
                    } else {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Double heartRateObj = doc.getDouble("heartRate");
                            Double temperatureObj = doc.getDouble("temperature");
                            Long oxygenObj = doc.getLong("oxygen");

                            // Null checks
                            double heartRate = heartRateObj != null ? heartRateObj : 0.0;
                            double temperature = temperatureObj != null ? temperatureObj : 0.0;
                            int oxygen = oxygenObj != null ? oxygenObj.intValue() : 0;

                            TextView tv = new TextView(this);
                            tv.setText("Latest Vitals:\nHR: " + heartRate + " bpm\nTemp: " + temperature + "°C\nO₂: " + oxygen + "%");
                            tv.setTextSize(14);
                            vitalsDisplayLayout.addView(tv);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load vitals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                        tv.setText("Medical History:\nCondition: " + (cond != null ? cond : "N/A") +
                                "\nMedication: " + (med != null ? med : "N/A") +
                                "\nNote: " + (note != null ? note : "N/A"));
                        tv.setTextSize(14);
                        medicalHistoryLayout.addView(tv);
                    } else {
                        TextView tv = new TextView(this);
                        tv.setText("No medical history available");
                        tv.setTextSize(14);
                        tv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                        medicalHistoryLayout.addView(tv);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load medical history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
    private void validateAndBookAppointmentAndLinkDoctor(String doctorId, String time) {
        db.collection("users")
                .document(doctorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && "doctor".equals(doc.getString("role"))) {
                        // Doctor exists - Link doctor to patient
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("linkedDoctorId", doctorId);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Now book appointment with proper listeners
                                    AppointmentService.bookAppointment(userId, doctorId, time)
                                            .addOnSuccessListener(documentReference -> {
                                                Toast.makeText(this, "Appointment booked and doctor linked!", Toast.LENGTH_SHORT).show();
                                                etDoctorId.setText("");
                                                etAppointmentTime.setText("");
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed to book appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to link doctor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Invalid Doctor ID", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error validating doctor ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}



