package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    EditText etFirstName, etLastName, etEmail, etPassword, etNMC, etKnownDoctorNMC;
    Spinner roleSpinner;
    Button btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    LinearLayout layoutDoctor, layoutPatient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etNMC = findViewById(R.id.etNMC);
        etKnownDoctorNMC = findViewById(R.id.etKnownDoctorNMC);
        roleSpinner = findViewById(R.id.roleSpinner);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        layoutDoctor = findViewById(R.id.layoutDoctor);
        layoutPatient = findViewById(R.id.layoutPatient);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select role", "patient", "doctor", "lab technician"});
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedRole = parent.getItemAtPosition(pos).toString();
                layoutDoctor.setVisibility(View.GONE);
                layoutPatient.setVisibility(View.GONE);
                if (selectedRole.equals("doctor")) {
                    layoutDoctor.setVisibility(View.VISIBLE);
                } else if (selectedRole.equals("patient")) {
                    layoutPatient.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRegister.setOnClickListener(v -> {
            String firstName = capitalizeWords(etFirstName.getText().toString().trim());
            String lastName = capitalizeWords(etLastName.getText().toString().trim());
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();
            String nmc = etNMC.getText().toString().trim();
            String knownDoctorNmc = etKnownDoctorNMC.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role.equals("Select role")) {
                Toast.makeText(this, "Please select a valid role", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role.equals("doctor") && nmc.isEmpty()) {
                Toast.makeText(this, "Please enter your NMC number", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        if (authResult.getUser() != null) {
                            String uid = authResult.getUser().getUid();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("firstName", firstName);
                            userData.put("lastName", lastName);
                            userData.put("email", email);
                            userData.put("role", role);

                            if (role.equals("doctor")) {
                                userData.put("nmcNumber", nmc);
                                saveUserToFirestore(uid, userData);
                            } else if (role.equals("patient") && !knownDoctorNmc.isEmpty()) {
                                db.collection("users")
                                        .whereEqualTo("nmcNumber", knownDoctorNmc)
                                        .whereEqualTo("role", "doctor")
                                        .get()
                                        .addOnSuccessListener(snapshots -> {
                                            if (!snapshots.isEmpty()) {
                                                String doctorId = snapshots.getDocuments().get(0).getId();
                                                userData.put("linkedDoctorId", doctorId);
                                            }
                                            saveUserToFirestore(uid, userData);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error finding doctor", Toast.LENGTH_SHORT).show();
                                            saveUserToFirestore(uid, userData);
                                        });
                            } else {
                                saveUserToFirestore(uid, userData);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        tvLogin.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    }

    private void saveUserToFirestore(String uid, Map<String, Object> userData) {
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private String capitalizeWords(String input) {
        String[] words = input.toLowerCase().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}





