package com.example.app_de_chat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CHANNEL_NAME = "Chat Messages";
    private static final String CHANNEL_DESCRIPTION = "Notifications for new chat messages";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String senderId = remoteMessage.getData().get("senderId");
            String senderName = remoteMessage.getData().get("senderName");

            if (title != null && body != null) {
                showNotification(title, body, senderId, senderName);
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            String senderId = remoteMessage.getData().get("senderId");
            String senderName = remoteMessage.getData().get("senderName");

            showNotification(title, body, senderId, senderName);
        }
    }

    private void showNotification(String title, String body, String senderId, String senderName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("userId", senderId);
        intent.putExtra("userName", senderName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "You have a new message")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Save the token to Firestore for the current user
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnFailureListener(e -> {
                        // If update fails, try to set the token
                        db.collection("users").document(userId)
                                .set(new java.util.HashMap<String, Object>() {{
                                    put("fcmToken", token);
                                }}, com.google.firebase.firestore.SetOptions.merge());
                    });
        }
    }
}
