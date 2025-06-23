package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.services.AppointmentService;
import com.example.nirogya.services.LabReportService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class DoctorDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String doctorId;
    private String doctorNMC;

    private RecyclerView rvPatients, rvAppointments;
    private PatientListAdapter patientAdapter;
    private AppointmentAdapter appointmentAdapter;

    private AppointmentService appointmentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvPatients = findViewById(R.id.rvAssignedPatients);
        rvAppointments = findViewById(R.id.rvAppointments);

        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        doctorId = auth.getCurrentUser().getUid();
        appointmentService = new AppointmentService(this, db, doctorId);

        fetchDoctorNMCAndPatients();
        fetchAppointmentRequests();
    }

    private void fetchDoctorNMCAndPatients() {
        db.collection("doctors").document(doctorId)
                .get()
                .addOnSuccessListener(doc -> {
                    doctorNMC = doc.getString("nmcNumber");
                    if (doctorNMC != null) {
                        loadAssignedPatients();
                    } else {
                        Toast.makeText(this, "NMC not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load doctor info", Toast.LENGTH_SHORT).show());
    }

    private void loadAssignedPatients() {
        db.collection("users")
                .whereEqualTo("linkedDoctorNmc", doctorNMC)
                .get()
                .addOnSuccessListener(query -> {
                    List<Patient> patientList = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Patient patient = doc.toObject(Patient.class);
                        if (patient != null) {
                            patient.setUid(doc.getId());
                            patientList.add(patient);
                        }
                    }
                    patientAdapter = new PatientListAdapter(this, patientList);
                    rvPatients.setAdapter(patientAdapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load patients", Toast.LENGTH_SHORT).show());
    }

    private void fetchAppointmentRequests() {
        appointmentService.fetchDoctorAppointments(doctorId, new AppointmentService.AppointmentFetchCallback() {
            @Override
            public void onAppointmentsFetched(List<Appointment> list) {
                appointmentAdapter = new AppointmentAdapter(list, true, (appointment, isAccepted) -> {
                    appointmentService.updateAppointmentStatus(appointment.getId(), isAccepted, new AppointmentService.AppointmentCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(DoctorDashboardActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(DoctorDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                rvAppointments.setAdapter(appointmentAdapter);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DoctorDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
