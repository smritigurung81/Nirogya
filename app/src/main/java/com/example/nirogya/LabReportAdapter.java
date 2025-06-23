package com.example.nirogya;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class LabReportAdapter extends RecyclerView.Adapter<LabReportAdapter.LabReportViewHolder> {

    private Context context;
    private List<LabReport> reportList;

    public LabReportAdapter(Context context, List<LabReport> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public LabReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.lab_report_item, parent, false);
        return new LabReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabReportViewHolder holder, int position) {
        LabReport report = reportList.get(position);

        holder.tvTitle.setText(report.getTitle());
        holder.tvDate.setText(report.getDate());

        Glide.with(context)
                .load(report.getImageUrl())
                .placeholder(R.drawable.placeholder) // Add a placeholder image in res/drawable
                .into(holder.ivReport);

        // Open image in browser or viewer on click
        holder.ivReport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(report.getImageUrl()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class LabReportViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReport;
        TextView tvTitle, tvDate;

        public LabReportViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReport = itemView.findViewById(R.id.ivReportImage);
            tvTitle = itemView.findViewById(R.id.tvReportTitle);
            tvDate = itemView.findViewById(R.id.tvReportDate);
        }
    }
}



