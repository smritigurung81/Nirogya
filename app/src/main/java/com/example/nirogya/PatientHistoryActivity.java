package com.example.nirogya;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nirogya.adapters.VitalsAdapter;
import com.example.nirogya.models.DoctorVitalsModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class PatientHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VitalsAdapter adapter;
    private List<DoctorVitalsModel> historyList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        adapter = new VitalsAdapter(this, historyList);
        recyclerView.setAdapter(adapter);

        String patientId = getIntent().getStringExtra("patientId");

        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(patientId)
                .collection("doctor_vitals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DoctorVitalsModel> temp = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        DoctorVitalsModel model = doc.toObject(DoctorVitalsModel.class);
                        if (model != null) {
                            temp.add(model);
                        }
                    }
                    historyList.clear();
                    historyList.addAll(temp);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show();
                });
    }
}
