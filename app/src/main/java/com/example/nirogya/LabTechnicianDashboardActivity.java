package com.example.nirogya;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class LabTechnicianDashboardActivity extends AppCompatActivity {

    private EditText etPatientId, etReportTitle;
    private ImageView ivReportPreview;
    private Button btnChooseImage, btnUploadReport, btnLogout;
    private Uri imageUri;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_technician_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize Cloudinary - use try-catch approach
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dhooyk69h");  // Your Cloudinary cloud name
            MediaManager.init(this, config);
        }

        etPatientId = findViewById(R.id.etPatientId);
        etReportTitle = findViewById(R.id.etReportTitle);
        ivReportPreview = findViewById(R.id.ivReportPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUploadReport = findViewById(R.id.btnUploadReport);
        btnLogout = findViewById(R.id.btnLogout); // New logout button

        btnChooseImage.setOnClickListener(v -> openImagePicker());

        btnUploadReport.setOnClickListener(v -> {
            String uid = etPatientId.getText().toString().trim();
            String title = etReportTitle.getText().toString().trim();

            if (uid.isEmpty() || title.isEmpty() || imageUri == null) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadLabReport(uid, title, imageUri);
        });

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(ivReportPreview);
        }
    }

    private void uploadLabReport(String uid, String title, Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading report...");
        progressDialog.show();

        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString();
                        Map<String, Object> reportData = new HashMap<>();
                        reportData.put("title", title);
                        reportData.put("imageUrl", imageUrl);
                        reportData.put("date", new Date());

                        db.collection("users").document(uid)
                                .collection("lab_reports")
                                .document(UUID.randomUUID().toString())
                                .set(reportData)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(LabTechnicianDashboardActivity.this, "Report uploaded successfully", Toast.LENGTH_SHORT).show();
                                    etPatientId.setText("");
                                    etReportTitle.setText("");
                                    ivReportPreview.setImageResource(0);
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(LabTechnicianDashboardActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(LabTechnicianDashboardActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }
}





