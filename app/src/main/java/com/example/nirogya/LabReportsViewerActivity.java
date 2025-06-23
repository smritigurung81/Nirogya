package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.example.nirogya.services.LabReportService;
import com.google.firebase.firestore.*;

import java.util.*;

public class LabReportsViewerActivity extends AppCompatActivity {

    RecyclerView rvReports;
    LabReportService labReportService;
    List<LabReport> reports = new ArrayList<>();
    LabReportAdapter adapter;
    String patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_reports_viewer);

        rvReports = findViewById(R.id.rvUploadedReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        patientId = getIntent().getStringExtra("patientId");

        labReportService = new LabReportService(this, FirebaseFirestore.getInstance());

        labReportService.fetchLabReports(patientId, new LabReportService.LabReportCallback() {
            @Override
            public void onReportsFetched(List<LabReport> fetchedReports) {
                reports = fetchedReports;
                adapter = new LabReportAdapter(LabReportsViewerActivity.this, reports);
                rvReports.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LabReportsViewerActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}


