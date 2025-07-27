package com.example.nirogya;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.adapters.AppointmentAdapter;
import com.example.nirogya.adapters.PatientListAdapter;
import com.example.nirogya.models.User;
import com.example.nirogya.services.AppointmentService;
import com.example.nirogya.services.PatientService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class DoctorDashboardActivity extends AppCompatActivity {
    private static final String TAG = "DoctorDashboard";
    private static final String PREFS_NAME = "NirogyaPrefs";
    private static final String KEY_APPOINTMENTS_FIXED = "appointments_fixed";

    private RecyclerView rvAssignedPatients, rvToday, rvPending, rvAccepted, rvDeclined;
    private PatientListAdapter patientListAdapter;
    private AppointmentAdapter adapterToday, adapterPending, adapterAccepted, adapterDeclined;
    private FirebaseFirestore db;
    private AppointmentService appointmentService;
    private PatientService patientService;
    private Button logoutBtn;
    private TextView welcomeText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        db = FirebaseFirestore.getInstance();
        appointmentService = new AppointmentService();
        patientService = new PatientService();

        welcomeText = findViewById(R.id.tvWelcomeDoctor);
        logoutBtn = findViewById(R.id.btnLogoutDoctor);

        rvAssignedPatients = findViewById(R.id.rvAssignedPatients);
        rvToday = findViewById(R.id.rvAppointmentsToday);
        rvPending = findViewById(R.id.rvAppointmentRequests);
        rvAccepted = findViewById(R.id.rvAcceptedAppointments);
        rvDeclined = findViewById(R.id.rvDeclinedAppointments);

        rvAssignedPatients.setLayoutManager(new LinearLayoutManager(this));
        rvToday.setLayoutManager(new LinearLayoutManager(this));
        rvPending.setLayoutManager(new LinearLayoutManager(this));
        rvAccepted.setLayoutManager(new LinearLayoutManager(this));
        rvDeclined.setLayoutManager(new LinearLayoutManager(this));

        adapterToday = new AppointmentAdapter(new ArrayList<>(), false);
        adapterPending = new AppointmentAdapter(new ArrayList<>(), true, createAppointmentActionCallback());
        adapterAccepted = new AppointmentAdapter(new ArrayList<>(), false);
        adapterDeclined = new AppointmentAdapter(new ArrayList<>(), false);

        rvToday.setAdapter(adapterToday);
        rvPending.setAdapter(adapterPending);
        rvAccepted.setAdapter(adapterAccepted);
        rvDeclined.setAdapter(adapterDeclined);

        patientListAdapter = new PatientListAdapter(new ArrayList<User>(), this);
        rvAssignedPatients.setAdapter(patientListAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            Log.d(TAG, "Doctor UID: " + uid);

            loadDoctorInfo(uid);
            checkAndFixExistingAppointments(uid);
        } else {
            Log.e(TAG, "No authenticated user found");
            redirectToLogin();
        }

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(DoctorDashboardActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadDoctorInfo(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String doctorNmc = documentSnapshot.getString("nmcNumber");

                        String doctorName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                        welcomeText.setText("Welcome, Doctor " + doctorName);

                        if (doctorNmc != null) {
                            patientService.fetchAssignedPatients(doctorNmc, patientListAdapter);
                        }

                        loadAppointments(uid);
                    } else {
                        Log.e(TAG, "Doctor document not found");
                        welcomeText.setText("Welcome, Doctor");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading doctor info: " + e.getMessage(), e);
                    welcomeText.setText("Welcome, Doctor");
                });
    }

    private void loadAppointments(String doctorUid) {
        Log.d(TAG, "Loading all appointments for doctor: " + doctorUid);

        appointmentService.fetchAppointmentsForDoctor(doctorUid, "today", adapterToday);
        appointmentService.fetchAppointmentsForDoctor(doctorUid, "pending", adapterPending);
        appointmentService.fetchAppointmentsForDoctor(doctorUid, "accepted", adapterAccepted);
        appointmentService.fetchAppointmentsForDoctor(doctorUid, "declined", adapterDeclined);
    }

    private void checkAndFixExistingAppointments(String doctorUid) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean appointmentsFixed = prefs.getBoolean(KEY_APPOINTMENTS_FIXED, false);

        if (!appointmentsFixed) {
            Log.d(TAG, "Fixing existing appointments - adding doctorId field");

            appointmentService.fixExistingAppointments(doctorUid, new AppointmentService.FixAppointmentsCallback() {
                @Override
                public void onSuccess(int updatedCount) {
                    Log.d(TAG, "Successfully fixed " + updatedCount + " appointments");

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_APPOINTMENTS_FIXED, true);
                    editor.apply();

                    if (updatedCount > 0) {
                        Toast.makeText(DoctorDashboardActivity.this,
                                "Updated " + updatedCount + " existing appointments",
                                Toast.LENGTH_SHORT).show();
                        loadAppointments(doctorUid);
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to fix existing appointments: " + error);
                    Toast.makeText(DoctorDashboardActivity.this,
                            "Error updating appointments: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.d(TAG, "Appointments already fixed, loading normally");
            loadAppointments(doctorUid);
        }
    }

    private AppointmentAdapter.OnAppointmentActionListener createAppointmentActionCallback() {
        return new AppointmentAdapter.OnAppointmentActionListener() {
            @Override
            public void onAccept(String appointmentId) {
                appointmentService.updateAppointmentStatus(appointmentId, "accepted", new AppointmentService.UpdateStatusCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(DoctorDashboardActivity.this, "Appointment accepted", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            loadAppointments(user.getUid());
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(DoctorDashboardActivity.this, "Error accepting appointment: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDecline(String appointmentId) {
                appointmentService.updateAppointmentStatus(appointmentId, "declined", new AppointmentService.UpdateStatusCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(DoctorDashboardActivity.this, "Appointment declined", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            loadAppointments(user.getUid());
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(DoctorDashboardActivity.this, "Error declining appointment: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadAppointments(user.getUid());
        }
    }
}
