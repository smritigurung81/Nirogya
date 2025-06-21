package com.example.nirogya;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    public AuthManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Sign up with role
    public void signUpUser(String email, String password, String fullName, String role, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        saveUserRole(user, fullName, role, callback);
                    } else {
                        callback.onFailure("Registration failed");
                    }
                });
    }


    // Sign in
    public void signInUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        getUserRole(callback);
                    } else {
                        callback.onFailure("Login failed");
                    }
                });
    }

    private void saveUserRole(FirebaseUser user, String fullName, String role, AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("fullName", fullName);  // Add fullName here
        userData.put("role", role);

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(role))
                .addOnFailureListener(e -> callback.onFailure("Failed to save user"));
    }


    private void getUserRole(AuthCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String role = task.getResult().getString("role");
                        callback.onSuccess(role);
                    } else {
                        callback.onFailure("Failed to get user role");
                    }
                });
    }

    public interface AuthCallback {
        void onSuccess(String role);
        void onFailure(String error);
    }
}