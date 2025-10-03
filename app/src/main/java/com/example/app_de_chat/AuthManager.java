package com.example.app_de_chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull; // For @NonNull annotation

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class AuthManager {
    private static final String PREFS = "auth_prefs";
    private static final String KEY_USER = "user";
    private static final String KEY_PASS = "pass";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String TAG = "AuthManager";
    private static FirebaseAuth firebaseAuthInstance;
    private static FirebaseFirestore firestoreInstance;


    // Private constructor to prevent instantiation
    private AuthManager() {}

    // Get FirebaseAuth instance (singleton pattern)
    private static FirebaseAuth getAuthInstance() {
        if (firebaseAuthInstance == null) {
            firebaseAuthInstance = FirebaseAuth.getInstance();
        }
        return firebaseAuthInstance;
    }

    // Get Firestore instance (singleton pattern)
    private static FirebaseFirestore getFirestoreInstance() {
        if (firestoreInstance == null) {
            firestoreInstance = FirebaseFirestore.getInstance();
        }
        return firestoreInstance;
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

    // Register a new user with Firebase and save to Firestore
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
                        FirebaseUser firebaseUser = getAuthInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            // Save user data to Firestore and get FCM token
                            saveUserToFirestore(firebaseUser, listener);
                        } else {
                            if (listener != null) {
                                listener.onFailure("User creation failed - no user returned.");
                            }
                        }
                    } else {
                        if (listener != null) {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                            listener.onFailure(errorMessage);
                        }
                    }
                });
    }

    // Save user data to Firestore
    private static void saveUserToFirestore(FirebaseUser firebaseUser, final AuthTaskListener listener) {
        // First get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    String fcmToken = null;
                    if (task.isSuccessful()) {
                        fcmToken = task.getResult();
                        Log.d(TAG, "FCM Registration Token: " + fcmToken);
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    }

                    // Create user data with FCM token
                    User user = new User(
                            firebaseUser.getUid(),
                            firebaseUser.getEmail(),
                            firebaseUser.getDisplayName() // This might be null initially
                    );

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", user.getUid());
                    userData.put("email", user.getEmail());
                    userData.put("displayName", user.getDisplayName());
                    if (fcmToken != null) {
                        userData.put("fcmToken", fcmToken);
                    }

                    getFirestoreInstance().collection("users")
                            .document(firebaseUser.getUid())
                            .set(userData)
                            .addOnCompleteListener(firestoreTask -> {
                                if (firestoreTask.isSuccessful()) {
                                    if (listener != null) {
                                        listener.onSuccess(firebaseUser);
                                    }
                                } else {
                                    if (listener != null) {
                                        String errorMessage = firestoreTask.getException() != null ?
                                                firestoreTask.getException().getMessage() : "Failed to save user data.";
                                        listener.onFailure("Registration successful but failed to save user data: " + errorMessage);
                                    }
                                }
                            });
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
                        FirebaseUser user = getAuthInstance().getCurrentUser();
                        if (user != null) {
                            // Update FCM token on login
                            updateFCMToken(user);
                        }
                        if (listener != null) {
                            listener.onSuccess(user);
                        }
                    } else {
                        if (listener != null) {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed.";
                            listener.onFailure(errorMessage);
                        }
                    }
                });
    }

    // Update FCM token for current user
    private static void updateFCMToken(FirebaseUser user) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Registration Token: " + token);

                        // Update token in Firestore
                        getFirestoreInstance().collection("users")
                                .document(user.getUid())
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully"))
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to update FCM token", e);
                                    // If update fails, try to set the token
                                    getFirestoreInstance().collection("users")
                                            .document(user.getUid())
                                            .set(new HashMap<String, Object>() {{
                                                put("fcmToken", token);
                                            }}, com.google.firebase.firestore.SetOptions.merge());
                                });
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    }
                });
    }

    // Log out the current user
    public static void logout() {
        getAuthInstance().signOut();
    }

    // Interface for authentication task callbacks
    public interface AuthTaskListener {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }
}
