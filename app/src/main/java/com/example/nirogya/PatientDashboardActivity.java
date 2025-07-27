package com.example.nirogya;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.example.nirogya.models.LabReport;


public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvAppointmentReminder;
    private TextView tvBP, tvHR, tvOxygen, tvTemp, tvDoctorNotes;
    // Sensor vitals TextViews (matching XML IDs)
    private TextView tvSensorHR, tvSensorSpO2, tvSensorTemp;
    private LinearLayout labReportsContainer;
    private LinearLayout todaysAppointmentsContainer, acceptedAppointmentsContainer, declinedAppointmentsContainer, layoutReminder;
    private Button btnBookAppointment, btnLogoutPatient;
    private ImageView imgLogo;

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
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance("https://nirogya-8c9f8-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        Log.d("FirebaseInitCheck", "Realtime DB Instance: " + FirebaseDatabase.getInstance());
        Log.d("FirebaseInitCheck", "DB Reference URL: " + FirebaseDatabase.getInstance().getReference().toString());
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

        // Doctor's vitals section
        tvBP = findViewById(R.id.tvBP);
        tvHR = findViewById(R.id.tvHR);
        tvOxygen = findViewById(R.id.tvOxygen);
        tvTemp = findViewById(R.id.tvTemp);
        tvDoctorNotes = findViewById(R.id.tvDoctorNotes);

        // Sensor vitals section (matching XML IDs)
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
        // Set placeholders
        tvSensorHR.setText("Heart Rate: -- bpm");
        tvSensorSpO2.setText("SpO₂: --%");
        tvSensorTemp.setText("Temperature: -- °C");

        realtimeDb.child("sensor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("SensorData", ">>> Entered onDataChange()");

                if (snapshot.exists()) {
                    try {
                        Object hrObj = snapshot.child("heartRate").getValue();
                        Object spo2Obj = snapshot.child("spo2").getValue();
                        Object tempObj = snapshot.child("temperature_C").getValue();

                        Log.d("SensorData", "Raw values: HR=" + hrObj + ", SPO2=" + spo2Obj + ", Temp=" + tempObj);

                        if (hrObj != null) {
                            float heartRate = ((Number) hrObj).floatValue();
                            tvSensorHR.setText("Heart Rate: " + String.format(Locale.getDefault(), "%.1f", heartRate) + " bpm");
                        }

                        if (spo2Obj != null) {
                            float spo2 = ((Number) spo2Obj).floatValue();
                            tvSensorSpO2.setText("SpO₂: " + String.format(Locale.getDefault(), "%.1f", spo2) + " %");
                        }

                        if (tempObj != null) {
                            float temp = ((Number) tempObj).floatValue();
                            tvSensorTemp.setText("Temperature: " + String.format(Locale.getDefault(), "%.1f", temp) + " °C");
                        }

                    } catch (Exception e) {
                        Log.e("SensorData", "Data parsing error: " + e.getMessage());
                        tvSensorHR.setText("Heart Rate: Error");
                        tvSensorSpO2.setText("SpO₂: Error");
                        tvSensorTemp.setText("Temperature: Error");
                    }
                } else {
                    tvSensorHR.setText("Heart Rate: No data");
                    tvSensorSpO2.setText("SpO₂: No data");
                    tvSensorTemp.setText("Temperature: No data");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("SensorData", "Firebase Error: " + error.getMessage());
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

                        // Update doctor's vitals section
                        if (vitals != null) {
                            tvBP.setText(vitals.get("systolic") + "/" + vitals.get("diastolic") + " mmHg");

                            if (vitals.get("heartRate") != null) {
                                tvHR.setText(vitals.get("heartRate") + " bpm");
                            }
                            if (vitals.get("oxygen") != null) {
                                tvOxygen.setText(vitals.get("oxygen") + " %");
                            }
                            if (vitals.get("temperature") != null) {
                                tvTemp.setText(vitals.get("temperature") + " °C");
                            }
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
                })
                .addOnFailureListener(e -> {
                    tvDoctorNotes.setText("Subjective: --\nObjective: --\nAssessment: --\nPlan: --");
                    Log.e("DoctorVitals", "Error loading doctor vitals: " + e.getMessage());
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

                        View card = getLayoutInflater().inflate(R.layout.item_lab_report, labReportsContainer, false);

                        ((TextView) card.findViewById(R.id.tvReportTitle)).setText(report.getReportTitle());
                        ((TextView) card.findViewById(R.id.tvPatientName)).setText("Patient: " + report.getPatientName());
                        ((TextView) card.findViewById(R.id.tvReportType)).setText("Type: " + report.getReportType());
                        ((TextView) card.findViewById(R.id.tvTechnicianName)).setText("By: " + report.getTechnicianName());

                        // Remarks handling
                        String remarks = report.getRemarks();
                        if (remarks == null || remarks.trim().isEmpty() || "null".equalsIgnoreCase(remarks.trim())) {
                            remarks = report.generateRemark();
                        }

                        ((TextView) card.findViewById(R.id.tvRemarks)).setText("Remarks: " + remarks);

                        // Test data rows
                        LinearLayout rowContainer = card.findViewById(R.id.containerReportRows);
                        String type = report.getReportType();

                        if ("CBC".equalsIgnoreCase(type)) {
                            addTableRow(rowContainer, "Hemoglobin", report.getHemoglobin(), "12–16 g/dL");
                            addTableRow(rowContainer, "WBC", report.getWbc(), "4–11 x10⁹/L");
                            addTableRow(rowContainer, "Platelets", report.getPlatelets(), "150–400 x10⁹/L");

                        } else if ("Lipid Profile".equalsIgnoreCase(type) || "Lipid Test".equalsIgnoreCase(type)) {
                            addTableRow(rowContainer, "HDL", report.getHdl(), "> 60 mg/dL");
                            addTableRow(rowContainer, "LDL", report.getLdl(), "< 100 mg/dL");
                            addTableRow(rowContainer, "Triglycerides", report.getTriglycerides(), "< 150 mg/dL");

                        } else if ("Blood Sugar".equalsIgnoreCase(type)) {
                            addTableRow(rowContainer, "Fasting Sugar", report.getFastingSugar(), "< 100 mg/dL");
                            addTableRow(rowContainer, "Postprandial Sugar", report.getPostSugar(), "< 140 mg/dL");
                            addTableRow(rowContainer, "HbA1c", report.getHba1c(), "< 5.7%");
                        }

                        // Expand/collapse toggle
                        card.setOnClickListener(v -> {
                            View details = card.findViewById(R.id.layoutReportDetails);
                            details.setVisibility(details.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                        });

                        labReportsContainer.addView(card);
                    }
                });
    }

    private void loadAppointments() {
        acceptedAppointmentsContainer.removeAllViews();
        declinedAppointmentsContainer.removeAllViews();
        todaysAppointmentsContainer.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        firestore.collection("appointments")
                .whereEqualTo("patientId", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        String patientName = doc.getString("patientName");
                        String date = doc.getString("date");
                        String time = doc.getString("time");

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

                    // 🔔 Reminder logic — only for upcoming appointments
                    firestore.collection("appointments")
                            .whereEqualTo("patientId", currentUserId)
                            .whereEqualTo("status", "accepted")
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener(reminderSnapshots -> {
                                Calendar todayCal = Calendar.getInstance();
                                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                                todayCal.set(Calendar.MINUTE, 0);
                                todayCal.set(Calendar.SECOND, 0);
                                todayCal.set(Calendar.MILLISECOND, 0);

                                for (DocumentSnapshot doc : reminderSnapshots) {
                                    Timestamp ts = doc.getTimestamp("timestamp");
                                    if (ts == null) continue;

                                    Calendar appointmentCal = Calendar.getInstance();
                                    appointmentCal.setTime(ts.toDate());
                                    appointmentCal.set(Calendar.HOUR_OF_DAY, 0);
                                    appointmentCal.set(Calendar.MINUTE, 0);
                                    appointmentCal.set(Calendar.SECOND, 0);
                                    appointmentCal.set(Calendar.MILLISECOND, 0);

                                    long diffMillis = appointmentCal.getTimeInMillis() - todayCal.getTimeInMillis();
                                    long daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis);

                                    if (daysLeft >= 0) {
                                        layoutReminder.setVisibility(View.VISIBLE);
                                        if (daysLeft == 0) {
                                            tvAppointmentReminder.setText("Reminder: Appointment is today!");
                                        } else {
                                            tvAppointmentReminder.setText("Reminder: Appointment after " + daysLeft + " day" + (daysLeft == 1 ? "" : "s"));
                                        }
                                        Animation blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink);
                                        layoutReminder.startAnimation(blinkAnim);
                                        return;
                                    }
                                }

                                layoutReminder.setVisibility(View.GONE);
                            });
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
                            .addOnSuccessListener(doctorSnapshot -> {
                                String doctorName = doctorSnapshot.getString("firstName") + " " + doctorSnapshot.getString("lastName");

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

    private void addTableRow(LinearLayout container, String test, String value, String normal) {
        View row = getLayoutInflater().inflate(R.layout.item_lab_report_row, container, false);
        ((TextView) row.findViewById(R.id.tvTestName)).setText(test);
        ((TextView) row.findViewById(R.id.tvTestValue)).setText(value);
        ((TextView) row.findViewById(R.id.tvTestNormal)).setText(normal);
        container.addView(row);
    }
}