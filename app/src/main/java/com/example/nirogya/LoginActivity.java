package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister, tvForgotPassword;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            mAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if ("doctor".equals(role)) {
                        startActivity(new Intent(this, DoctorDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, PatientDashboardActivity.class));
                    }
                });
            }).addOnFailureListener(e -> Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show());
        });

        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> Toast.makeText(this, "Reset link to be implemented", Toast.LENGTH_SHORT).show());
    }
}
