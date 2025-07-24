package com.example.nirogya;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.adapters.LabReportAdapter;
import com.example.nirogya.models.LabReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyReportsActivity extends AppCompatActivity {

    private RecyclerView rvMyReports;
    private LabReportAdapter adapter;
    private List<LabReport> reportList = new ArrayList<>();
    private FirebaseFirestore db;
    private String technicianId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        technicianId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        rvMyReports = findViewById(R.id.rvMyReports);
        rvMyReports.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LabReportAdapter(this, reportList);
        rvMyReports.setAdapter(adapter);

        loadReports();
    }

    private void loadReports() {
        db.collection("lab_reports")
                .whereEqualTo("technicianId", technicianId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reportList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        LabReport report = doc.toObject(LabReport.class);
                        reportList.add(report);
                    }
                    adapter.updateList(reportList);
                })
                .addOnFailureListener(e -> {
                    // Handle error appropriately
                });
    }
}
