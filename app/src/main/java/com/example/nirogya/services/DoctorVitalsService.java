package com.example.nirogya.services;

import android.content.Context;
import android.widget.Toast;

import com.example.nirogya.Vital;
import com.google.firebase.firestore.*;
import com.example.nirogya.R;

import java.util.*;

public class DoctorVitalsService {

    private final FirebaseFirestore db;
    private final String uid;
    private final Context context;

    public interface VitalsCallback {
        void onVitalsFetched(List<Vital> vitals);
    }

    public DoctorVitalsService(Context context, FirebaseFirestore db, String uid) {
        this.context = context;
        this.db = db;
        this.uid = uid;
    }

    public void fetchDoctorVitals(VitalsCallback callback) {
        db.collection("users").document(uid)
                .collection("doctor_vitals")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Toast.makeText(context, "Failed to load doctor vitals", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Vital> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String systolic = doc.getString("systolic");
                        String diastolic = doc.getString("diastolic");
                        String oxygen = doc.getString("oxygen");
                        String hr = doc.getString("heartRate");
                        String temp = doc.getString("temperature");
                        String note = doc.getString("note");

                        if (systolic != null && diastolic != null) {
                            String bp = systolic + "/" + diastolic;
                            list.add(new Vital("Blood Pressure", bp, R.drawable.ic_blood_pressure));
                        }
                        if (oxygen != null) list.add(new Vital("Oxygen", oxygen, R.drawable.ic_oxygen));
                        if (temp != null) list.add(new Vital("Temperature", temp, R.drawable.ic_temperature));
                        if (hr != null) list.add(new Vital("Heart Rate", hr, R.drawable.ic_heart_rate));
                        if (note != null) list.add(new Vital("Doctor Note", note, R.drawable.ic_note));

                        break; // only latest entry
                    }

                    callback.onVitalsFetched(list);
                });
    }
}
