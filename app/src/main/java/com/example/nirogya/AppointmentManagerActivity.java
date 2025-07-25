package com.example.nirogya;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nirogya.adapters.AppointmentAdapter;
import com.example.nirogya.models.Appointment;
import com.example.nirogya.services.AppointmentService;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;

public class AppointmentManagerActivity extends AppCompatActivity {

    private RecyclerView todayAppointments, requestAppointments, acceptedAppointments, declinedAppointments;
    private AppointmentAdapter adapterToday, adapterPending, adapterAccepted, adapterDeclined;
    private AppointmentService appointmentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_manager);

        todayAppointments = findViewById(R.id.rvAppointmentsToday);
        requestAppointments = findViewById(R.id.rvAppointmentRequests);
        acceptedAppointments = findViewById(R.id.rvAcceptedAppointments);
        declinedAppointments = findViewById(R.id.rvDeclinedAppointments);

        todayAppointments.setLayoutManager(new LinearLayoutManager(this));
        requestAppointments.setLayoutManager(new LinearLayoutManager(this));
        acceptedAppointments.setLayoutManager(new LinearLayoutManager(this));
        declinedAppointments.setLayoutManager(new LinearLayoutManager(this));

        adapterToday = new AppointmentAdapter(new ArrayList<>(), false);
        adapterPending = new AppointmentAdapter(new ArrayList<>(), false);
        adapterAccepted = new AppointmentAdapter(new ArrayList<>(), false);
        adapterDeclined = new AppointmentAdapter(new ArrayList<>(), false);

        todayAppointments.setAdapter(adapterToday);
        requestAppointments.setAdapter(adapterPending);
        acceptedAppointments.setAdapter(adapterAccepted);
        declinedAppointments.setAdapter(adapterDeclined);

        appointmentService = new AppointmentService();

        loadDoctorAppointments();
    }

    private void loadDoctorAppointments() {
        String doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        appointmentService.fetchAppointmentsForDoctor(doctorId, "today", adapterToday);
        appointmentService.fetchAppointmentsForDoctor(doctorId, "pending", adapterPending);
        appointmentService.fetchAppointmentsForDoctor(doctorId, "accepted", adapterAccepted);
        appointmentService.fetchAppointmentsForDoctor(doctorId, "declined", adapterDeclined);
    }
}
