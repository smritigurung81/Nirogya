package com.example.nirogya.services;

import android.util.Log;

import com.example.nirogya.DateHelper;
import com.example.nirogya.adapters.AppointmentAdapter;
import com.example.nirogya.models.Appointment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentService {
    private static final String TAG = "AppointmentService";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private boolean isUserAuthenticated() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null;
    }

    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ✅ UPDATED with full debug logging
    public void fetchAppointmentsForDoctor(String doctorId, String status, AppointmentAdapter adapter) {
        Log.d(TAG, "Fetching " + status + " appointments for Doctor UID: " + doctorId);

        if (!isUserAuthenticated()) {
            Log.e(TAG, "User not authenticated");
            adapter.updateList(new ArrayList<>());
            return;
        }

        String currentUserId = getCurrentUserId();
        if (!doctorId.equals(currentUserId)) {
            Log.e(TAG, "Permission denied: User can only access their own appointments");
            adapter.updateList(new ArrayList<>());
            return;
        }

        Query query;

        if (status.equals("today")) {
            long startOfDay = DateHelper.getStartOfTodayMillis();
            long endOfDay = DateHelper.getEndOfTodayMillis();

            query = db.collection("appointments")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("status", "accepted")
                    .whereGreaterThanOrEqualTo("timestamp", new Timestamp(new Date(startOfDay)))
                    .whereLessThanOrEqualTo("timestamp", new Timestamp(new Date(endOfDay)));
        } else {
            query = db.collection("appointments")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("status", status);
        }

        query.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> list = new ArrayList<>();

                    Log.d(TAG, "✅ Firestore returned " + querySnapshot.size() + " documents for status: " + status);

                    for (var doc : querySnapshot.getDocuments()) {
                        Log.d(TAG, "📄 Document ID: " + doc.getId()
                                + " | doctorId: " + doc.getString("doctorId")
                                + " | status: " + doc.getString("status"));

                        Appointment appointment = doc.toObject(Appointment.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(doc.getId());
                            list.add(appointment);
                            Log.d(TAG, "✔ Loaded appointment object: " + appointment.toString());
                        } else {
                            Log.w(TAG, "⚠ Skipped null appointment object from document: " + doc.getId());
                        }
                    }

                    Log.d(TAG, "✅ Final appointment list size for status '" + status + "': " + list.size());
                    adapter.updateList(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load appointments for status " + status + ": " + e.getMessage(), e);
                    adapter.updateList(new ArrayList<>());
                });
    }

    public void createAppointment(String patientId, String doctorId, String date, String time,
                                  String patientName, String doctorName, CreateAppointmentCallback callback) {
        Log.d(TAG, "Creating appointment - Patient: " + patientId + ", Doctor: " + doctorId);

        if (!isUserAuthenticated()) {
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        String currentUserId = getCurrentUserId();
        if (!patientId.equals(currentUserId)) {
            Log.e(TAG, "Permission denied: User can only create appointments for themselves");
            if (callback != null) callback.onFailure("Permission denied");
            return;
        }

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("patientId", patientId);
        appointmentData.put("doctorId", doctorId);
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("status", "pending");
        appointmentData.put("patientName", patientName);
        appointmentData.put("doctorName", doctorName);
        appointmentData.put("timestamp", Timestamp.now());

        db.collection("appointments")
                .add(appointmentData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Appointment created with ID: " + docRef.getId());
                    if (callback != null) callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating appointment: " + e.getMessage(), e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void updateAppointmentStatus(String appointmentId, String newStatus, UpdateStatusCallback callback) {
        Log.d(TAG, "Updating appointment " + appointmentId + " to status: " + newStatus);

        if (!isUserAuthenticated()) {
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        if (callback != null) callback.onFailure("Appointment not found");
                        return;
                    }

                    String doctorId = documentSnapshot.getString("doctorId");
                    String currentUserId = getCurrentUserId();

                    if (!currentUserId.equals(doctorId)) {
                        Log.e(TAG, "Permission denied: Only the assigned doctor can update this appointment");
                        if (callback != null) callback.onFailure("Permission denied");
                        return;
                    }

                    db.collection("appointments")
                            .document(appointmentId)
                            .update("status", newStatus)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Appointment status updated successfully to: " + newStatus);
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating appointment status: " + e.getMessage(), e);
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching appointment for update: " + e.getMessage(), e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void fixExistingAppointments(String doctorId, FixAppointmentsCallback callback) {
        Log.d(TAG, "Fixing existing appointments by adding doctorId: " + doctorId);

        if (!isUserAuthenticated()) {
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        String currentUserId = getCurrentUserId();
        if (!doctorId.equals(currentUserId)) {
            Log.e(TAG, "Permission denied: User can only fix their own appointments");
            if (callback != null) callback.onFailure("Permission denied");
            return;
        }

        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    int updateCount = 0;

                    for (var document : querySnapshot.getDocuments()) {
                        Log.d(TAG, "Checking appointment: " + document.getId());
                        // Custom update logic here if needed
                    }

                    if (updateCount > 0) {
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Successfully updated " + updateCount + " appointments");
                                    if (callback != null) callback.onSuccess(updateCount);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating appointments: " + e.getMessage(), e);
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    } else {
                        Log.d(TAG, "No appointments need updating");
                        if (callback != null) callback.onSuccess(0);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching appointments for fixing: " + e.getMessage(), e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void fetchAppointmentsForPatient(String patientId, AppointmentAdapter adapter) {
        Log.d(TAG, "Fetching appointments for patient: " + patientId);

        if (!isUserAuthenticated()) {
            adapter.updateList(new ArrayList<>());
            return;
        }

        String currentUserId = getCurrentUserId();
        if (!patientId.equals(currentUserId)) {
            Log.e(TAG, "Permission denied: User can only access their own appointments");
            adapter.updateList(new ArrayList<>());
            return;
        }

        db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> list = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        Appointment appointment = doc.toObject(Appointment.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(doc.getId());
                            list.add(appointment);
                        }
                    }
                    Log.d(TAG, "Loaded " + list.size() + " appointments for patient");
                    adapter.updateList(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching patient appointments: " + e.getMessage(), e);
                    adapter.updateList(new ArrayList<>());
                });
    }

    public void deleteAppointment(String appointmentId, DeleteAppointmentCallback callback) {
        if (!isUserAuthenticated()) {
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        if (callback != null) callback.onFailure("Appointment not found");
                        return;
                    }

                    String patientId = documentSnapshot.getString("patientId");
                    String doctorId = documentSnapshot.getString("doctorId");
                    String currentUserId = getCurrentUserId();

                    if (!currentUserId.equals(patientId) && !currentUserId.equals(doctorId)) {
                        if (callback != null) callback.onFailure("Permission denied");
                        return;
                    }

                    db.collection("appointments")
                            .document(appointmentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Appointment deleted successfully");
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting appointment: " + e.getMessage(), e);
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking appointment permissions: " + e.getMessage(), e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // Callback interfaces
    public interface CreateAppointmentCallback {
        void onSuccess(String appointmentId);
        void onFailure(String error);
    }

    public interface UpdateStatusCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface FixAppointmentsCallback {
        void onSuccess(int updatedCount);
        void onFailure(String error);
    }

    public interface DeleteAppointmentCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
