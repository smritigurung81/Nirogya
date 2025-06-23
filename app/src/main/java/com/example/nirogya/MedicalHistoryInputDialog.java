package com.example.nirogya;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MedicalHistoryInputDialog {

    // Changed interface name to match your usage
    public interface MedicalHistoryCallback {
        void onMedicalHistoryAdded(String disease, String duration, String remarks);
    }

    // Updated method signature to use the new interface name
    public static void show(Context context, MedicalHistoryCallback listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_medical_history, null);

        EditText etDisease = view.findViewById(R.id.etDisease);
        EditText etDuration = view.findViewById(R.id.etDuration);
        EditText etRemarks = view.findViewById(R.id.etRemarks);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Add Medical History")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String disease = etDisease.getText().toString().trim();
                String duration = etDuration.getText().toString().trim();
                String remarks = etRemarks.getText().toString().trim();

                // Updated method call to match the new interface
                listener.onMedicalHistoryAdded(disease, duration, remarks);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}