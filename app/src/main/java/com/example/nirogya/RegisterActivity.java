package com.example.nirogya;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etFirstName, etLastName, etEmail, etPassword, etNMC, etKnownDoctorNMC;
    Spinner roleSpinner;
    Button btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    LinearLayout layoutDoctor, layoutPatient, layoutLabTechnician;

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
        layoutLabTechnician = findViewById(R.id.layoutLabTechnician);

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
                layoutLabTechnician.setVisibility(View.GONE);
                if (!selectedRole.equals("patient")) etKnownDoctorNMC.setText("");
                if (selectedRole.equals("doctor")) layoutDoctor.setVisibility(View.VISIBLE);
                else if (selectedRole.equals("patient")) layoutPatient.setVisibility(View.VISIBLE);
                else if (selectedRole.equals("lab technician")) layoutLabTechnician.setVisibility(View.VISIBLE);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRegister.setOnClickListener(v -> handleRegistration());
        tvLogin.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    }

    private void handleRegistration() {
        String firstName = capitalizeWords(etFirstName.getText().toString().trim());
        String lastName = capitalizeWords(etLastName.getText().toString().trim());
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String roleInput = roleSpinner.getSelectedItem().toString();

        String role;
        switch (roleInput.toLowerCase()) {
            case "patient": role = "Patient"; break;
            case "doctor": role = "Doctor"; break;
            case "lab technician": role = "LabTechnician"; break;
            default: role = "";
        }

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show(); return;
        }
        if (role.isEmpty()) {
            Toast.makeText(this, "Please select a valid role", Toast.LENGTH_SHORT).show(); return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show(); return;
        }

        String nmc = etNMC.getText().toString().trim();
        if (role.equals("Doctor")) {
            nmc = nmc.replaceAll("[^\\d]", "");
            if (nmc.isEmpty()) {
                Toast.makeText(this, "Please enter your NMC number", Toast.LENGTH_SHORT).show(); return;
            }
            nmc = "NMC" + nmc;
        }

        String knownDoctorNmc = etKnownDoctorNMC.getText().toString().trim().replaceAll("[^\\d]", "");
        if (!knownDoctorNmc.isEmpty()) {
            knownDoctorNmc = "NMC" + knownDoctorNmc;
        }

        String finalNmc = nmc;
        String finalKnownDoctorNmc = knownDoctorNmc;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String uid = authResult.getUser().getUid();
                        Log.d("RegisterActivity", "Firebase user created: " + uid);

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("firstName", firstName);
                        userData.put("lastName", lastName);
                        userData.put("email", email);
                        userData.put("role", role);

                        if (role.equals("Doctor")) {
                            userData.put("nmcNumber", finalNmc);
                            saveUserToFirestore(uid, userData, role);
                        } else if (role.equals("Patient") && !finalKnownDoctorNmc.isEmpty()) {
                            userData.put("linkedDoctorNmc", finalKnownDoctorNmc);

                            db.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener(unused -> {
                                        Log.d("RegisterActivity", "Initial patient document written. UID: " + uid);
                                        linkToDoctor(finalKnownDoctorNmc, uid, role);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("RegisterActivity", "Initial save failed: " + e.getMessage());
                                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            saveUserToFirestore(uid, userData, role);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void linkToDoctor(String knownDoctorNmc, String uid, String role) {
        Log.d("RegisterActivity", "linkToDoctor() called with NMC: " + knownDoctorNmc);
        db.collection("users")
                .whereEqualTo("nmcNumber", knownDoctorNmc)
                .whereEqualTo("role", "Doctor")
                .get()
                .addOnSuccessListener(snapshots -> {
                    Log.d("RegisterActivity", "Doctor query returned: " + snapshots.size() + " result(s)");

                    Map<String, Object> update = new HashMap<>();
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        String doctorId = doc.getId();
                        String doctorNmc = doc.getString("nmcNumber");

                        Log.d("RegisterActivity", "Doctor found! ID: " + doctorId + ", NMC: " + doctorNmc);
                        update.put("linkedDoctorId", doctorId);
                        update.put("linkedDoctorNmc", doctorNmc);
                    } else {
                        Log.w("RegisterActivity", "No doctor found with NMC: " + knownDoctorNmc);
                    }

                    db.collection("users").document(uid).update(update)
                            .addOnSuccessListener(unused -> {
                                Log.d("RegisterActivity", "Patient document updated with linked doctor.");
                                redirectToDashboard(role);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("RegisterActivity", "Failed to update link: " + e.getMessage());
                                Toast.makeText(this, "Registered without link", Toast.LENGTH_SHORT).show();
                                redirectToDashboard(role);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Error during doctor query: " + e.getMessage());
                    Toast.makeText(this, "Error finding doctor", Toast.LENGTH_SHORT).show();
                    redirectToDashboard(role);
                });
    }

    private void saveUserToFirestore(String uid, Map<String, Object> userData, String role) {
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(unused -> {
                    Log.d("RegisterActivity", "User data saved to Firestore.");
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                    redirectToDashboard(role);
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Failed to save user data: " + e.getMessage());
                    Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "Doctor":
                intent = new Intent(this, DoctorDashboardActivity.class); break;
            case "LabTechnician":
                intent = new Intent(this, LabTechnicianDashboardActivity.class); break;
            case "Patient":
            default:
                intent = new Intent(this, PatientDashboardActivity.class); break;
        }
        startActivity(intent);
        finish();
    }

    private String capitalizeWords(String input) {
        String[] words = input.toLowerCase().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
