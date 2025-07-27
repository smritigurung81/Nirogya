package com.example.nirogya.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.R;
import com.example.nirogya.models.Appointment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
    private static final String TAG = "AppointmentAdapter";

    private final List<Appointment> appointmentList;
    private final boolean showButtons;
    private final OnAppointmentActionListener actionListener;
    private final Map<String, String> patientNameCache = new HashMap<>();

    // Constructor without actionListener
    public AppointmentAdapter(List<Appointment> initialList, boolean showButtons) {
        this(initialList, showButtons, null);
    }

    // Constructor with actionListener
    public AppointmentAdapter(List<Appointment> initialList, boolean showButtons, OnAppointmentActionListener actionListener) {
        this.appointmentList = new ArrayList<>();
        if (initialList != null) {
            this.appointmentList.addAll(initialList);
        }
        this.showButtons = showButtons;
        this.actionListener = actionListener;
    }

    // Fixed updateList method
    public void updateList(List<Appointment> newList) {
        appointmentList.clear();
        if (newList != null) {
            appointmentList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);
        String patientId = appointment.getPatientId();

        // Fetch patient name from Firestore (with cache)
        if (patientId != null && !patientId.isEmpty()) {
            if (patientNameCache.containsKey(patientId)) {
                holder.tvPatientName.setText("Patient: " + patientNameCache.get(patientId));
            } else {
                holder.tvPatientName.setText("Patient: Loading...");
                FirebaseFirestore.getInstance().collection("users").document(patientId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String first = doc.getString("firstName");
                                String last = doc.getString("lastName");
                                String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                                patientNameCache.put(patientId, fullName);
                                holder.tvPatientName.setText("Patient: " + fullName);
                            } else {
                                holder.tvPatientName.setText("Patient: Unknown");
                            }
                        })
                        .addOnFailureListener(e -> {
                            holder.tvPatientName.setText("Patient: Unknown");
                            Log.e(TAG, "Error fetching patient name: " + e.getMessage(), e);
                        });
            }
        } else {
            holder.tvPatientName.setText("Patient: Unknown");
        }

        holder.tvDate.setText("Date: " + (appointment.getDate() != null ? appointment.getDate() : "N/A"));
        holder.tvTime.setText("Time: " + (appointment.getTime() != null ? appointment.getTime() : "N/A"));

        String status = appointment.getStatus() != null ? appointment.getStatus().toUpperCase() : "UNKNOWN";
        holder.tvStatus.setText(status);
        setStatusColor(holder.tvStatus, status.toLowerCase());

        if (showButtons && "pending".equalsIgnoreCase(status)) {
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnDecline.setVisibility(View.VISIBLE);

            if (actionListener != null) {
                holder.btnAccept.setOnClickListener(v -> {
                    String id = appointment.getAppointmentId() != null ? appointment.getAppointmentId() : appointment.getId();
                    if (id != null) actionListener.onAccept(id);
                });
                holder.btnDecline.setOnClickListener(v -> {
                    String id = appointment.getAppointmentId() != null ? appointment.getAppointmentId() : appointment.getId();
                    if (id != null) actionListener.onDecline(id);
                });
            }
        } else {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);
        }
    }

    private void setStatusColor(TextView view, String status) {
        int color;
        switch (status) {
            case "pending":
                color = view.getContext().getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "accepted":
                color = view.getContext().getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "declined":
                color = view.getContext().getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                color = view.getContext().getResources().getColor(android.R.color.darker_gray);
        }
        view.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvDate, tvTime, tvStatus;
        Button btnAccept, btnDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvStatus = itemView.findViewById(R.id.tvAppointmentStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    public interface OnAppointmentActionListener {
        void onAccept(String appointmentId);
        void onDecline(String appointmentId);
    }
}
