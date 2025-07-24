package com.example.nirogya.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nirogya.R;
import com.example.nirogya.models.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private boolean showButtons;

    public AppointmentAdapter(List<Appointment> appointments, boolean showButtons) {
        this.appointments = appointments;
        this.showButtons = showButtons;
    }

    public void updateList(List<Appointment> list) {
        this.appointments = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentAdapter.ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);

        holder.tvPatientName.setText("Patient: " + appointment.getPatientName());
        holder.tvDoctorName.setText("Doctor: " + appointment.getDoctorName());
        holder.tvDate.setText(appointment.getDate());
        holder.tvTime.setText(appointment.getTime());
        holder.tvStatus.setText(appointment.getStatus());

        if (showButtons) {
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            // Optional: add click handlers for accept/decline
        } else {
            holder.layoutActionButtons.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvDoctorName, tvDate, tvTime, tvStatus;
        LinearLayout layoutActionButtons;
        Button btnAccept, btnDecline;

        ViewHolder(View view) {
            super(view);
            tvPatientName = view.findViewById(R.id.tv_patient_name);
            tvDoctorName = view.findViewById(R.id.tv_doctor_name);
            tvDate = view.findViewById(R.id.tv_date);
            tvTime = view.findViewById(R.id.tv_time);
            tvStatus = view.findViewById(R.id.tv_status);
            layoutActionButtons = view.findViewById(R.id.layout_action_buttons);
            btnAccept = view.findViewById(R.id.btn_accept);
            btnDecline = view.findViewById(R.id.btn_decline);
        }
    }
}
