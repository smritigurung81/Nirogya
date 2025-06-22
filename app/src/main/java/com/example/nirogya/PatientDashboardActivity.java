package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeMessage;
    private LinearLayout vitalsDisplayLayout, medicalHistoryLayout;
    private EditText etHeartRate, etTemperature, etOxygen, etCondition, etMedication, etNote, etDoctorName, etAppointmentDateTime;
    private Button btnUploadVitals, btnSaveHistory, btnBookAppointment;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String patientUid;

    private final Calendar appointmentCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        // Initialize views
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        vitalsDisplayLayout = findViewById(R.id.vitalsDisplayLayout);
        medicalHistoryLayout = findViewById(R.id.medicalHistoryLayout);
        etHeartRate = findViewById(R.id.etHeartRate);
        etTemperature = findViewById(R.id.etTemperature);
        etOxygen = findViewById(R.id.etOxygen);
        etCondition = findViewById(R.id.etCondition);
        etMedication = findViewById(R.id.etMedication);
        etNote = findViewById(R.id.etNote);
        etDoctorName = findViewById(R.id.etDoctorName);
        etAppointmentDateTime = findViewById(R.id.etAppointmentDateTime);

        btnUploadVitals = findViewById(R.id.btnUploadVitals);
        btnSaveHistory = findViewById(R.id.btnSaveHistory);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);

        ImageView profileMenu = findViewById(R.id.profileMenu);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            patientUid = currentUser.getUid();
            String fullName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            // Set welcome message with patient's name
            String welcomeText = (fullName != null && !fullName.trim().isEmpty())
                    ? "Welcome " + fullName
                    : "Welcome Patient";
            tvWelcomeMessage.setText(welcomeText);

            // Setup profile menu popup with logout
            setupProfileMenu(profileMenu, fullName, email);

            loadLatestVitals();
            loadMedicalHistory();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            // Redirect to login if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        // DateTime picker for appointment
        etAppointmentDateTime.setOnClickListener(v -> showDateTimePicker());

        // Upload vitals click
        btnUploadVitals.setOnClickListener(v -> uploadVitals());

        // Save medical history click
        btnSaveHistory.setOnClickListener(v -> saveMedicalHistory());

        // Book appointment click
        btnBookAppointment.setOnClickListener(v -> bookAppointment());
    }

    private void setupProfileMenu(ImageView profileMenu, String fullName, String email) {
        profileMenu.setOnClickListener(view -> showPatientProfilePopup(view, fullName, email));
    }

    private void showPatientProfilePopup(View anchorView, String fullName, String email) {
        // Inflate the universal popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_profile_universal, null);

        // Create PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // focusable
        );

        // Get references to UI elements (using universal IDs)
        TextView nameTextView = popupView.findViewById(R.id.tv_user_name);
        TextView emailTextView = popupView.findViewById(R.id.tv_user_email);
        TextView roleTextView = popupView.findViewById(R.id.tv_user_role);
        LinearLayout layoutUserRole = popupView.findViewById(R.id.layout_user_role);
        LinearLayout btnProfile = popupView.findViewById(R.id.btn_profile);
        LinearLayout btnLogout = popupView.findViewById(R.id.btn_logout);

        // Set patient information
        String patientName = (fullName != null && !fullName.trim().isEmpty())
                ? fullName
                : "Patient";
        String patientEmail = (email != null && !email.trim().isEmpty())
                ? email
                : "No email available";

        nameTextView.setText(patientName);
        emailTextView.setText(patientEmail);
        roleTextView.setText("Patient");

        // Show patient-specific elements
        layoutUserRole.setVisibility(View.VISIBLE);
        btnProfile.setVisibility(View.VISIBLE);

        // Handle profile settings click
        btnProfile.setOnClickListener(v -> {
            popupWindow.dismiss();
            // Navigate to patient profile settings (implement as needed)
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, PatientProfileActivity.class);
            // startActivity(intent);
        });

        // Handle logout click
        btnLogout.setOnClickListener(v -> {
            popupWindow.dismiss();
            showLogoutConfirmation();
        });

        // Configure popup appearance
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);

        // Show popup positioned properly
        popupWindow.showAsDropDown(anchorView, -200, 10); // Adjust offset as needed
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLogout() {
        try {
            mAuth.signOut();
            Intent intent = new Intent(PatientDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("PatientDashboard", "Error during logout", e);
            Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLatestVitals() {
        if (patientUid == null) {
            Log.e("PatientDashboard", "Patient UID is null");
            return;
        }

        vitalsDisplayLayout.removeAllViews();
        db.collection("vitals")
                .whereEqualTo("patientId", patientUid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isFinishing() && !isDestroyed()) { // Check if activity is still valid
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot latest = queryDocumentSnapshots.getDocuments().get(0);
                            Double temp = latest.getDouble("temperature");
                            Double heartRate = latest.getDouble("heartRate");
                            Double oxygen = latest.getDouble("oxygenLevel");

                            // Format with null checks
                            String tempStr = temp != null ? String.format("%.1f", temp) : "N/A";
                            String hrStr = heartRate != null ? String.format("%.0f", heartRate) : "N/A";
                            String oxyStr = oxygen != null ? String.format("%.0f", oxygen) : "N/A";

                            String vitalsSummary = "📊 Temp: " + tempStr + "°C | HR: " + hrStr + " bpm | O₂: " + oxyStr + "%";
                            addTextToLayout(vitalsDisplayLayout, vitalsSummary, 14);
                        } else {
                            addTextToLayout(vitalsDisplayLayout, "No vitals recorded", 14);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e("PatientDashboard", "Failed to load vitals", e);
                        Toast.makeText(this, "Failed to load vitals", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadVitals() {
        String heartRateStr = etHeartRate.getText().toString().trim();
        String temperatureStr = etTemperature.getText().toString().trim();
        String oxygenStr = etOxygen.getText().toString().trim();

        if (heartRateStr.isEmpty() || temperatureStr.isEmpty() || oxygenStr.isEmpty()) {
            Toast.makeText(this, "Please fill all vital fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double heartRate, temperature;
        int oxygen;

        try {
            heartRate = Double.parseDouble(heartRateStr);
            temperature = Double.parseDouble(temperatureStr);
            oxygen = Integer.parseInt(oxygenStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid vital values", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Firestore
        Vitals vitals = new Vitals(temperature, heartRate, oxygen, System.currentTimeMillis(), patientUid);
        db.collection("vitals").add(vitals)
                .addOnSuccessListener(documentReference -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Vitals uploaded", Toast.LENGTH_SHORT).show();
                        clearVitalsInputs();
                        loadLatestVitals();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e("PatientDashboard", "Failed to upload vitals", e);
                        Toast.makeText(this, "Failed to upload vitals", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearVitalsInputs() {
        etHeartRate.setText("");
        etTemperature.setText("");
        etOxygen.setText("");
    }

    private void loadMedicalHistory() {
        if (patientUid == null) {
            Log.e("PatientDashboard", "Patient UID is null");
            return;
        }

        medicalHistoryLayout.removeAllViews();

        db.collection("medicalHistory")
                .whereEqualTo("patientId", patientUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isFinishing() && !isDestroyed()) { // Check if activity is still valid
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String condition = doc.getString("condition");
                                String medication = doc.getString("medication");
                                String note = doc.getString("note");

                                String historyText = "Condition: " + (condition != null ? condition : "N/A") +
                                        "\nMedication: " + (medication != null ? medication : "N/A") +
                                        "\nNote: " + (note != null ? note : "N/A");
                                addTextToLayout(medicalHistoryLayout, historyText, 14, 16);
                            }
                        } else {
                            addTextToLayout(medicalHistoryLayout, "No medical history recorded", 14);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e("PatientDashboard", "Failed to load medical history", e);
                        Toast.makeText(this, "Failed to load medical history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveMedicalHistory() {
        String condition = etCondition.getText().toString().trim();
        String medication = etMedication.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (condition.isEmpty() || medication.isEmpty()) {
            Toast.makeText(this, "Please fill at least condition and medication", Toast.LENGTH_SHORT).show();
            return;
        }

        MedicalHistory history = new MedicalHistory(condition, medication, note, patientUid);

        db.collection("medicalHistory").add(history)
                .addOnSuccessListener(documentReference -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Medical history saved", Toast.LENGTH_SHORT).show();
                        clearMedicalHistoryInputs();
                        loadMedicalHistory();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e("PatientDashboard", "Failed to save medical history", e);
                        Toast.makeText(this, "Failed to save medical history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearMedicalHistoryInputs() {
        etCondition.setText("");
        etMedication.setText("");
        etNote.setText("");
    }

    private void showDateTimePicker() {
        // Date picker dialog
        int year = appointmentCalendar.get(Calendar.YEAR);
        int month = appointmentCalendar.get(Calendar.MONTH);
        int day = appointmentCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    appointmentCalendar.set(year1, month1, dayOfMonth);

                    // After date selected, show time picker
                    showTimePicker();
                }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = appointmentCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = appointmentCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    appointmentCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    appointmentCalendar.set(Calendar.MINUTE, minute1);

                    // Format and set datetime to EditText
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    etAppointmentDateTime.setText(sdf.format(appointmentCalendar.getTime()));
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void bookAppointment() {
        String doctorName = etDoctorName.getText().toString().trim();
        String appointmentDateTime = etAppointmentDateTime.getText().toString().trim();

        if (doctorName.isEmpty() || appointmentDateTime.isEmpty()) {
            Toast.makeText(this, "Please enter doctor's name and select appointment date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        Appointment appointment = new Appointment(patientUid, doctorName, appointmentDateTime);

        db.collection("appointments").add(appointment)
                .addOnSuccessListener(documentReference -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Appointment booked successfully", Toast.LENGTH_SHORT).show();
                        etDoctorName.setText("");
                        etAppointmentDateTime.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e("PatientDashboard", "Failed to book appointment", e);
                        Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to add text to layouts consistently
    private void addTextToLayout(LinearLayout layout, String text, int textSize) {
        addTextToLayout(layout, text, textSize, 0);
    }

    private void addTextToLayout(LinearLayout layout, String text, int textSize, int bottomPadding) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSize);
        tv.setPadding(0, 0, 0, bottomPadding);
        layout.addView(tv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (patientUid != null) {
            loadLatestVitals();
            loadMedicalHistory();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any pending callbacks if needed
    }

    // Model classes

    public static class Vitals {
        public double temperature, heartRate;
        public int oxygenLevel;
        public long timestamp;
        public String patientId;

        public Vitals() {
            // empty constructor for firestore
        }

        public Vitals(double temperature, double heartRate, int oxygenLevel, long timestamp, String patientId) {
            this.temperature = temperature;
            this.heartRate = heartRate;
            this.oxygenLevel = oxygenLevel;
            this.timestamp = timestamp;
            this.patientId = patientId;
        }
    }

    public static class MedicalHistory {
        public String condition, medication, note, patientId;

        public MedicalHistory() {
        }

        public MedicalHistory(String condition, String medication, String note, String patientId) {
            this.condition = condition;
            this.medication = medication;
            this.note = note;
            this.patientId = patientId;
        }
    }

    public static class Appointment {
        public String patientId, doctorName, appointmentDateTime;

        public Appointment() {
        }

        public Appointment(String patientId, String doctorName, String appointmentDateTime) {
            this.patientId = patientId;
            this.doctorName = doctorName;
            this.appointmentDateTime = appointmentDateTime;
        }
    }
}

