package com.example.nirogya;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddVitalsNotesDialog {

    public static void show(Context context, String patientUid) {
        // Inflate the dialog layout
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_vitals_notes, null);

        // Reference input fields
        EditText etSys = view.findViewById(R.id.etSystolic);
        EditText etDia = view.findViewById(R.id.etDiastolic);
        EditText etHr = view.findViewById(R.id.etHeartRate);
        EditText etOxygen = view.findViewById(R.id.etOxygen);
        EditText etTemp = view.findViewById(R.id.etTemperature);
        EditText etNote = view.findViewById(R.id.etNotes);

        // Build dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Add Vitals & Notes")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        // Override positive button behavior
        dialog.setOnShowListener(d -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                // Collect values
                String systolic = etSys.getText().toString().trim();
                String diastolic = etDia.getText().toString().trim();
                String heartRate = etHr.getText().toString().trim();
                String oxygen = etOxygen.getText().toString().trim();
                String temperature = etTemp.getText().toString().trim();
                String note = etNote.getText().toString().trim();

                // Optional: Validate inputs here

                // Create vitals map
                Map<String, Object> vitals = new HashMap<>();
                vitals.put("systolic", systolic);
                vitals.put("diastolic", diastolic);
                vitals.put("heartRate", heartRate);
                vitals.put("oxygen", oxygen);
                vitals.put("temperature", temperature);
                vitals.put("note", note);
                vitals.put("date", new Date());

                // Save to Firestore
                FirebaseFirestore.getInstance()
                        .collection("users").document(patientUid)
                        .collection("doctor_vitals")
                        .add(vitals)
                        .addOnSuccessListener(r -> {
                            Toast.makeText(context, "Vitals saved successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        });

        dialog.show();
    }
}

