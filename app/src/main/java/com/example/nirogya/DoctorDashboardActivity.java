package com.example.nirogya;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class DoctorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeMessage;
    private LinearLayout patientListLayout, appointmentListLayout, vitalListLayout;
    private FirebaseAuth mAuth;
    private String doctorUid;

    private final DoctorPatientService patientService = new DoctorPatientService();
    private final DoctorAppointmentService appointmentService = new DoctorAppointmentService();
    private final DoctorVitalsService vitalsService = new DoctorVitalsService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        patientListLayout = findViewById(R.id.patientListLayout);
        appointmentListLayout = findViewById(R.id.appointmentListLayout);
        vitalListLayout = findViewById(R.id.vitalListLayout);
        ImageView profileMenu = findViewById(R.id.profileMenu);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            doctorUid = currentUser.getUid();
            String fullName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            // Set welcome message with doctor's name
            String welcomeText = (fullName != null && !fullName.trim().isEmpty())
                    ? "Welcome Dr. " + fullName
                    : "Welcome Doctor";
            tvWelcomeMessage.setText(welcomeText);

            setupProfileMenu(profileMenu, fullName, email);
            loadPatients();
            loadAppointments();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            // Redirect to login if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupProfileMenu(ImageView profileMenu, String fullName, String email) {
        profileMenu.setOnClickListener(view -> showDoctorProfilePopup(view, fullName, email));
    }

    private void showDoctorProfilePopup(View anchorView, String fullName, String email) {
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

        // Set doctor information
        String doctorName = (fullName != null && !fullName.trim().isEmpty())
                ? "Dr. " + fullName
                : "Doctor";
        String doctorEmail = (email != null && !email.trim().isEmpty())
                ? email
                : "No email available";

        nameTextView.setText(doctorName);
        emailTextView.setText(doctorEmail);
        roleTextView.setText("Doctor");

        // Show doctor-specific elements
        layoutUserRole.setVisibility(View.VISIBLE);
        btnProfile.setVisibility(View.VISIBLE);

        // Handle profile settings click
        btnProfile.setOnClickListener(v -> {
            popupWindow.dismiss();
            // Navigate to doctor profile settings (implement as needed)
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, DoctorProfileActivity.class);
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
            Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("DoctorDashboard", "Error during logout", e);
            Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPatients() {
        if (doctorUid == null) {
            Log.e("DoctorDashboard", "Doctor UID is null");
            return;
        }

        patientService.getAssignedPatients(doctorUid, task -> {
            if (!isFinishing() && !isDestroyed()) { // Check if activity is still valid
                patientListLayout.removeAllViews();
                vitalListLayout.removeAllViews();

                if (task.isSuccessful() && task.getResult() instanceof QuerySnapshot result) {
                    if (result.isEmpty()) {
                        addTextToLayout(patientListLayout, "No patients assigned", 14);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : result) {
                        String fullName = doc.getString("fullName");
                        String patientId = doc.getId();

                        if (fullName != null) {
                            addTextToLayout(patientListLayout, "👤 " + fullName, 15);

                            // Fetch latest vitals
                            loadVitalsForPatient(patientId);
                        }
                    }
                } else {
                    Log.e("DoctorDashboard", "Failed to load patients", task.getException());
                    Toast.makeText(this, "Failed to load patients", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadVitalsForPatient(String patientId) {
        vitalsService.getVitalsForPatient(patientId, vitalsTask -> {
            if (!isFinishing() && !isDestroyed()) { // Check if activity is still valid
                if (vitalsTask.isSuccessful() && vitalsTask.getResult() instanceof QuerySnapshot vitalsResult) {
                    if (!vitalsResult.isEmpty()) {
                        DocumentSnapshot latest = vitalsResult.getDocuments().get(0);
                        Double temp = latest.getDouble("temperature");
                        Double heartRate = latest.getDouble("heartRate");
                        Double oxygen = latest.getDouble("oxygenLevel");

                        // Format with null checks
                        String tempStr = temp != null ? String.format("%.1f", temp) : "N/A";
                        String hrStr = heartRate != null ? String.format("%.0f", heartRate) : "N/A";
                        String oxyStr = oxygen != null ? String.format("%.0f", oxygen) : "N/A";

                        String vitalsSummary = "📊 Temp: " + tempStr + "°C | HR: " + hrStr + " bpm | O₂: " + oxyStr + "%";
                        addTextToLayout(vitalListLayout, vitalsSummary, 13, 32);
                    }
                }
            }
        });
    }

    private void loadAppointments() {
        if (doctorUid == null) {
            Log.e("DoctorDashboard", "Doctor UID is null");
            return;
        }

        appointmentListLayout.removeAllViews();

        appointmentService.getAppointmentsForDoctor(doctorUid, task -> {
            if (!isFinishing() && !isDestroyed()) { // Check if activity is still valid
                if (task.isSuccessful() && task.getResult() instanceof QuerySnapshot appointments) {
                    if (appointments.isEmpty()) {
                        addTextToLayout(appointmentListLayout, "No pending appointments", 14);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : appointments) {
                        String appointmentId = doc.getId();
                        String patientId = doc.getString("patientId");
                        String reason = doc.getString("reason");
                        String dateTime = doc.getString("dateTime");
                        String status = doc.getString("status");

                        // Create appointment view
                        createAppointmentView(appointmentId, patientId, reason, dateTime, status);
                    }
                } else {
                    Log.e("DoctorDashboard", "Error loading appointments", task.getException());
                    Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createAppointmentView(String appointmentId, String patientId, String reason, String dateTime, String status) {
        TextView tv = new TextView(this);
        String appointmentText = "📅 From: " + (patientId != null ? patientId : "Unknown") +
                "\nReason: " + (reason != null ? reason : "No reason provided") +
                "\nAt: " + (dateTime != null ? dateTime : "No date") +
                "\nStatus: " + (status != null ? status : "pending");
        tv.setText(appointmentText);
        tv.setTextSize(14);
        tv.setPadding(0, 8, 0, 4);

        // Only show approve/decline buttons for pending appointments
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, 16);
        wrapper.addView(tv);

        if ("pending".equals(status)) {
            Button btnApprove = new Button(this);
            btnApprove.setText("Approve");
            btnApprove.setOnClickListener(v -> updateAppointmentStatus(appointmentId, "approved"));

            Button btnDecline = new Button(this);
            btnDecline.setText("Decline");
            btnDecline.setOnClickListener(v -> updateAppointmentStatus(appointmentId, "declined"));

            wrapper.addView(btnApprove);
            wrapper.addView(btnDecline);
        }

        appointmentListLayout.addView(wrapper);
    }

    private void updateAppointmentStatus(String appointmentId, String status) {
        appointmentService.updateAppointmentStatus(appointmentId, status, task -> {
            if (!isFinishing() && !isDestroyed()) { // Check if activity is still valid
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Appointment " + status, Toast.LENGTH_SHORT).show();
                    loadAppointments(); // Reload appointments
                } else {
                    Log.e("DoctorDashboard", "Failed to update appointment", task.getException());
                    Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addTextToLayout(LinearLayout layout, String text, int textSize) {
        addTextToLayout(layout, text, textSize, 0);
    }

    private void addTextToLayout(LinearLayout layout, String text, int textSize, int leftPadding) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSize);
        tv.setPadding(leftPadding, 8, 0, 8);
        layout.addView(tv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (doctorUid != null) {
            loadPatients();
            loadAppointments();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any pending callbacks if needed
    }
}



