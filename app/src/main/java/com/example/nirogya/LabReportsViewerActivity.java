package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.adapters.LabReportAdapter;
import com.example.nirogya.models.LabReport;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LabReportsViewerActivity extends AppCompatActivity {

    private RecyclerView rvReports;
    private LabReportAdapter adapter;
    private List<LabReport> reports;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_reports_viewer);

        rvReports = findViewById(R.id.rvReports);
        db = FirebaseFirestore.getInstance();
        reports = new ArrayList<>();

        adapter = new LabReportAdapter(this, reports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(adapter);

        adapter.setOnItemClickListener(report -> {
            // Handle click on a lab report
            handleReportClick(report);
        });

        loadReports();
    }

    private void loadReports() {
        db.collection("lab_reports")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reports.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        LabReport report = doc.toObject(LabReport.class);
                        reports.add(report);
                    }
                    adapter.updateList(reports);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch reports: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void handleReportClick(LabReport report) {
        Toast.makeText(this, "Clicked on: " + report.getReportTitle(), Toast.LENGTH_SHORT).show();
        // You can open a detailed view here
    }
}
