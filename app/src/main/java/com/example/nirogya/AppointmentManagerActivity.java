package com.example.nirogya;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class AppointmentManagerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String doctorUid;

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_manager);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        doctorUid = mAuth.getCurrentUser().getUid();

        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();
        adapter = new AppointmentAdapter(appointmentList, this::handleAppointmentAction);
        rvAppointments.setAdapter(adapter);

        loadAppointments();
    }

    private void loadAppointments() {
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    appointmentList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Appointment appointment = doc.toObject(Appointment.class);
                        appointment.setId(doc.getId());
                        appointmentList.add(appointment);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void handleAppointmentAction(Appointment appointment, boolean isAccepted) {
        db.collection("appointments")
                .document(appointment.getId())
                .update("status", isAccepted ? "accepted" : "declined")
                .addOnSuccessListener(aVoid -> Toast.makeText(this,
                        "Appointment " + (isAccepted ? "accepted" : "declined"), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

