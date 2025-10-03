package com.example.app_de_chat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "chat_notifications";
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;
    private OkHttpClient httpClient;

    // You'll need to get this from your Firebase console
    private static final String FCM_SERVER_KEY = "YOUR_FCM_SERVER_KEY_HERE";

    public NotificationHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.httpClient = new OkHttpClient();
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

    public void sendNotificationToUser(String receiverId, String senderName, String message) {
        if (auth.getCurrentUser() == null) return;

        String senderId = auth.getCurrentUser().getUid();

        // First, create notification document in Firestore for fallback
        Map<String, Object> notification = new HashMap<>();
        notification.put("receiverId", receiverId);
        notification.put("senderId", senderId);
        notification.put("senderName", senderName);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification document created successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification document: " + e.getMessage());
                });

        // Send FCM push notification
        sendFCMNotification(receiverId, senderName, message, senderId);
    }

    private void sendFCMNotification(String receiverId, String senderName, String message, String senderId) {
        // Get the receiver's FCM token
        db.collection("users").document(receiverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fcmToken = documentSnapshot.getString("fcmToken");
                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            sendPushNotification(fcmToken, senderName, message, senderId, senderName);
                        } else {
                            Log.w(TAG, "No FCM token found for user: " + receiverId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user FCM token: " + e.getMessage());
                });
    }

    private void sendPushNotification(String fcmToken, String title, String body, String senderId, String senderName) {
        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);

            JSONObject data = new JSONObject();
            data.put("senderId", senderId);
            data.put("senderName", senderName);
            data.put("title", title);
            data.put("body", body);

            JSONObject message = new JSONObject();
            message.put("to", fcmToken);
            message.put("notification", notification);
            message.put("data", data);
            message.put("priority", "high");

            RequestBody requestBody = RequestBody.create(
                    message.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + FCM_SERVER_KEY)
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
                    }
                    response.close();
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating FCM message: " + e.getMessage());
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
