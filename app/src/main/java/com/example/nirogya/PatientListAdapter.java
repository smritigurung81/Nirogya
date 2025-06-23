package com.example.nirogya;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.ViewHolder> {

    private Context context;
    private List<Patient> patientList;

    public PatientListAdapter(Context context, List<Patient> list) {
        this.context = context;
        this.patientList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patientList.get(position);
        holder.tvName.setText(patient.getName());

        // Handle Add Note button click
        holder.btnAddNote.setOnClickListener(v -> {
            AddVitalsNotesDialog.show(context, patient.getUid());
        });

        // Handle View Reports button click
        holder.btnViewReports.setOnClickListener(v -> {
            Intent labIntent = new Intent(context, LabReportsViewerActivity.class);
            labIntent.putExtra("patientId", patient.getUid());
            context.startActivity(labIntent);
        });

        // You can update alert info visibility here if needed
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Button btnAddNote, btnViewReports;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPatientName);
            btnAddNote = itemView.findViewById(R.id.btnAddVitalsAndNotes);
            btnViewReports = itemView.findViewById(R.id.btnViewReports);
        }
    }
}



