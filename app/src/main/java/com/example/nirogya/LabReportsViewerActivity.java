package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.firebase.firestore.*;

import java.util.*;

public class LabReportsViewerActivity extends AppCompatActivity {

    RecyclerView rvReports;
    FirebaseFirestore db;
    List<LabReport> reports = new ArrayList<>();
    LabReportAdapter adapter;
    String patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_reports_viewer);

        rvReports = findViewById(R.id.rvUploadedReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        db = FirebaseFirestore.getInstance();

        patientId = getIntent().getStringExtra("patientId");

        db.collection("users").document(patientId).collection("lab_reports")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null || error != null) return;
                    reports.clear();
                    for (DocumentSnapshot snap : value.getDocuments()) {
                        LabReport report = snap.toObject(LabReport.class);
                        reports.add(report);
                    }
                    adapter = new LabReportAdapter(this, reports);
                    rvReports.setAdapter(adapter);
                });
    }
}
