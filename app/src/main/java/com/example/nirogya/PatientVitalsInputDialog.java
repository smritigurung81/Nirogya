package com.example.nirogya;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

public class PatientVitalsInputDialog {

    public interface OnVitalsEnteredListener {
        void onVitalsEntered(String systolic, String diastolic, String heartRate, String oxygen, String temperature);
    }

    public static void show(Context context, OnVitalsEnteredListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_patient_vitals_input, null);

        EditText etSys = view.findViewById(R.id.etSystolic);
        EditText etDia = view.findViewById(R.id.etDiastolic);
        EditText etHr = view.findViewById(R.id.etHeartRate);
        EditText etOxygen = view.findViewById(R.id.etOxygen);
        EditText etTemp = view.findViewById(R.id.etTemperature);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Enter Your Vitals")
                .setView(view)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSubmit.setOnClickListener(v -> {
                String sys = etSys.getText().toString().trim();
                String dia = etDia.getText().toString().trim();
                String hr = etHr.getText().toString().trim();
                String oxygen = etOxygen.getText().toString().trim();
                String temp = etTemp.getText().toString().trim();

                if (sys.isEmpty() || dia.isEmpty() || hr.isEmpty() || oxygen.isEmpty() || temp.isEmpty()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                listener.onVitalsEntered(sys, dia, hr, oxygen, temp);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}

