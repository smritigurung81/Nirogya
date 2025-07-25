package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nirogya.models.DoctorVitalsModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Map;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvPatientVitals, tvDoctorNotes;
    private Button btnAddVitals, btnBookAppointment, btnLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseReference rtdbRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = user.getUid();
        rtdbRef = FirebaseDatabase.getInstance().getReference("vitals_user").child(currentUserId);

        initializeViews();
        setupButtonListeners();
        loadWelcomeMessage();
        listenToRealtimeVitals();
        loadDoctorVitals();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcomePatient);
        tvPatientVitals = findViewById(R.id.tvPatientVitals);
        tvDoctorNotes = findViewById(R.id.tvDoctorNotes);
        btnAddVitals = findViewById(R.id.btnAddVitals);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnLogout = findViewById(R.id.btnLogoutPatient);
    }

    private void setupButtonListeners() {
        btnAddVitals.setOnClickListener(v ->
                Toast.makeText(this, "Add Vitals feature coming soon", Toast.LENGTH_SHORT).show());

        btnBookAppointment.setOnClickListener(v ->
                Toast.makeText(this, "Book Appointment feature coming soon", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadWelcomeMessage() {
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    if (name != null) {
                        tvWelcome.setText("Welcome, " + name);
                    }
                });
    }

    private void listenToRealtimeVitals() {
        rtdbRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Map<String, Object> vitals = (Map<String, Object>) snap.child("vitals").getValue();
                    if (vitals != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Systolic: ").append(vitals.get("systolic")).append("\n");
                        sb.append("Diastolic: ").append(vitals.get("diastolic")).append("\n");
                        sb.append("Heart Rate: ").append(vitals.get("heartrate")).append("\n");
                        sb.append("Oxygen: ").append(vitals.get("oxygen")).append("\n");
                        sb.append("Temperature: ").append(vitals.get("temperature")).append("\n");
                        tvPatientVitals.setText(sb.toString());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PatientDashboardActivity.this, "Failed to load real-time vitals", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDoctorVitals() {
        firestore.collection("users")
                .document(currentUserId)
                .collection("doctor_vitals")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        DoctorVitalsModel model = doc.toObject(DoctorVitalsModel.class);

                        StringBuilder sb = new StringBuilder();
                        if (model != null) {
                            Map<String, String> vitals = model.getVitals();
                            Map<String, String> soap = model.getSoap();

                            if (vitals != null) {
                                sb.append("Systolic: ").append(vitals.get("systolic")).append("\n");
                                sb.append("Diastolic: ").append(vitals.get("diastolic")).append("\n");
                                sb.append("Heart Rate: ").append(vitals.get("heartrate")).append("\n");
                                sb.append("Oxygen: ").append(vitals.get("oxygen")).append("\n");
                                sb.append("Temperature: ").append(vitals.get("temperature")).append("\n");
                            }

                            if (soap != null) {
                                sb.append("Subjective: ").append(soap.get("subjective")).append("\n");
                                sb.append("Objective: ").append(soap.get("objective")).append("\n");
                                sb.append("Assessment: ").append(soap.get("assessment")).append("\n");
                                sb.append("Plan: ").append(soap.get("plan")).append("\n");
                            }
                        }

                        tvDoctorNotes.setText(sb.toString());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load doctor vitals", Toast.LENGTH_SHORT).show());
    }
}
