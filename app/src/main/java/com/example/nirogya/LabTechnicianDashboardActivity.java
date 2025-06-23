package com.example.nirogya;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.nirogya.services.LabReportService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class LabTechnicianDashboardActivity extends AppCompatActivity {

    private EditText etPatientId, etReportTitle;
    private ImageView ivReportPreview;
    private Button btnChooseImage, btnUploadReport;
    private Uri imageUri;

    private static final int PICK_IMAGE_REQUEST = 1;

    private LabReportService labReportService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_technician_dashboard);

        // Initialize MediaManager (Cloudinary)
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dhooyk69h");  // Replace with your actual cloud name
            config.put("api_key", "141128198432229");  // Your API key
            config.put("api_secret", "ssG-b2okdn-XoehpCfxV9LAqKBg");  // Your API secret
            MediaManager.init(this, config);
        }

        // Initialize UI elements
        etPatientId = findViewById(R.id.etPatientId);
        etReportTitle = findViewById(R.id.etReportTitle);
        ivReportPreview = findViewById(R.id.ivReportPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUploadReport = findViewById(R.id.btnUploadReport);

        // Initialize LabReportService
        labReportService = new LabReportService(this, FirebaseFirestore.getInstance());

        // Image picker
        btnChooseImage.setOnClickListener(v -> openImagePicker());

        // Upload report
        btnUploadReport.setOnClickListener(v -> {
            String patientId = etPatientId.getText().toString().trim();
            String title = etReportTitle.getText().toString().trim();

            if (patientId.isEmpty() || title.isEmpty() || imageUri == null) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadToCloudinary(patientId, title, imageUri);
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

    private void uploadToCloudinary(String patientId, String title, Uri imageUri) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading report...");
        dialog.setCancelable(false);
        dialog.show();

        // Use AtomicReference to handle the imageUrl properly in callback
        final AtomicReference<String> imageUrl = new AtomicReference<>("");

        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        imageUrl.set(resultData.get("secure_url").toString());

                        labReportService.uploadLabReport(patientId, title, imageUrl.get(), new LabReportService.ReportUploadCallback() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                                Toast.makeText(LabTechnicianDashboardActivity.this, "Report uploaded successfully", Toast.LENGTH_SHORT).show();
                                etPatientId.setText("");
                                etReportTitle.setText("");
                                ivReportPreview.setImageDrawable(null);
                                imageUri = null;
                            }

                            @Override
                            public void onFailure(String error) {
                                dialog.dismiss();
                                Toast.makeText(LabTechnicianDashboardActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        dialog.dismiss();
                        Toast.makeText(LabTechnicianDashboardActivity.this, "Cloudinary error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }
}