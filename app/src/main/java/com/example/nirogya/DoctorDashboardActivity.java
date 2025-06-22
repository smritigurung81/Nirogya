// DoctorDashboardActivity.java
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
import com.google.firebase.firestore.FirebaseFirestore;
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

            String welcomeText = (fullName != null && !fullName.trim().isEmpty())
                    ? "Welcome Dr. " + fullName
                    : "Welcome Doctor";
            tvWelcomeMessage.setText(welcomeText);

            setupProfileMenu(profileMenu, fullName, email);
            loadPatients();
            loadAppointments();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void loadAppointments() {
        if (doctorUid == null) {
            Log.e("DoctorDashboard", "Doctor UID is null");
            return;
        }

        appointmentListLayout.removeAllViews();

        FirebaseFirestore.getInstance().collection("appointments")
                .whereEqualTo("doctorId", doctorUid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(appointments -> {
                    if (!isFinishing() && !isDestroyed()) {
                        if (appointments.isEmpty()) {
                            addTextToLayout(appointmentListLayout, "No pending appointments", 14);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : appointments) {
                            String appointmentId = doc.getId();
                            String patientId = doc.getString("patientId");
                            String reason = doc.getString("reason");
                            String dateTime = doc.getString("appointmentDateTime");
                            String status = doc.getString("status");

                            loadPatientNameAndCreateView(appointmentId, patientId, reason, dateTime, status);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Log.e("DoctorDashboard", "Error loading appointments", e);
                        Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPatientNameAndCreateView(String appointmentId, String patientId, String reason, String dateTime, String status) {
        FirebaseFirestore.getInstance().collection("users")
                .document(patientId)
                .get()
                .addOnSuccessListener(patientDoc -> {
                    if (!isFinishing() && !isDestroyed()) {
                        String patientName = patientDoc.exists() ?
                                patientDoc.getString("fullName") : "Unknown Patient";
                        createAppointmentView(appointmentId, patientName, reason, dateTime, status);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        createAppointmentView(appointmentId, "Unknown Patient", reason, dateTime, status);
                    }
                });
    }

    private void createAppointmentView(String appointmentId, String patientName, String reason, String dateTime, String status) {
        TextView tv = new TextView(this);
        String appointmentText = "📅 Patient: " + patientName +
                "\nReason: " + (reason != null ? reason : "General consultation") +
                "\nAt: " + (dateTime != null ? dateTime : "No date") +
                "\nStatus: " + (status != null ? status : "pending");
        tv.setText(appointmentText);
        tv.setTextSize(14);
        tv.setPadding(0, 8, 0, 4);

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
            if (!isFinishing() && !isDestroyed()) {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Appointment " + status, Toast.LENGTH_SHORT).show();
                    loadAppointments(); // Reload after update
                } else {
                    Log.e("DoctorDashboard", "Failed to update appointment", task.getException());
                    Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupProfileMenu(ImageView profileMenu, String fullName, String email) {
        profileMenu.setOnClickListener(view -> showDoctorProfilePopup(view, fullName, email));
    }

    private void showDoctorProfilePopup(View anchorView, String fullName, String email) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_profile_universal, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        TextView nameTextView = popupView.findViewById(R.id.tv_user_name);
        TextView emailTextView = popupView.findViewById(R.id.tv_user_email);
        TextView roleTextView = popupView.findViewById(R.id.tv_user_role);
        LinearLayout layoutUserRole = popupView.findViewById(R.id.layout_user_role);
        LinearLayout btnProfile = popupView.findViewById(R.id.btn_profile);
        LinearLayout btnLogout = popupView.findViewById(R.id.btn_logout);

        nameTextView.setText(fullName != null ? "Dr. " + fullName : "Doctor");
        emailTextView.setText(email != null ? email : "No email");
        roleTextView.setText("Doctor");

        layoutUserRole.setVisibility(View.VISIBLE);
        btnProfile.setVisibility(View.VISIBLE);

        btnProfile.setOnClickListener(v -> {
            popupWindow.dismiss();
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            popupWindow.dismiss();
            showLogoutConfirmation();
        });

        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAsDropDown(anchorView, -200, 10);
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
            startActivity(new Intent(DoctorDashboardActivity.this, LoginActivity.class));
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("DoctorDashboard", "Error during logout", e);
            Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPatients() {
        if (doctorUid == null) return;

        patientService.getAssignedPatients(doctorUid, task -> {
            if (!isFinishing() && !isDestroyed()) {
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
                            loadVitalsForPatient(patientId);
                        }
                    }
                }
            }
        });
    }

    private void loadVitalsForPatient(String patientId) {
        vitalsService.getVitalsForPatient(patientId, task -> {
            if (!isFinishing() && !isDestroyed()) {
                if (task.isSuccessful() && task.getResult() instanceof QuerySnapshot vitalsResult) {
                    if (!vitalsResult.isEmpty()) {
                        DocumentSnapshot latest = vitalsResult.getDocuments().get(0);
                        Double temp = latest.getDouble("temperature");
                        Double heartRate = latest.getDouble("heartRate");
                        Double oxygen = latest.getDouble("oxygenLevel");

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
}



