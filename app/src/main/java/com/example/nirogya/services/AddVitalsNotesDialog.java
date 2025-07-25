package com.example.nirogya.services;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.nirogya.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddVitalsNotesDialog extends DialogFragment {

    private String patientId;

    private EditText etHeartRate, etOxygen, etTemperature;
    private EditText etSystolic, etDiastolic;
    private EditText etSubjective, etObjective, etAssessment, etPlan;
    private Button btnSave;

    public AddVitalsNotesDialog(String patientId) {
        this.patientId = patientId;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_vitals_notes, container, false);

        etSystolic = view.findViewById(R.id.etSystolic);
        etDiastolic = view.findViewById(R.id.etDiastolic);
        etHeartRate = view.findViewById(R.id.etHeartRate);
        etOxygen = view.findViewById(R.id.etOxygen);
        etTemperature = view.findViewById(R.id.etTemperature);

        etSubjective = view.findViewById(R.id.etSubjective);
        etObjective = view.findViewById(R.id.etObjective);
        etAssessment = view.findViewById(R.id.etAssessment);
        etPlan = view.findViewById(R.id.etPlan);

        btnSave = view.findViewById(R.id.btnSaveVitals);
        btnSave.setOnClickListener(v -> saveVitalsAndNotes());

        return view;
    }

    private void saveVitalsAndNotes() {
        String systolic = etSystolic.getText().toString().trim();
        String diastolic = etDiastolic.getText().toString().trim();
        String heartRate = etHeartRate.getText().toString().trim();
        String oxygen = etOxygen.getText().toString().trim();
        String temperature = etTemperature.getText().toString().trim();

        String subjective = etSubjective.getText().toString().trim();
        String objective = etObjective.getText().toString().trim();
        String assessment = etAssessment.getText().toString().trim();
        String plan = etPlan.getText().toString().trim();

        if (systolic.isEmpty() || diastolic.isEmpty() || heartRate.isEmpty() ||
                oxygen.isEmpty() || temperature.isEmpty()) {
            Toast.makeText(getContext(), "Please enter all vitals", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> vitals = new HashMap<>();
        vitals.put("systolic", systolic);
        vitals.put("diastolic", diastolic);
        vitals.put("heartRate", heartRate);
        vitals.put("oxygen", oxygen);
        vitals.put("temperature", temperature);

        Map<String, Object> soap = new HashMap<>();
        soap.put("subjective", subjective);
        soap.put("objective", objective);
        soap.put("assessment", assessment);
        soap.put("plan", plan);

        Map<String, Object> data = new HashMap<>();
        data.put("vitals", vitals);
        data.put("soap", soap);
        data.put("timestamp", Timestamp.now());
        data.put("doctorId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(patientId)
                .collection("doctor_vitals")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Saved successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
