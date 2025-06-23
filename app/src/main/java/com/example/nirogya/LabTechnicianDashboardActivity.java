package com.example.nirogya;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.*;

import java.util.*;

public class LabTechnicianDashboardActivity extends AppCompatActivity {

    private EditText etPatientId, etReportTitle;
    private ImageView ivReportPreview;
    private Button btnChooseImage, btnUploadReport;
    private Uri imageUri;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_technician_dashboard);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        etPatientId = findViewById(R.id.etPatientId);
        etReportTitle = findViewById(R.id.etReportTitle);
        ivReportPreview = findViewById(R.id.ivReportPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUploadReport = findViewById(R.id.btnUploadReport);

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

        String reportId = UUID.randomUUID().toString();
        StorageReference imageRef = storageRef.child("lab_reports/" + uid + "/" + reportId + ".jpg");

        imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Map<String, Object> reportData = new HashMap<>();
                reportData.put("title", title);
                reportData.put("imageUrl", uri.toString());
                reportData.put("date", new Date());

                db.collection("users").document(uid)
                        .collection("lab_reports")
                        .document(reportId)
                        .set(reportData)
                        .addOnSuccessListener(aVoid -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Report uploaded successfully", Toast.LENGTH_SHORT).show();
                            etPatientId.setText("");
                            etReportTitle.setText("");
                            ivReportPreview.setImageResource(0);
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}



