package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.services.AppointmentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppointmentManagerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String doctorUid;

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;

    private AppointmentService appointmentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_manager);

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        doctorUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Initialize appointment service
        appointmentService = new AppointmentService(this, db, doctorUid);

        // RecyclerView setup
        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();

        // Pass `true` to show Accept/Decline buttons for doctor
        adapter = new AppointmentAdapter(appointmentList, true, this::handleAppointmentAction);
        rvAppointments.setAdapter(adapter);

        loadAppointments();
    }

    private void loadAppointments() {
        appointmentService.fetchDoctorAppointments(doctorUid, new AppointmentService.AppointmentFetchCallback() {
            @Override
            public void onAppointmentsFetched(List<Appointment> list) {
                appointmentList.clear();
                appointmentList.addAll(list);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AppointmentManagerActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleAppointmentAction(Appointment appointment, boolean isAccepted) {
        appointmentService.updateAppointmentStatus(appointment.getId(), isAccepted, new AppointmentService.AppointmentCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AppointmentManagerActivity.this,
                        "Appointment " + (isAccepted ? "accepted" : "declined"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AppointmentManagerActivity.this,
                        "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
