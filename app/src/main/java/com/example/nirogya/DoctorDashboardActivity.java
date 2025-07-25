package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
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

        // Initialize Firestore and Services
        db = FirebaseFirestore.getInstance();
        appointmentService = new AppointmentService();
        patientService = new PatientService();

        // UI Elements
        welcomeText = findViewById(R.id.tvWelcomeDoctor);
        logoutBtn = findViewById(R.id.btnLogoutDoctor);

        rvAssignedPatients = findViewById(R.id.rvAssignedPatients);
        rvToday = findViewById(R.id.rvAppointmentsToday);
        rvPending = findViewById(R.id.rvAppointmentRequests);
        rvAccepted = findViewById(R.id.rvAcceptedAppointments);
        rvDeclined = findViewById(R.id.rvDeclinedAppointments);

        // Layout Managers
        rvAssignedPatients.setLayoutManager(new LinearLayoutManager(this));
        rvToday.setLayoutManager(new LinearLayoutManager(this));
        rvPending.setLayoutManager(new LinearLayoutManager(this));
        rvAccepted.setLayoutManager(new LinearLayoutManager(this));
        rvDeclined.setLayoutManager(new LinearLayoutManager(this));

        // Adapters
        adapterToday = new AppointmentAdapter(new ArrayList<>(), false);
        adapterPending = new AppointmentAdapter(new ArrayList<>(), false);
        adapterAccepted = new AppointmentAdapter(new ArrayList<>(), false);
        adapterDeclined = new AppointmentAdapter(new ArrayList<>(), false);

        rvToday.setAdapter(adapterToday);
        rvPending.setAdapter(adapterPending);
        rvAccepted.setAdapter(adapterAccepted);
        rvDeclined.setAdapter(adapterDeclined);

        patientListAdapter = new PatientListAdapter(new ArrayList<User>(), this);
        rvAssignedPatients.setAdapter(patientListAdapter);

        // Load Doctor Info
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("DoctorDashboard", "Doctor UID: " + user.getUid());

        if (user != null) {
            String uid = user.getUid();

            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String doctorNmc = documentSnapshot.getString("nmcNumber");

                    String doctorName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                    welcomeText.setText("Welcome, Doctor " + doctorName);

                    // Load assigned patients using linkedDoctorNmc
                    patientService.fetchAssignedPatients(doctorNmc, patientListAdapter);

                    // Load appointments for doctor
                    appointmentService.fetchAppointmentsForDoctor(uid, "today", adapterToday);
                    appointmentService.fetchAppointmentsForDoctor(uid, "pending", adapterPending);
                    appointmentService.fetchAppointmentsForDoctor(uid, "accepted", adapterAccepted);
                    appointmentService.fetchAppointmentsForDoctor(uid, "declined", adapterDeclined);
                }
            }).addOnFailureListener(e -> {
                welcomeText.setText("Welcome, Doctor");
            });
        }

        // Logout Button
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(DoctorDashboardActivity.this, LoginActivity.class));
            finish();
        });
    }
}
