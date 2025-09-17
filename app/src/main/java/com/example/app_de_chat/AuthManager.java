package com.example.app_de_chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull; // For @NonNull annotation

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class AuthManager {
    private static final String PREFS = "auth_prefs";
    private static final String KEY_USER = "user";
    private static final String KEY_PASS = "pass";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static FirebaseAuth firebaseAuthInstance;


    // Private constructor to prevent instantiation
    private AuthManager() {}

    // Get FirebaseAuth instance (singleton pattern)
    private static FirebaseAuth getAuthInstance() {
        if (firebaseAuthInstance == null) {
            firebaseAuthInstance = FirebaseAuth.getInstance();
        }
        return firebaseAuthInstance;
    }

    // Check if a user is currently logged in
    public static boolean isLoggedIn() {
        return getAuthInstance().getCurrentUser() != null;
    }

    // Get the currently logged-in FirebaseUser
    public static FirebaseUser getCurrentUser() {
        return getAuthInstance().getCurrentUser();
    }

    // Get the email of the registered user (if logged in)
    public static String getRegisteredUserEmail() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.getEmail();
        }
        return null;
    }

    // Register a new user with Firebase
    public static void register(Context context, String email, String password, final AuthTaskListener listener) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            if (listener != null) {
                listener.onFailure("Email and password cannot be empty.");
            }
            return;
        }

        getAuthInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (listener != null) {
                            listener.onSuccess(getAuthInstance().getCurrentUser());
                        }
                    } else {
                        if (listener != null) {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                            listener.onFailure(errorMessage);
                        }
                    }
                });
    }

    // Log in an existing user with Firebase
    public static void login(Context context, String email, String password, final AuthTaskListener listener) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            if (listener != null) {
                listener.onFailure("Email and password cannot be empty.");
            }
            return;
        }

        getAuthInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (listener != null) {
                            listener.onSuccess(getAuthInstance().getCurrentUser());
                        }
                    } else {
                        if (listener != null) {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed.";
                            listener.onFailure(errorMessage);
                        }
                    }
                });
    }

    // Log out the current user
    public static void logout() {
        getAuthInstance().signOut();
    }

    // Interface for callback listeners
    public interface AuthTaskListener {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }
}
