package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.adapters.AppointmentAdapter;
import com.example.nirogya.models.Appointment;
import com.example.nirogya.services.AppointmentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppointmentManagerActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private AppointmentAdapter appointmentAdapter;
    private AppointmentService appointmentService;
    private List<Appointment> appointmentList;
    private String doctorId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_manager);

        doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Initialize service
        appointmentService = new AppointmentService(this, db, doctorId);

        // Setup RecyclerView
        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList, true);
        rvAppointments.setAdapter(appointmentAdapter);

        // Load data
        loadDoctorAppointments();
    }

    private void loadDoctorAppointments() {
        appointmentService.fetchAppointmentsForDoctor(
                appointments -> {
                    appointmentList.clear();
                    appointmentList.addAll(appointments);
                    appointmentAdapter.notifyDataSetChanged();
                },
                e -> Toast.makeText(this, "Failed to fetch appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}
