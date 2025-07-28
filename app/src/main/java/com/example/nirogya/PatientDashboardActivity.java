package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nirogya.models.LabReport;
import com.google.firebase.Timestamp;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvAppointmentReminder;
    private TextView tvBP, tvHR, tvOxygen, tvTemp, tvDoctorNotes;
    private TextView tvSensorHR, tvSensorSpO2, tvSensorTemp;
    private LinearLayout labReportsContainer;
    private LinearLayout todaysAppointmentsContainer, acceptedAppointmentsContainer, declinedAppointmentsContainer, layoutReminder;
    private Button btnBookAppointment, btnLogoutPatient;
    private ImageView imgLogo;
    private ImageButton btnChatbot;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference realtimeDb;

    private String currentUserId;
    private String linkedDoctorId;
    private String patientFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        initializeViews();

        btnChatbot.setOnClickListener(v -> {
            startActivity(new Intent(PatientDashboardActivity.this, ChatbotActivity.class));
        });
        btnChatbot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance("https://nirogya-8c9f8-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            fetchUserAndProceed();
            loadSensorVitals();
            loadLabReports();
            setupLogout();
        }
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAppointmentReminder = findViewById(R.id.tvAppointmentReminder);
        layoutReminder = findViewById(R.id.layoutReminder);
        labReportsContainer = findViewById(R.id.labReportsContainer);
        todaysAppointmentsContainer = findViewById(R.id.todaysAppointmentsContainer);
        acceptedAppointmentsContainer = findViewById(R.id.acceptedAppointmentsContainer);
        declinedAppointmentsContainer = findViewById(R.id.declinedAppointmentsContainer);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnLogoutPatient = findViewById(R.id.btnLogoutPatient);
        imgLogo = findViewById(R.id.imgLogo);
        btnChatbot = findViewById(R.id.btnChatbot);

        tvBP = findViewById(R.id.tvBP);
        tvHR = findViewById(R.id.tvHR);
        tvOxygen = findViewById(R.id.tvOxygen);
        tvTemp = findViewById(R.id.tvTemp);
        tvDoctorNotes = findViewById(R.id.tvDoctorNotes);

        tvSensorHR = findViewById(R.id.tvSensorHR);
        tvSensorSpO2 = findViewById(R.id.tvSensorSpO2);
        tvSensorTemp = findViewById(R.id.tvSensorTemp);
    }

    private void fetchUserAndProceed() {
        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    String firstName = snapshot.getString("firstName");
                    String lastName = snapshot.getString("lastName");
                    patientFullName = firstName + " " + lastName;
                    linkedDoctorId = snapshot.getString("linkedDoctorId");

                    tvWelcome.setText("Welcome, " + patientFullName + "!");

                    if (linkedDoctorId != null && !linkedDoctorId.isEmpty()) {
                        loadDoctorVitals();
                        loadAppointments();
                        setupBookAppointment();
                    }
                });
    }

    private void loadSensorVitals() {
        tvSensorHR.setText("Heart Rate: -- bpm");
        tvSensorSpO2.setText("SpO₂: --%");
        tvSensorTemp.setText("Temperature: -- °C");

        realtimeDb.child("sensor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        float heartRate = snapshot.child("heartRate").getValue(Float.class);
                        float spo2 = snapshot.child("spo2").getValue(Float.class);
                        float temp = snapshot.child("temperature_C").getValue(Float.class);

                        tvSensorHR.setText(String.format(Locale.getDefault(), "Heart Rate: %.1f bpm", heartRate));
                        tvSensorSpO2.setText(String.format(Locale.getDefault(), "SpO₂: %.1f %%", spo2));
                        tvSensorTemp.setText(String.format(Locale.getDefault(), "Temperature: %.1f °C", temp));
                    }
                } catch (Exception e) {
                    tvSensorHR.setText("Heart Rate: Error");
                    tvSensorSpO2.setText("SpO₂: Error");
                    tvSensorTemp.setText("Temperature: Error");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvSensorHR.setText("Heart Rate: Error");
                tvSensorSpO2.setText("SpO₂: Error");
                tvSensorTemp.setText("Temperature: Error");
            }
        });
    }

    private void loadDoctorVitals() {
        firestore.collection("users").document(currentUserId)
                .collection("doctor_vitals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        Map<String, Object> vitals = (Map<String, Object>) doc.get("vitals");
                        Map<String, Object> soap = (Map<String, Object>) doc.get("soap");

                        if (vitals != null) {
                            tvBP.setText(vitals.get("systolic") + "/" + vitals.get("diastolic") + " mmHg");
                            tvHR.setText(vitals.get("heartRate") + " bpm");
                            tvOxygen.setText(vitals.get("oxygen") + " %");
                            tvTemp.setText(vitals.get("temperature") + " °C");
                        }

                        if (soap != null) {
                            SpannableStringBuilder builder = new SpannableStringBuilder();
                            String[] labels = {"Subjective", "Objective", "Assessment", "Plan"};
                            for (String label : labels) {
                                int start = builder.length();
                                builder.append(label + ": ");
                                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.append(soap.get(label.toLowerCase()) + "\n");
                            }
                            tvDoctorNotes.setText(builder);
                        }
                    }
                });
    }

    private void loadLabReports() {
        labReportsContainer.removeAllViews();

        firestore.collection("lab_reports")
                .whereEqualTo("patientId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        LabReport report = doc.toObject(LabReport.class);
                        if (report == null) continue;
                        report.setId(doc.getId());

                        View card = getLayoutInflater().inflate(R.layout.item_lab_report, labReportsContainer, false);
                        LinearLayout container = card.findViewById(R.id.containerReportRows);
                        LinearLayout layoutDetails = card.findViewById(R.id.layoutReportDetails);
                        layoutDetails.setVisibility(View.GONE);

                        ((TextView) card.findViewById(R.id.tvReportTitle)).setText(report.getReportTitle());
                        ((TextView) card.findViewById(R.id.tvPatientName)).setText("Patient: " + report.getPatientName());
                        ((TextView) card.findViewById(R.id.tvReportType)).setText("Type: " + report.getReportType());
                        ((TextView) card.findViewById(R.id.tvTechnicianName)).setText("By: " + report.getTechnicianName());

                        card.setOnClickListener(v -> {
                            layoutDetails.setVisibility(
                                    layoutDetails.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
                            );
                        });

                        List<String> high = new ArrayList<>();
                        List<String> low = new ArrayList<>();

                        if ("CBC".equals(report.getReportType())) {
                            addTestRow(container, "Hemoglobin", report.getHemoglobin(), "12–16 g/dL", 12f, 16f, low, high);
                            addTestRow(container, "WBC", report.getWbc(), "4,000–11,000 /µL", 4000f, 11000f, low, high);
                            addTestRow(container, "Platelets", report.getPlatelets(), "150K–450K /µL", 150000f, 450000f, low, high);
                        } else if ("Lipid Profile".equals(report.getReportType())) {
                            addTestRow(container, "HDL", report.getHdl(), "≥ 40 mg/dL", 40f, null, low, high);
                            addTestRow(container, "LDL", report.getLdl(), "< 100 mg/dL", null, 100f, low, high);
                            addTestRow(container, "Triglycerides", report.getTriglycerides(), "< 150 mg/dL", null, 150f, low, high);
                        } else if ("Blood Sugar".equals(report.getReportType())) {
                            addTestRow(container, "Fasting Sugar", report.getFastingSugar(), "70–99 mg/dL", 70f, 99f, low, high);
                            addTestRow(container, "Postprandial Sugar", report.getPostSugar(), "< 140 mg/dL", null, 140f, low, high);
                            addTestRow(container, "HbA1c", report.getHba1c(), "< 5.7%", null, 5.7f, low, high);
                        }

                        String remarks = "";
                        if (!high.isEmpty()) remarks += "High: " + TextUtils.join(", ", high);
                        if (!low.isEmpty()) {
                            if (!remarks.isEmpty()) remarks += " | ";
                            remarks += "Low: " + TextUtils.join(", ", low);
                        }
                        if (remarks.isEmpty()) remarks = "All values within normal range";

                        ((TextView) card.findViewById(R.id.tvRemarks)).setText("Remarks: " + remarks);
                        report.setRemarks(remarks);
                        firestore.collection("lab_reports").document(report.getId()).update("remarks", remarks);

                        labReportsContainer.addView(card);
                    }
                });
    }

    private void addTestRow(LinearLayout container, String name, String value, String range,
                            Float lowThreshold, Float highThreshold, List<String> lows, List<String> highs) {

        View row = getLayoutInflater().inflate(R.layout.item_lab_report_row, container, false);
        TextView tvName = row.findViewById(R.id.tvTestName);
        TextView tvVal = row.findViewById(R.id.tvTestValue);
        TextView tvRange = row.findViewById(R.id.tvTestNormal);

        tvName.setText(name);
        tvVal.setText(value != null ? value : "--");
        tvRange.setText(range);

        // Always show text in black
        tvName.setTextColor(Color.BLACK);
        tvVal.setTextColor(Color.BLACK);
        tvRange.setTextColor(Color.BLACK);

        try {
            float val = parseFloat(value);

            if (highThreshold != null && val > highThreshold) {
                highs.add(name);
            } else if (lowThreshold != null && val < lowThreshold) {
                lows.add(name);
            }
            // No color applied — logic is only for remark generation
        } catch (Exception e) {
            // silently ignore parsing error
        }

        container.addView(row);
    }
    private float parseFloat(String s) {
        if (s == null) return 0f;
        return Float.parseFloat(s.replaceAll("[^\\d.]", ""));
    }


    private void loadAppointments() {
        acceptedAppointmentsContainer.removeAllViews();
        declinedAppointmentsContainer.removeAllViews();
        todaysAppointmentsContainer.removeAllViews();

        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        firestore.collection("appointments")
                .whereEqualTo("patientId", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String patientName = doc.getString("patientName");

                        View card = getLayoutInflater().inflate(R.layout.item_appointment_card, acceptedAppointmentsContainer, false);
                        ((TextView) card.findViewById(R.id.tvPatientName)).setText("Patient: " + patientName);
                        ((TextView) card.findViewById(R.id.tvAppointmentDate)).setText("Date: " + date);
                        ((TextView) card.findViewById(R.id.tvAppointmentTime)).setText("Time: " + time);

                        if (date.equals(today)) {
                            card.setBackgroundResource(R.drawable.card_background_today);
                            todaysAppointmentsContainer.addView(card);
                        } else if ("accepted".equalsIgnoreCase(status)) {
                            card.setBackgroundResource(R.drawable.card_background_appointment_accepted);
                            acceptedAppointmentsContainer.addView(card);
                        } else if ("declined".equalsIgnoreCase(status)) {
                            card.setBackgroundResource(R.drawable.card_background_appointment_declined);
                            declinedAppointmentsContainer.addView(card);
                        }
                    }

                    setupReminder();
                });
    }

    private void setupReminder() {
        firestore.collection("appointments")
                .whereEqualTo("patientId", currentUserId)
                .whereEqualTo("status", "accepted")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    for (DocumentSnapshot doc : snapshots) {
                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts == null) continue;

                        Calendar appDate = Calendar.getInstance();
                        appDate.setTime(ts.toDate());
                        appDate.set(Calendar.HOUR_OF_DAY, 0);
                        appDate.set(Calendar.MINUTE, 0);
                        appDate.set(Calendar.SECOND, 0);
                        appDate.set(Calendar.MILLISECOND, 0);

                        long days = TimeUnit.MILLISECONDS.toDays(appDate.getTimeInMillis() - today.getTimeInMillis());
                        if (days >= 0) {
                            layoutReminder.setVisibility(View.VISIBLE);
                            tvAppointmentReminder.setText(days == 0 ?
                                    "Reminder: Appointment is today!" :
                                    "Reminder: Appointment in " + days + " day" + (days == 1 ? "" : "s"));
                            layoutReminder.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink));
                            return;
                        }
                    }

                    layoutReminder.setVisibility(View.GONE);
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

                    firestore.collection("users").document(linkedDoctorId).get()
                            .addOnSuccessListener(docSnap -> {
                                String doctorName = docSnap.getString("firstName") + " " + docSnap.getString("lastName");

                                Map<String, Object> data = new HashMap<>();
                                data.put("patientId", currentUserId);
                                data.put("doctorId", linkedDoctorId);
                                data.put("patientName", patientFullName);
                                data.put("doctorName", doctorName);
                                data.put("status", "pending");
                                data.put("timestamp", cal.getTime());
                                data.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime()));
                                data.put("time", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime()));

                                firestore.collection("appointments")
                                        .add(data)
                                        .addOnSuccessListener(r -> Toast.makeText(this, "Appointment requested.", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(this, "Booking failed.", Toast.LENGTH_SHORT).show());
                            });
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
