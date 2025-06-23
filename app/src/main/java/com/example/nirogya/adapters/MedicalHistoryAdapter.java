package com.example.nirogya.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nirogya.R;
import com.example.nirogya.models.MedicalHistory;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MedicalHistoryAdapter extends RecyclerView.Adapter<MedicalHistoryAdapter.ViewHolder> {
    private List<MedicalHistory> historyList;

    public MedicalHistoryAdapter(List<MedicalHistory> historyList) {
        this.historyList = historyList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDisease, tvDuration, tvRemarks, tvDate;

        public ViewHolder(View view) {
            super(view);
            tvDisease = view.findViewById(R.id.tvDisease);
            tvDuration = view.findViewById(R.id.tvDuration);
            tvRemarks = view.findViewById(R.id.tvRemarks);
            tvDate = view.findViewById(R.id.tvDate);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MedicalHistory history = historyList.get(position);
        holder.tvDisease.setText(history.getDisease());
        holder.tvDuration.setText("Duration: " + history.getDuration());
        holder.tvRemarks.setText("Remarks: " + history.getRemarks());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.tvDate.setText("Date: " + sdf.format(history.getDate()));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
}
