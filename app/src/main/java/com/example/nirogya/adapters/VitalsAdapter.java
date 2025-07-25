package com.example.nirogya.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.models.DoctorVitalsModel;
import com.example.nirogya.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VitalsAdapter extends RecyclerView.Adapter<VitalsAdapter.ViewHolder> {

    private Context context;
    private List<DoctorVitalsModel> vitalsList;

    public VitalsAdapter(Context context, List<DoctorVitalsModel> vitalsList) {
        this.context = context;
        this.vitalsList = vitalsList;
    }

    @NonNull
    @Override
    public VitalsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vital, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VitalsAdapter.ViewHolder holder, int position) {
        DoctorVitalsModel item = vitalsList.get(position);

        Map<String, String> vitals = item.getVitals();
        Map<String, String> soap = item.getSoap();

        holder.tvSystolic.setText("Systolic: " + safe(get(vitals, "systolic")));
        holder.tvDiastolic.setText("Diastolic: " + safe(get(vitals, "diastolic")));
        holder.tvHeartRate.setText("Heart Rate: " + safe(get(vitals, "heartRate")));
        holder.tvOxygen.setText("Oxygen: " + safe(get(vitals, "oxygen")));
        holder.tvTemperature.setText("Temperature: " + safe(get(vitals, "temperature")));

        holder.tvSubjective.setText("Subjective: " + safe(get(soap, "subjective")));
        holder.tvObjective.setText("Objective: " + safe(get(soap, "objective")));
        holder.tvAssessment.setText("Assessment: " + safe(get(soap, "assessment")));
        holder.tvPlan.setText("Plan: " + safe(get(soap, "plan")));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
        if (item.getTimestamp() != null) {
            holder.tvTimestamp.setText("Date: " + sdf.format(item.getTimestamp().toDate()));
        } else {
            holder.tvTimestamp.setText("Date: N/A");
        }
    }

    @Override
    public int getItemCount() {
        return vitalsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystolic, tvDiastolic, tvHeartRate, tvOxygen, tvTemperature;
        TextView tvSubjective, tvObjective, tvAssessment, tvPlan, tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystolic = itemView.findViewById(R.id.tvSystolic);
            tvDiastolic = itemView.findViewById(R.id.tvDiastolic);
            tvHeartRate = itemView.findViewById(R.id.tvHeartRate);
            tvOxygen = itemView.findViewById(R.id.tvOxygen);
            tvTemperature = itemView.findViewById(R.id.tvTemperature);
            tvSubjective = itemView.findViewById(R.id.tvSubjective);
            tvObjective = itemView.findViewById(R.id.tvObjective);
            tvAssessment = itemView.findViewById(R.id.tvAssessment);
            tvPlan = itemView.findViewById(R.id.tvPlan);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }

    private String safe(String value) {
        return (value != null && !value.isEmpty()) ? value : "N/A";
    }

    private String get(Map<String, String> map, String key) {
        return (map != null && map.containsKey(key)) ? map.get(key) : "N/A";
    }

    public void updateList(List<DoctorVitalsModel> newList) {
        vitalsList = newList;
        notifyDataSetChanged();
    }
}
