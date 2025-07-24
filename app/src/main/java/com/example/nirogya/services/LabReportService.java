package com.example.nirogya.services;

import android.content.Context;
import com.example.nirogya.models.LabReport;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class LabReportService {

    private Context context;
    private FirebaseFirestore firestore;

    public interface OnReportsFetchedListener {
        void onReportsFetched(List<LabReport> reports);
        void onError(String error);
    }

    public LabReportService(Context context, FirebaseFirestore firestore) {
        this.context = context;
        this.firestore = firestore;
    }

    public void fetchLabReports(String patientId, OnReportsFetchedListener listener) {
        if (patientId == null || patientId.isEmpty()) {
            // Fetch all lab reports
            firestore.collection("lab_reports")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<LabReport> reports = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                LabReport report = document.toObject(LabReport.class);
                                report.setId(document.getId());
                                reports.add(report);
                            }
                            listener.onReportsFetched(reports);
                        } else {
                            listener.onError("Failed to fetch lab reports: " + task.getException().getMessage());
                        }
                    });
        } else {
            // Fetch lab reports for specific patient
            firestore.collection("lab_reports")
                    .whereEqualTo("patientId", patientId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<LabReport> reports = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                LabReport report = document.toObject(LabReport.class);
                                report.setId(document.getId());
                                reports.add(report);
                            }
                            listener.onReportsFetched(reports);
                        } else {
                            listener.onError("Failed to fetch lab reports: " + task.getException().getMessage());
                        }
                    });
        }
    }

    public void addLabReport(LabReport report, OnReportAddedListener listener) {
        firestore.collection("lab_reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    report.setId(documentReference.getId());
                    listener.onReportAdded(report);
                })
                .addOnFailureListener(e -> {
                    listener.onError("Failed to add lab report: " + e.getMessage());
                });
    }

    public interface OnReportAddedListener {
        void onReportAdded(LabReport report);
        void onError(String error);
    }
}