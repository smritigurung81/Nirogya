package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Splash screen layout with logo

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // ✅ Prevent redirect loop: check immediately
            if (auth.getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return;
            }

            // ✅ Fetch user role
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                            auth.signOut();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                            return;
                        }

                        String role = doc.getString("role");
                        if (role == null) {
                            Toast.makeText(this, "Role not defined", Toast.LENGTH_SHORT).show();
                            auth.signOut();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                            return;
                        }

                        Intent intent;
                        switch (role) {
                            case "Patient":
                                intent = new Intent(this, PatientDashboardActivity.class);
                                break;
                            case "Doctor":
                                intent = new Intent(this, DoctorDashboardActivity.class);
                                break;
                            case "LabTechnician":
                                intent = new Intent(this, LabTechnicianDashboardActivity.class);
                                break;
                            default:
                                Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
                                auth.signOut();
                                intent = new Intent(this, LoginActivity.class);
                        }

                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });

        }, SPLASH_DURATION);
    }
}



