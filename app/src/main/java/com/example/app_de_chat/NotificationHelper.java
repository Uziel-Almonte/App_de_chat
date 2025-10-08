package com.example.app_de_chat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/chat-android-firebase-d832b/messages:send";
    private static final String SCOPES = "https://www.googleapis.com/auth/firebase.messaging";

    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;
    private OkHttpClient httpClient;

    public NotificationHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.httpClient = new OkHttpClient();

        Log.d(TAG, "NotificationHelper initialized");
        verifyServiceAccountFile();

        createNotificationChannel();
        startListeningForNotifications();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Chat Messages";
            String description = "Notifications for new chat messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void startListeningForNotifications() {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();

        // Listen for new notifications addressed to current user
        notificationListener = db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            if (doc.exists()) {
                                String senderName = doc.getString("senderName");
                                String message = doc.getString("message");
                                String senderId = doc.getString("senderId");

                                showNotification(senderName, message, senderId, senderName);

                                // Mark notification as read
                                doc.getReference().update("read", true);
                            }
                        }
                    }
                });
    }

    // Agregar este método a NotificationHelper para verificar el archivo
    private void verifyServiceAccountFile() {
        try {
            InputStream serviceAccount = context.getAssets().open("service-account.json");
            Log.d(TAG, "Service account file found and accessible");
            serviceAccount.close();
        } catch (IOException e) {
            Log.e(TAG, "Service account file NOT found: " + e.getMessage());
        }
    }

    public void sendNotificationToUser(String receiverId, String senderName, String messageText) {
        Log.d(TAG, "sendNotificationToUser called - receiverId: " + receiverId + ", senderName: " + senderName);

        String currentUserId = FirebaseAuth.getInstance().getUid();
        Log.d(TAG, "Current senderId: " + currentUserId);

        if (currentUserId != null && !currentUserId.equals(receiverId)) {
            // Use addListenerForSingleValueEvent instead of addSnapshotListener
            db.collection("users").document(receiverId)
                    .get()  // Changed from addSnapshotListener to get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Log.d(TAG, "User document retrieved successfully");
                        if (documentSnapshot.exists()) {
                            String fcmToken = documentSnapshot.getString("fcmToken");
                            Log.d(TAG, "FCM Token for user " + receiverId + ": " + (fcmToken != null ? "EXISTS" : "NULL"));

                            if (fcmToken != null && !fcmToken.isEmpty()) {
                                String notificationTitle = "New message from " + senderName;
                                String notificationBody = messageText;

                                Log.d(TAG, "Sending push notification with FCM token");
                                sendPushNotification(fcmToken, notificationTitle, notificationBody, currentUserId, senderName);
                            } else {
                                Log.w(TAG, "No FCM token found for user: " + receiverId);
                            }
                        } else {
                            Log.w(TAG, "User document does not exist for: " + receiverId);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve user document: " + e.getMessage()));
        } else {
            Log.d(TAG, "Not sending notification - either currentUserId is null or same as receiverId");
        }
    }

    private void sendFCMNotification(String receiverId, String senderName, String message, String senderId) {
        Log.d(TAG, "sendFCMNotification called for receiverId: " + receiverId);

        db.collection("users").document(receiverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "User document retrieved successfully");
                    if (documentSnapshot.exists()) {
                        String fcmToken = documentSnapshot.getString("fcmToken");
                        Log.d(TAG, "FCM Token for user " + receiverId + ": " + (fcmToken != null ? "EXISTS" : "NULL"));

                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            Log.d(TAG, "Sending push notification with FCM token");
                            sendPushNotification(fcmToken, senderName, message, senderId, senderName);
                        } else {
                            Log.w(TAG, "No FCM token found for user: " + receiverId);
                        }
                    } else {
                        Log.e(TAG, "User document does not exist for: " + receiverId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user FCM token: " + e.getMessage());
                });
    }

    private void sendPushNotification(String fcmToken, String title, String body, String senderId, String senderName) {
        Log.d(TAG, "sendPushNotification called");

        // Ejecutar en segundo plano
        new Thread(() -> {
            try {
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    Log.e(TAG, "Failed to get access token");
                    return;
                }

                JSONObject message = new JSONObject();
                message.put("token", fcmToken);

                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", body);
                message.put("notification", notification);

                JSONObject data = new JSONObject();
                data.put("senderId", senderId);
                data.put("senderName", senderName);
                data.put("title", title);
                data.put("body", body);
                message.put("data", data);

                JSONObject android = new JSONObject();
                android.put("priority", "high");
                message.put("android", android);

                JSONObject payload = new JSONObject();
                payload.put("message", message);

                RequestBody requestBody = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(FCM_URL)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Failed to send FCM notification: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "FCM notification sent successfully");
                        } else {
                            Log.e(TAG, "Failed to send FCM notification: " + response.message());
                            String responseBody = response.body().string();
                            Log.e(TAG, "Response body: " + responseBody);
                        }
                        response.close();
                    }
                });

            } catch (JSONException e) {
                Log.e(TAG, "Error creating FCM message: " + e.getMessage());
            }
        }).start();
    }

    private String getAccessToken() {
        Log.d(TAG, "Getting access token...");
        try {
            InputStream serviceAccount = context.getAssets().open("service-account.json");
            Log.d(TAG, "Service account file opened successfully");

            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(serviceAccount)
                    .createScoped(Arrays.asList(SCOPES));
            Log.d(TAG, "Google credentials created");

            googleCredentials.refresh();
            Log.d(TAG, "Credentials refreshed successfully");

            String token = googleCredentials.getAccessToken().getTokenValue();
            Log.d(TAG, "Access token obtained: " + (token != null ? "SUCCESS" : "NULL"));

            return token;
        } catch (IOException e) {
            Log.e(TAG, "Error getting access token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void showNotification(String title, String body, String senderId, String senderName) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("userId", senderId);
        intent.putExtra("userName", senderName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "You have a new message")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    public void stopListening() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}
