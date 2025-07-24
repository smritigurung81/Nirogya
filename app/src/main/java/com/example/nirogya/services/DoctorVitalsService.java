package com.example.nirogya.services;

import android.content.Context;
import android.widget.Toast;

import com.example.nirogya.models.Vital;
import com.google.firebase.firestore.*;

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
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1) // Only latest entry
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Toast.makeText(context, "Failed to load doctor vitals", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Vital> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String timestamp = doc.getString("timestamp");

                        Map<String, Object> vitals = (Map<String, Object>) doc.get("vitals");
                        Map<String, Object> soap = (Map<String, Object>) doc.get("soap");

                        if (vitals != null) {
                            Object systolic = vitals.get("systolic");
                            Object diastolic = vitals.get("diastolic");
                            Object oxygen = vitals.get("oxygen");
                            Object hr = vitals.get("heartRate");
                            Object temp = vitals.get("temperature");

                            if (systolic != null && diastolic != null) {
                                String bp = systolic.toString() + "/" + diastolic.toString();
                                list.add(new Vital("Blood Pressure", bp, timestamp));
                            }

                            if (oxygen != null) list.add(new Vital("Oxygen", oxygen.toString(), timestamp));
                            if (temp != null) list.add(new Vital("Temperature", temp.toString(), timestamp));
                            if (hr != null) list.add(new Vital("Heart Rate", hr.toString(), timestamp));
                        }

                        if (soap != null) {
                            StringBuilder noteBuilder = new StringBuilder();
                            if (soap.get("subjective") != null)
                                noteBuilder.append("S: ").append(soap.get("subjective")).append("\n");
                            if (soap.get("objective") != null)
                                noteBuilder.append("O: ").append(soap.get("objective")).append("\n");
                            if (soap.get("assessment") != null)
                                noteBuilder.append("A: ").append(soap.get("assessment")).append("\n");
                            if (soap.get("plan") != null)
                                noteBuilder.append("P: ").append(soap.get("plan")).append("\n");

                            if (noteBuilder.length() > 0)
                                list.add(new Vital("Doctor Notes", noteBuilder.toString().trim(), timestamp));
                        }
                    }

                    callback.onVitalsFetched(list);
                });
    }
}
