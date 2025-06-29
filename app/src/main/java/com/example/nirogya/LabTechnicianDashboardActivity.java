package com.example.nirogya;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;

public class LabTechnicianDashboardActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_PERMISSION = 1002;

    private TextInputEditText etPatientId;
    private TextInputEditText etReportTitle;
    private Button btnChooseImage;
    private Button btnUploadReport;
    private Button btnLogout;
    private ImageView ivReportPreview;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_technician_dashboard);

        initializeViews();
        setupClickListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lab Technician Dashboard");
        }
    }

    private void initializeViews() {
        etPatientId = findViewById(R.id.etPatientId);
        etReportTitle = findViewById(R.id.etReportTitle);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUploadReport = findViewById(R.id.btnUploadReport);
        btnLogout = findViewById(R.id.btnLogout);
        ivReportPreview = findViewById(R.id.ivReportPreview);
    }

    private void setupClickListeners() {
        btnChooseImage.setOnClickListener(v -> checkPermissionAndPickImage());
        btnUploadReport.setOnClickListener(v -> uploadReport());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        } else {
            pickImageFromGallery();
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void uploadReport() {
        String patientId = etPatientId.getText().toString().trim();
        String reportTitle = etReportTitle.getText().toString().trim();

        if (patientId.isEmpty()) {
            etPatientId.setError("Patient ID is required");
            etPatientId.requestFocus();
            return;
        }

        if (reportTitle.isEmpty()) {
            etReportTitle.setError("Report title is required");
            etReportTitle.requestFocus();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement actual upload logic here
        showUploadSuccessDialog(patientId, reportTitle);
    }

    private void showUploadSuccessDialog(String patientId, String reportTitle) {
        new AlertDialog.Builder(this)
                .setTitle("Upload Successful")
                .setMessage("Report '" + reportTitle + "' has been uploaded for patient " + patientId)
                .setPositiveButton("OK", (dialog, which) -> clearForm())
                .show();
    }

    private void clearForm() {
        etPatientId.setText("");
        etReportTitle.setText("");
        ivReportPreview.setImageResource(0);
        ivReportPreview.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        selectedImageUri = null;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        // ✅ Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // ✅ Redirect to LoginActivity with cleared back stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    ivReportPreview.setImageBitmap(bitmap);
                    ivReportPreview.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to go back?")
                .setPositiveButton("Yes", (dialog, which) -> LabTechnicianDashboardActivity.super.onBackPressed())
                .setNegativeButton("No", null)
                .show();
    }
}
