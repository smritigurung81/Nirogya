package com.example.nirogya.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nirogya.R;
import com.example.nirogya.models.User;
import com.example.nirogya.services.AddVitalsNotesDialog;
import com.example.nirogya.LabReportsViewerActivity;
import com.example.nirogya.PatientHistoryActivity;
import java.util.List;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.ViewHolder> {

    private List<User> patientList;
    private Context context;

    public PatientListAdapter(List<User> patientList, Context context) {
        this.patientList = patientList;
        this.context = context;
    }

    public void updateList(List<User> newList) {
        this.patientList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_assigned_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User patient = patientList.get(position);
        holder.name.setText("Patient: " + patient.getFullName());

        holder.btnVitals.setOnClickListener(v -> {
            if (context instanceof AppCompatActivity) {
                AddVitalsNotesDialog dialog = new AddVitalsNotesDialog(patient.getUid());
                dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "AddVitalsNotesDialog");
            }
        });

        holder.btnReports.setOnClickListener(v -> {
            Intent i = new Intent(context, LabReportsViewerActivity.class);
            i.putExtra("patientId", patient.getUid());
            context.startActivity(i);
        });

        holder.btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(context, PatientHistoryActivity.class);
            i.putExtra("patientId", patient.getUid());
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        Button btnVitals, btnReports, btnHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvPatientName);
            btnVitals = itemView.findViewById(R.id.btnVitalsNotes);
            btnReports = itemView.findViewById(R.id.btnReports);
            btnHistory = itemView.findViewById(R.id.btnHistory);
        }
    }
}
