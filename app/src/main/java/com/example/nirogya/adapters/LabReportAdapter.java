package com.example.nirogya.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.R;
import com.example.nirogya.models.LabReport;

import java.util.List;

public class LabReportAdapter extends RecyclerView.Adapter<LabReportAdapter.ViewHolder> {

    private Context context;
    private List<LabReport> reportList;
    private OnItemClickListener listener;

    public LabReportAdapter(Context context, List<LabReport> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    public interface OnItemClickListener {
        void onItemClick(LabReport report);
    }

    public void setOnItemClickListener(OnItemClickListener clickListener) {
        this.listener = clickListener;
    }

    public void updateList(List<LabReport> newList) {
        this.reportList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LabReportAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lab_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabReportAdapter.ViewHolder holder, int position) {
        LabReport report = reportList.get(position);

        holder.tvTitle.setText(report.getReportTitle());
        holder.tvPatientName.setText("Patient: " + report.getPatientName());
        holder.tvType.setText("Type: " + report.getReportType());
        holder.tvTechnician.setText("By: " + report.getTechnicianName());

        // Clear old rows
        holder.containerReportRows.removeAllViews();

        String remarks = "";

        // Helper to add a row
        View row;
        switch (report.getReportType()) {
            case "CBC":
                row = createRow("Hemoglobin", report.getHemoglobin(), "13–17 g/dL");
                holder.containerReportRows.addView(row);
                row = createRow("WBC", report.getWbc(), "4–11 x10⁹/L");
                holder.containerReportRows.addView(row);
                row = createRow("Platelets", report.getPlatelets(), "150–400 x10⁹/L");
                holder.containerReportRows.addView(row);

                try {
                    double hb = Double.parseDouble(report.getHemoglobin());
                    if (hb < 13 || hb > 17) remarks += "Abnormal Hemoglobin. ";
                    double wbc = Double.parseDouble(report.getWbc());
                    if (wbc < 4 || wbc > 11) remarks += "Abnormal WBC. ";
                    double plt = Double.parseDouble(report.getPlatelets());
                    if (plt < 150 || plt > 400) remarks += "Abnormal Platelets.";
                } catch (Exception ignored) {}
                break;

            case "Lipid Profile":
                row = createRow("HDL", report.getHdl(), "≥ 40 mg/dL");
                holder.containerReportRows.addView(row);
                row = createRow("LDL", report.getLdl(), "< 100 mg/dL");
                holder.containerReportRows.addView(row);
                row = createRow("Triglycerides", report.getTriglycerides(), "< 150 mg/dL");
                holder.containerReportRows.addView(row);

                try {
                    double hdl = Double.parseDouble(report.getHdl());
                    if (hdl < 40) remarks += "Low HDL. ";
                    double ldl = Double.parseDouble(report.getLdl());
                    if (ldl > 100) remarks += "High LDL. ";
                    double tri = Double.parseDouble(report.getTriglycerides());
                    if (tri > 150) remarks += "High Triglycerides.";
                } catch (Exception ignored) {}
                break;

            case "Blood Sugar":
                row = createRow("Fasting", report.getFastingSugar(), "70–100 mg/dL");
                holder.containerReportRows.addView(row);
                row = createRow("Postprandial", report.getPostSugar(), "< 140 mg/dL");
                holder.containerReportRows.addView(row);
                row = createRow("HbA1c", report.getHba1c(), "< 5.7%");
                holder.containerReportRows.addView(row);

                try {
                    double fast = Double.parseDouble(report.getFastingSugar());
                    if (fast < 70 || fast > 100) remarks += "Abnormal Fasting. ";
                    double post = Double.parseDouble(report.getPostSugar());
                    if (post > 140) remarks += "High Postprandial. ";
                    double hba = Double.parseDouble(report.getHba1c());
                    if (hba > 5.7) remarks += "High HbA1c.";
                } catch (Exception ignored) {}
                break;
        }

        if (!remarks.isEmpty()) {
            holder.tvRemarks.setVisibility(View.VISIBLE);
            holder.tvRemarks.setText("Remarks: " + remarks.trim());
        } else {
            holder.tvRemarks.setVisibility(View.GONE);
        }

        // Expand/collapse
        holder.itemView.setOnClickListener(v -> {
            boolean visible = holder.layoutReportDetails.getVisibility() == View.VISIBLE;
            holder.layoutReportDetails.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (listener != null) listener.onItemClick(report);
        });
    }

    private View createRow(String test, String value, String range) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvTest = new TextView(context);
        tvTest.setText(test);
        tvTest.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tvValue = new TextView(context);
        tvValue.setText(value);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tvRange = new TextView(context);
        tvRange.setText(range);
        tvRange.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        row.addView(tvTest);
        row.addView(tvValue);
        row.addView(tvRange);

        return row;
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPatientName, tvType, tvTechnician, tvRemarks;
        LinearLayout layoutReportDetails, containerReportRows;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReportTitle);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvType = itemView.findViewById(R.id.tvReportType);
            tvTechnician = itemView.findViewById(R.id.tvTechnicianName);
            tvRemarks = itemView.findViewById(R.id.tvRemarks);
            layoutReportDetails = itemView.findViewById(R.id.layoutReportDetails);
            containerReportRows = itemView.findViewById(R.id.containerReportRows);
        }
    }
}
