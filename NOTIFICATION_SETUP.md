# Notification Setup Instructions

## What I've Done:

## Important: FCM Server Key Setup

In the `NotificationHelper.java` file, you need to replace this line:
```java
private static final String FCM_SERVER_KEY = "YOUR_FCM_SERVER_KEY_HERE";
```

**To get your FCM Server Key:**
1. Go to Firebase Console (https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings (gear icon)
4. Go to "Cloud Messaging" tab
5. Copy the "Server key" (legacy key)
6. Replace "YOUR_FCM_SERVER_KEY_HERE" with your actual key
7. if it says permission denied after putting the fcm key, paste this in the firestore rules:
   ``rules_version = '2';
   service cloud.firestore {
   match /databases/{database}/documents {
   // Allow users to read/write their own FCM tokens
   match /users/{userId} {
   allow read, write: if request.auth != null && request.auth.uid == userId;
   }

   // Allow users to read/write notifications for themselves
   match /notifications/{document} {
   allow read, write: if request.auth != null &&
   (resource.data.receiverId == request.auth.uid ||
   resource.data.senderId == request.auth.uid);
   }
   }
   }```

## How Notifications Work Now:

1. **On Login/Registration**: FCM tokens are automatically saved to Firestore
2. **When sending messages**: 
   - Message is saved to Firebase Realtime Database
   - Notification document is created in Firestore
   - FCM push notification is sent to recipient's device
3. **On receiving**: 
   - MyFirebaseMessagingService handles incoming notifications
   - Local notifications are displayed
   - Tapping notification opens the chat

## Android 14 Compatibility:

- Notification permissions are requested for Android 13+
- Proper notification channels are created
- Intent flags are set correctly for Android 14
- PendingIntent uses FLAG_IMMUTABLE

## Testing:

1. Install the app on two devices/emulators
2. Register/login on both devices
3. Check logs for "FCM Registration Token" messages
4. Send messages between users
5. You should see notifications appear

## Troubleshooting:

- Check device notification settings if notifications don't appear
- Verify Firebase project setup and google-services.json
- Check logcat for any error messages
- Ensure both devices have internet connection
