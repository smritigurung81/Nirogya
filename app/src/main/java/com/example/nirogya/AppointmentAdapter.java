package com.example.nirogya;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointmentList;
    private OnAppointmentActionListener listener;

    public interface OnAppointmentActionListener {
        void onAppointmentAction(Appointment appointment, boolean isAccepted);
    }

    public AppointmentAdapter(List<Appointment> appointmentList, OnAppointmentActionListener listener) {
        this.appointmentList = appointmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatientName, tvDoctorName, tvDate, tvTime, tvStatus;
        private Button btnAccept, btnDecline;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvDoctorName = itemView.findViewById(R.id.tv_doctor_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }

        public void bind(Appointment appointment) {
            tvPatientName.setText(appointment.getPatientName());
            tvDoctorName.setText("Dr. " + appointment.getDoctorName());
            tvDate.setText(appointment.getDate());
            tvTime.setText(appointment.getTime());
            tvStatus.setText(appointment.getStatus());

            // Set button click listeners
            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppointmentAction(appointment, true);
                }
            });

            btnDecline.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppointmentAction(appointment, false);
                }
            });
        }
    }
}