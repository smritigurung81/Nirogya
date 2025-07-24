package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.UUID;

public class LabTechnicianDashboardActivity extends AppCompatActivity {

    private EditText etPatientId, etReportTitle, etTechnicianName, etTechnicianId;

    private EditText etHemoglobin, etWBC, etPlatelets;
    private EditText etHDL, etLDL, etTriglycerides;
    private EditText etFasting, etPost, etHbA1c;

    private Button btnUploadReport, btnLogout, btnViewReports;
    private Spinner spinnerReportType;
    private LinearLayout layoutCBC, layoutLipid, layoutSugar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_technician_dashboard);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etPatientId = findViewById(R.id.etPatientId);
        etReportTitle = findViewById(R.id.etReportTitle);
        etTechnicianName = findViewById(R.id.etTechnicianName);
        etTechnicianId = findViewById(R.id.etTechnicianId);

        etHemoglobin = findViewById(R.id.etHemoglobin);
        etWBC = findViewById(R.id.etWBC);
        etPlatelets = findViewById(R.id.etPlatelets);

        etHDL = findViewById(R.id.etHDL);
        etLDL = findViewById(R.id.etLDL);
        etTriglycerides = findViewById(R.id.etTriglycerides);

        etFasting = findViewById(R.id.etFasting);
        etPost = findViewById(R.id.etPost);
        etHbA1c = findViewById(R.id.etHbA1c);

        btnUploadReport = findViewById(R.id.btnUploadReport);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewReports = findViewById(R.id.btnViewReports);

        spinnerReportType = findViewById(R.id.spinnerReportType);
        layoutCBC = findViewById(R.id.layoutCbcForm);
        layoutLipid = findViewById(R.id.layoutLipidForm);
        layoutSugar = findViewById(R.id.layoutSugarForm);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.lab_report_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportType.setAdapter(adapter);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnUploadReport.setOnClickListener(v -> saveReportDataToFirestore());

        btnViewReports.setOnClickListener(v ->
                startActivity(new Intent(this, MyReportsActivity.class)));

        spinnerReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                layoutCBC.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                layoutLipid.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                layoutSugar.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void saveReportDataToFirestore() {
        String patientUid = etPatientId.getText().toString().trim();
        String reportTitle = etReportTitle.getText().toString().trim();
        String techName = etTechnicianName.getText().toString().trim();
        String techId = etTechnicianId.getText().toString().trim();
        int type = spinnerReportType.getSelectedItemPosition();

        if (TextUtils.isEmpty(patientUid) || TextUtils.isEmpty(reportTitle)
                || TextUtils.isEmpty(techName) || TextUtils.isEmpty(techId)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(patientUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Patient ID not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String patientName = documentSnapshot.getString("firstName") + " " +
                            documentSnapshot.getString("lastName");

                    HashMap<String, Object> reportData = new HashMap<>();
                    reportData.put("patientId", patientUid);
                    reportData.put("patientName", patientName);
                    reportData.put("reportTitle", reportTitle);
                    reportData.put("technicianName", techName);
                    reportData.put("technicianId", techId);
                    reportData.put("timestamp", System.currentTimeMillis());

                    String remark = "";

                    switch (type) {
                        case 0:
                            reportData.put("reportType", "CBC");
                            String h = etHemoglobin.getText().toString().trim();
                            String w = etWBC.getText().toString().trim();
                            String p = etPlatelets.getText().toString().trim();
                            reportData.put("hemoglobin", h);
                            reportData.put("wbc", w);
                            reportData.put("platelets", p);
                            remark = evaluateCBC(h, w, p);
                            break;

                        case 1:
                            reportData.put("reportType", "Lipid Profile");
                            String hdl = etHDL.getText().toString().trim();
                            String ldl = etLDL.getText().toString().trim();
                            String tri = etTriglycerides.getText().toString().trim();
                            reportData.put("hdl", hdl);
                            reportData.put("ldl", ldl);
                            reportData.put("triglycerides", tri);
                            remark = evaluateLipid(hdl, ldl, tri);
                            break;

                        case 2:
                            reportData.put("reportType", "Blood Sugar");
                            String fast = etFasting.getText().toString().trim();
                            String post = etPost.getText().toString().trim();
                            String a1c = etHbA1c.getText().toString().trim();
                            reportData.put("fastingSugar", fast);
                            reportData.put("postSugar", post);
                            reportData.put("hba1c", a1c);
                            remark = evaluateSugar(fast, post, a1c);
                            break;
                    }

                    reportData.put("remarks", remark);

                    firestore.collection("lab_reports")
                            .document(UUID.randomUUID().toString())
                            .set(reportData)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Report uploaded successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch patient", Toast.LENGTH_SHORT).show());
    }

    private String evaluateCBC(String h, String w, String p) {
        StringBuilder remark = new StringBuilder();
        try {
            double hb = Double.parseDouble(h);
            double wb = Double.parseDouble(w);
            double pt = Double.parseDouble(p);
            if (hb < 13 || hb > 17) remark.append("Hemoglobin abnormal. ");
            if (wb < 4 || wb > 11) remark.append("WBC abnormal. ");
            if (pt < 150 || pt > 400) remark.append("Platelets abnormal. ");
        } catch (Exception e) {
            remark.append("Invalid CBC values.");
        }
        return remark.length() == 0 ? "CBC within normal range." : remark.toString();
    }

    private String evaluateLipid(String hdl, String ldl, String tri) {
        StringBuilder remark = new StringBuilder();
        try {
            double h = Double.parseDouble(hdl);
            double l = Double.parseDouble(ldl);
            double t = Double.parseDouble(tri);
            if (h < 40) remark.append("Low HDL. ");
            if (l > 100) remark.append("High LDL. ");
            if (t > 150) remark.append("High Triglycerides. ");
        } catch (Exception e) {
            remark.append("Invalid Lipid Profile values.");
        }
        return remark.length() == 0 ? "Lipid profile normal." : remark.toString();
    }

    private String evaluateSugar(String f, String p, String a) {
        StringBuilder remark = new StringBuilder();
        try {
            double fast = Double.parseDouble(f);
            double post = Double.parseDouble(p);
            double hb = Double.parseDouble(a);
            if (fast < 70 || fast > 100) remark.append("Abnormal fasting sugar. ");
            if (post > 140) remark.append("High postprandial sugar. ");
            if (hb > 5.7) remark.append("High HbA1c. ");
        } catch (Exception e) {
            remark.append("Invalid Blood Sugar values.");
        }
        return remark.length() == 0 ? "Blood sugar levels normal." : remark.toString();
    }
}
