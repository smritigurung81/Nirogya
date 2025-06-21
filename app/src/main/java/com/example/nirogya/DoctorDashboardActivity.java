package com.example.nirogya;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

        // UI Init
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        patientListLayout = findViewById(R.id.patientListLayout);
        appointmentListLayout = findViewById(R.id.appointmentListLayout);
        vitalListLayout = findViewById(R.id.vitalListLayout);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            doctorUid = mAuth.getCurrentUser().getUid();
            loadDoctorProfile();
            loadPatients();
            loadAppointments();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDoctorProfile() {
        patientService.getAssignedPatients(doctorUid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Object result = task.getResult();
                if (result instanceof QuerySnapshot) {
                    QuerySnapshot querySnapshot = (QuerySnapshot) result;
                    if (querySnapshot.size() > 0) {
                        String name = querySnapshot.getDocuments().get(0).getString("doctorName");
                        tvWelcomeMessage.setText("Welcome Dr. " + (name != null ? name : "Doctor"));
                    } else {
                        tvWelcomeMessage.setText("Welcome Doctor");
                    }
                } else {
                    tvWelcomeMessage.setText("Welcome Doctor");
                }
            } else {
                tvWelcomeMessage.setText("Welcome Doctor");
            }
        });
    }

    private void loadPatients() {
        patientService.getAssignedPatients(doctorUid, task -> {
            patientListLayout.removeAllViews();

            if (task.isSuccessful() && task.getResult() != null) {
                Object resultObj = task.getResult();
                if (resultObj instanceof QuerySnapshot) {
                    QuerySnapshot result = (QuerySnapshot) resultObj;

                    if (result.size() == 0) {
                        TextView tv = new TextView(this);
                        tv.setText("No patients assigned");
                        patientListLayout.addView(tv);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : result) {
                        String fullName = doc.getString("fullName");
                        String patientId = doc.getId();

                        TextView tv = new TextView(this);
                        tv.setText("👤 " + fullName);
                        tv.setTextSize(14);
                        tv.setPadding(0, 8, 0, 8);
                        patientListLayout.addView(tv);

                        // Load latest vitals for patient
                        vitalsService.getVitalsForPatient(patientId, vitalsTask -> {
                            if (vitalsTask.isSuccessful() && vitalsTask.getResult() != null) {
                                Object vitalsResultObj = vitalsTask.getResult();
                                if (vitalsResultObj instanceof QuerySnapshot) {
                                    QuerySnapshot vitalsResult = (QuerySnapshot) vitalsResultObj;
                                    if (vitalsResult.size() > 0) {
                                        DocumentSnapshot latest = vitalsResult.getDocuments().get(0);
                                        Double temp = latest.getDouble("temperature");
                                        Double heartRate = latest.getDouble("heartRate");
                                        Double oxygen = latest.getDouble("oxygenLevel");

                                        TextView vitalsTv = new TextView(this);
                                        vitalsTv.setText("📊 Temp: " + temp + "°C | HR: " + heartRate + " bpm | O₂: " + oxygen + "%");
                                        vitalsTv.setTextSize(13);
                                        vitalsTv.setPadding(32, 0, 0, 12);
                                        vitalListLayout.addView(vitalsTv);
                                    }
                                }
                            }
                        });
                    }
                }
            } else {
                Log.e("DoctorDashboard", "Failed to load patients: ", task.getException());
                Toast.makeText(this, "Failed to load patients", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAppointments() {
        appointmentListLayout.removeAllViews();

        appointmentService.getAppointmentsForDoctor(doctorUid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Object appointmentsObj = task.getResult();
                if (appointmentsObj instanceof QuerySnapshot) {
                    QuerySnapshot appointments = (QuerySnapshot) appointmentsObj;

                    if (appointments.size() == 0) {
                        TextView tv = new TextView(this);
                        tv.setText("No pending appointments");
                        appointmentListLayout.addView(tv);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : appointments) {
                        String appointmentId = doc.getId();
                        String patientId = doc.getString("patientId");
                        String reason = doc.getString("reason");
                        String dateTime = doc.getString("dateTime");
                        String status = doc.getString("status");

                        // Appointment details
                        TextView tv = new TextView(this);
                        tv.setText("📅 From: " + patientId + "\nReason: " + reason + "\nAt: " + dateTime + "\nStatus: " + status);
                        tv.setTextSize(14);
                        tv.setPadding(0, 8, 0, 4);

                        // Approve Button
                        Button btnApprove = new Button(this);
                        btnApprove.setText("Approve");
                        btnApprove.setOnClickListener(v -> {
                            appointmentService.updateAppointmentStatus(appointmentId, "approved", updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(this, "Appointment approved", Toast.LENGTH_SHORT).show();
                                    loadAppointments();
                                } else {
                                    Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });

                        // Decline Button
                        Button btnDecline = new Button(this);
                        btnDecline.setText("Decline");
                        btnDecline.setOnClickListener(v -> {
                            appointmentService.updateAppointmentStatus(appointmentId, "declined", updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(this, "Appointment declined", Toast.LENGTH_SHORT).show();
                                    loadAppointments();
                                } else {
                                    Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });

                        // Wrap all in a vertical layout
                        LinearLayout wrapper = new LinearLayout(this);
                        wrapper.setOrientation(LinearLayout.VERTICAL);
                        wrapper.setPadding(0, 0, 0, 16);
                        wrapper.addView(tv);
                        wrapper.addView(btnApprove);
                        wrapper.addView(btnDecline);

                        appointmentListLayout.addView(wrapper);
                    }
                }
            } else {
                Log.e("DoctorDashboard", "Error loading appointments: ", task.getException());
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
            }
        });
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
