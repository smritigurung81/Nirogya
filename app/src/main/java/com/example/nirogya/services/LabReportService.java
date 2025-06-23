package com.example.nirogya.services;

import android.content.Context;

import com.example.nirogya.LabReport;
import com.google.firebase.firestore.*;

import java.util.*;

public class LabReportService {

    private final FirebaseFirestore db;
    private final Context context;

    public LabReportService(Context context, FirebaseFirestore db) {
        this.context = context;
        this.db = db;
    }

    // ----------- Upload Functionality (Lab Technician) ------------ //

    public interface ReportUploadCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void uploadLabReport(String patientId, String title, String imageUrl, ReportUploadCallback callback) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("title", title);
        reportData.put("imageUrl", imageUrl);
        reportData.put("date", new Date());

        db.collection("users")
                .document(patientId)
                .collection("lab_reports")
                .document(UUID.randomUUID().toString())
                .set(reportData)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ----------- Fetching Functionality (Patient, Doctor Viewer) ------------ //

    public interface LabReportCallback {
        void onReportsFetched(List<LabReport> reports);
        void onError(String error);
    }

    public void fetchLabReports(String patientId, LabReportCallback callback) {
        db.collection("users")
                .document(patientId)
                .collection("lab_reports")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        callback.onError("Failed to load lab reports.");
                        return;
                    }

                    List<LabReport> reportList = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        LabReport report = doc.toObject(LabReport.class);
                        reportList.add(report);
                    }

                    callback.onReportsFetched(reportList);
                });
    }
}
