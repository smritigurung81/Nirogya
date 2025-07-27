package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvSensorVitals, tvDaysRemaining,
            tvAcceptedAppointments, tvDeclinedAppointments;
    private ImageButton btnChatbot;
    private TextView tvBP, tvHR, tvOxygen, tvTemp, tvDoctorNotes;
    private LinearLayout labReportsContainer;
    private Button btnBookAppointment, btnLogoutPatient;
    private ImageView imgLogo;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference realtimeDb;
    private String currentUserId, linkedDoctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        initializeViews();
        ImageButton btnChatbot = findViewById(R.id.btnChatbot);
        btnChatbot.setOnTouchListener((v, event) -> false);
        // Add chatbot button listener here:
        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(PatientDashboardActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        btnChatbot.startAnimation(pulse);


        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            fetchUserAndProceed();
            loadSensorVitals();
            loadLabReports();
            setupBookAppointment();
            setupLogout();
        }
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSensorVitals = findViewById(R.id.tvSensorVitals);
        tvDaysRemaining = findViewById(R.id.tvDaysRemaining);
        tvAcceptedAppointments = findViewById(R.id.tvAcceptedAppointments);
        tvDeclinedAppointments = findViewById(R.id.tvDeclinedAppointments);
        labReportsContainer = findViewById(R.id.labReportsContainer);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnLogoutPatient = findViewById(R.id.btnLogoutPatient);
        imgLogo = findViewById(R.id.imgLogo);
        tvBP = findViewById(R.id.tvBP);
        tvHR = findViewById(R.id.tvHR);
        tvOxygen = findViewById(R.id.tvOxygen);
        tvTemp = findViewById(R.id.tvTemp);
        tvDoctorNotes = findViewById(R.id.tvDoctorNotes);
        btnChatbot = findViewById(R.id.btnChatbot);
    }

    private void fetchUserAndProceed() {
        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    String firstName = snapshot.getString("firstName");
                    String lastName = snapshot.getString("lastName");
                    linkedDoctorId = snapshot.getString("linkedDoctorId");

                    tvWelcome.setText("Welcome, " + firstName + " " + lastName + "!");

                    if (linkedDoctorId != null && !linkedDoctorId.isEmpty()) {
                        loadDoctorVitals();
                        loadAppointments();
                        loadDaysRemaining();
                    }
                });
    }

    private void loadSensorVitals() {
        tvSensorVitals.setText("Fetching sensor data...");
        realtimeDb.child("sensor").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String heartRate = String.valueOf(snapshot.child("heartRate").getValue());
                    String spo2 = String.valueOf(snapshot.child("spo2").getValue());
                    String temperature = String.valueOf(snapshot.child("temperature_C").getValue());

                    String formatted = "Heart Rate: " + heartRate + " bpm\n"
                            + "SpO₂: " + spo2 + "%\n"
                            + "Body Temperature: " + temperature + " °C";
                    tvSensorVitals.setText(formatted);
                } else {
                    tvSensorVitals.setText("No sensor data found.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvSensorVitals.setText("Error fetching sensor data.");
            }
        });
    }

    private void loadDoctorVitals() {
        firestore.collection("users").document(currentUserId)
                .collection("doctor_vitals")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        Map<String, Object> vitals = (Map<String, Object>) doc.get("vitals");
                        Map<String, Object> soap = (Map<String, Object>) doc.get("soap");

                        tvBP.setText("BP: " + vitals.get("systolic") + "/" + vitals.get("diastolic") + " mmHg");
                        tvHR.setText("Heart Rate: " + vitals.get("heartRate") + " bpm");
                        tvOxygen.setText("SpO₂: " + vitals.get("oxygen") + "%");
                        tvTemp.setText("Temperature: " + vitals.get("temperature") + " °C");

                        SpannableStringBuilder builder = new SpannableStringBuilder();
                        String[] labels = {"Subjective", "Objective", "Assessment", "Plan"};
                        for (String label : labels) {
                            int start = builder.length();
                            builder.append(label + ": ");
                            builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            builder.append(soap.get(label.toLowerCase()) + "\n\n");
                        }
                        tvDoctorNotes.setText(builder);
                    }
                })
                .addOnFailureListener(e -> tvDoctorNotes.setText("Error loading doctor notes."));
    }

    private void loadLabReports() {
        labReportsContainer.removeAllViews();

        firestore.collection("lab_reports")
                .whereEqualTo("patientId", currentUserId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        View card = getLayoutInflater().inflate(R.layout.item_lab_report, labReportsContainer, false);

                        TextView tvReportTitle = card.findViewById(R.id.tvReportTitle);
                        TextView tvPatientName = card.findViewById(R.id.tvPatientName);
                        TextView tvReportType = card.findViewById(R.id.tvReportType);
                        TextView tvTechnician = card.findViewById(R.id.tvTechnicianName);
                        TextView tvRemarks = card.findViewById(R.id.tvRemarks);
                        LinearLayout layoutDetails = card.findViewById(R.id.layoutReportDetails);
                        LinearLayout rowContainer = card.findViewById(R.id.containerReportRows);

                        String type = doc.getString("reportType");
                        String title = doc.getString("reportTitle");

                        tvReportTitle.setText(title);
                        tvPatientName.setText("Patient: " + doc.getString("patientName"));
                        tvReportType.setText("Type: " + type);
                        tvTechnician.setText("By: " + doc.getString("technicianName"));
                        layoutDetails.setVisibility(View.GONE); // initially collapsed

                        switch (type) {
                            case "CBC":
                                addTableRow(rowContainer, "Hemoglobin", doc.getString("hemoglobin"), "12–16 g/dL");
                                addTableRow(rowContainer, "WBC", doc.getString("wbc"), "4–11 x10⁹/L");
                                addTableRow(rowContainer, "Platelets", doc.getString("platelets"), "150–450 x10⁹/L");
                                break;
                            case "Lipid Profile":
                                addTableRow(rowContainer, "HDL", doc.getString("hdl"), ">40 mg/dL");
                                addTableRow(rowContainer, "LDL", doc.getString("ldl"), "<100 mg/dL");
                                addTableRow(rowContainer, "Triglycerides", doc.getString("triglycerides"), "<150 mg/dL");
                                break;
                            case "Blood Sugar":
                                addTableRow(rowContainer, "Fasting Sugar", doc.getString("fastingSugar"), "70–100 mg/dL");
                                addTableRow(rowContainer, "Post Sugar", doc.getString("postSugar"), "<140 mg/dL");
                                addTableRow(rowContainer, "HbA1c", doc.getString("hba1c"), "<5.7%");
                                break;
                        }

                        String remarks = doc.getString("remarks");
                        tvRemarks.setText("Remarks: " + (remarks != null ? remarks : "None"));

                        card.setOnClickListener(v -> {
                            if (layoutDetails.getVisibility() == View.VISIBLE) {
                                layoutDetails.setVisibility(View.GONE);
                            } else {
                                layoutDetails.setVisibility(View.VISIBLE);
                            }
                        });

                        labReportsContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView error = new TextView(this);
                    error.setText("Error loading lab reports.");
                    labReportsContainer.addView(error);
                });
    }

    private void addTableRow(LinearLayout container, String test, String value, String normal) {
        View row = getLayoutInflater().inflate(R.layout.item_lab_report_row, container, false);
        ((TextView) row.findViewById(R.id.tvTestName)).setText(test);
        ((TextView) row.findViewById(R.id.tvTestValue)).setText(value);
        ((TextView) row.findViewById(R.id.tvTestNormal)).setText(normal);
        container.addView(row);
    }

    private void loadAppointments() {
        firestore.collection("users").document(linkedDoctorId)
                .collection("appointments")
                .whereEqualTo("patientId", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    StringBuilder accepted = new StringBuilder();
                    StringBuilder declined = new StringBuilder();
                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String entry = date + " at " + time + "\n";

                        if ("accepted".equalsIgnoreCase(status)) accepted.append(entry);
                        else if ("declined".equalsIgnoreCase(status)) declined.append(entry);
                    }
                    tvAcceptedAppointments.setText(accepted.length() > 0 ? accepted : "None");
                    tvDeclinedAppointments.setText(declined.length() > 0 ? declined : "None");
                });
    }

    private void loadDaysRemaining() {
        firestore.collection("users").document(linkedDoctorId)
                .collection("appointments")
                .whereEqualTo("patientId", currentUserId)
                .whereEqualTo("status", "accepted")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Timestamp ts = snapshots.getDocuments().get(0).getTimestamp("timestamp");
                        if (ts != null) {
                            long days = TimeUnit.MILLISECONDS.toDays(ts.toDate().getTime() - new Date().getTime());
                            tvDaysRemaining.setText(days + " days left until your next appointment");
                        }
                    } else {
                        tvDaysRemaining.setText("No upcoming appointment found.");
                    }
                });
    }

    private void setupBookAppointment() {
        btnBookAppointment.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                cal.set(y, m, d);
                new TimePickerDialog(this, (tView, h, min) -> {
                    cal.set(Calendar.HOUR_OF_DAY, h);
                    cal.set(Calendar.MINUTE, min);

                    Map<String, Object> data = new HashMap<>();
                    data.put("patientId", currentUserId);
                    data.put("status", "pending");
                    data.put("timestamp", cal.getTime());
                    data.put("date", new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime()));
                    data.put("time", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime()));

                    firestore.collection("users")
                            .document(linkedDoctorId)
                            .collection("appointments")
                            .add(data)
                            .addOnSuccessListener(r -> Toast.makeText(this, "Appointment requested.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Booking failed.", Toast.LENGTH_SHORT).show());

                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupLogout() {
        btnLogoutPatient.setOnClickListener(v -> {
            firebaseAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
