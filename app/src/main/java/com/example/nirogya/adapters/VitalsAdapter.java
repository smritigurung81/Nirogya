package com.example.nirogya.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.R;
import com.example.nirogya.models.Vital;

import java.util.List;

public class VitalsAdapter extends RecyclerView.Adapter<VitalsAdapter.ViewHolder> {

    private List<Vital> vitals;

    public VitalsAdapter(List<Vital> vitals) {
        this.vitals = vitals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vital, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vital vital = vitals.get(position);

        // Set timestamp
        if (vital.getTimestamp() != null && !vital.getTimestamp().isEmpty()) {
            holder.tvVitalTimestamp.setText(vital.getTimestamp());
        } else {
            holder.tvVitalTimestamp.setText("No timestamp");
        }

        // Combine all vitals into one string
        StringBuilder vitalValues = new StringBuilder();

        if (vital.getTemperature() != null && !vital.getTemperature().isEmpty()) {
            vitalValues.append("Temp: ").append(vital.getTemperature()).append("°F");
        }

        if (vital.getOxygen() != null && !vital.getOxygen().isEmpty()) {
            if (vitalValues.length() > 0) vitalValues.append(", ");
            vitalValues.append("SpO2: ").append(vital.getOxygen()).append("%");
        }

        if (vital.getHeartRate() != null && !vital.getHeartRate().isEmpty()) {
            if (vitalValues.length() > 0) vitalValues.append(", ");
            vitalValues.append("HR: ").append(vital.getHeartRate()).append(" bpm");
        }

        if (vitalValues.length() > 0) {
            holder.tvVitalValues.setText(vitalValues.toString());
        } else {
            holder.tvVitalValues.setText("No vital signs recorded");
        }

        // Set SOAP notes
        if (vital.getSoapNotes() != null && !vital.getSoapNotes().trim().isEmpty()) {
            holder.tvSOAPNotes.setText(vital.getSoapNotes());
            holder.tvSOAPNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvSOAPNotes.setText("No SOAP notes");
            holder.tvSOAPNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return vitals != null ? vitals.size() : 0;
    }

    public void updateVitals(List<Vital> newVitals) {
        this.vitals = newVitals;
        notifyDataSetChanged();
    }

    public void updateList(List<Vital> newVitals) {
        updateVitals(newVitals);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVitalTimestamp;
        TextView tvVitalValues;
        TextView tvSOAPNotes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVitalTimestamp = itemView.findViewById(R.id.tvVitalTimestamp);
            tvVitalValues = itemView.findViewById(R.id.tvVitalValues);
            tvSOAPNotes = itemView.findViewById(R.id.tvSOAPNotes);
        }
    }
}
