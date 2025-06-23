package com.example.nirogya;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VitalsAdapter extends RecyclerView.Adapter<VitalsAdapter.VitalViewHolder> {
    private List<Vital> vitals;

    public VitalsAdapter(List<Vital> vitals) {
        this.vitals = vitals;
    }

    @NonNull
    @Override
    public VitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vital, parent, false);
        return new VitalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VitalViewHolder holder, int position) {
        Vital vital = vitals.get(position);
        holder.tvVitalName.setText(vital.getName());
        holder.tvVitalValue.setText(vital.getValue());
        holder.ivVitalIcon.setImageResource(vital.getIconResource());
    }

    @Override
    public int getItemCount() {
        return vitals.size();
    }

    public static class VitalViewHolder extends RecyclerView.ViewHolder {
        TextView tvVitalName, tvVitalValue;
        ImageView ivVitalIcon;

        public VitalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVitalName = itemView.findViewById(R.id.tvVitalName);
            tvVitalValue = itemView.findViewById(R.id.tvVitalValue);
            ivVitalIcon = itemView.findViewById(R.id.ivVitalIcon);
        }
    }
}
