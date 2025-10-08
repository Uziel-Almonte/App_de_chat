package com.example.app_de_chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.net.Uri;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    String receiverId, receiverName, senderRoom, receiverRoom;
    DatabaseReference dbReferenceSender, dbReferenceReceiver, userReference;
    String senderId, senderName, senderImage;
    FirebaseAuth mAuth;
    FirebaseUser user;

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imagePickerBtn;

    long timestamp;

    ImageView sendBtn;
    EditText messageText;
    RecyclerView recyclerView;

    MessageAdapter messageAdapter;
    NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user != null) {
            // Cargar displayName desde Firestore
            loadCurrentUserDisplayName();
        }

        // Set up the toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);

        // inicializar variables del sender
        senderId = FirebaseAuth.getInstance().getUid();
        userReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize image picker button
        imagePickerBtn = findViewById(R.id.imagePickerIcon);
        imagePickerBtn.setOnClickListener(v -> openImagePicker());

        // recibir informacion del receptor
        receiverId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("userName");

        // Try to resolve receiver display name if missing or equals the UID
        resolveReceiverNameIfNeeded();

        // cargar informacion del sender
        loadSenderInfo();

        if (receiverId != null) {
            senderRoom = senderId + receiverId;
            receiverRoom = receiverId + senderId;
        }

        // inicializar vistas
        sendBtn = findViewById(R.id.sendMessageIcon);
        messageAdapter = new MessageAdapter(this);
        recyclerView = findViewById(R.id.chatrecycler);
        messageText = findViewById(R.id.messageEdit);

        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize database references
        dbReferenceSender = FirebaseDatabase.getInstance().getReference("chats").child(senderRoom);
        dbReferenceReceiver = FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom);

        // encargar mesnajes
        loadMessages();

        // Set send button click listener
        sendBtn.setOnClickListener(v -> {
            String message = messageText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageText.setText("");
            } else {
                Toast.makeText(ChatActivity.this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Guardar conversación reciente automáticamente
        saveRecentChat();
    }

    private void resolveReceiverNameIfNeeded() {
        if (receiverId == null) return;
        boolean needsResolve = (receiverName == null || receiverName.isEmpty() || receiverName.equals(receiverId));
        if (!needsResolve) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(receiverName);
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(receiverId).get()
                .addOnSuccessListener(doc -> {
                    String dn = doc.getString("displayName");
                    String email = doc.getString("email");
                    String best = (dn != null && !dn.isEmpty()) ? dn : (email != null && !email.isEmpty()) ? email : receiverId;
                    receiverName = best;
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(receiverName);
                    // Backfill our recent_chats entry with the proper name for next time
                    if (senderId != null) {
                        FirebaseFirestore.getInstance()
                                .collection("recent_chats").document(senderId)
                                .collection("chats").document(receiverId)
                                .update("userName", receiverName);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(receiverName != null ? receiverName : receiverId);
                });
    }

    private void loadCurrentUserDisplayName() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        senderName = documentSnapshot.getString("displayName");
                        if (senderName == null || senderName.isEmpty()) {
                            senderName = user.getEmail();
                        }
                        Log.d("ChatActivity", "Loaded displayName from Firestore: " + senderName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Failed to load displayName from Firestore", e);
                    senderName = user.getEmail();
                });
    }

    private void loadSenderInfo() {
        userReference.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    senderName = snapshot.child("senderName").getValue(String.class);
                    if (senderName == null) {
                        // Usar email como fallback
                        senderName = "test2";
                    }
                    senderImage = snapshot.child("imageUri").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        dbReferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MessageModel> messages = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel messageModel = dataSnapshot.getValue(MessageModel.class);
                    if (messageModel != null) {
                        messages.add(messageModel);
                    }
                }

                // ordena los menajes por timestamp
                messages.sort((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));


                messageAdapter.clear();
                for (MessageModel message : messages) {
                    messageAdapter.add(message);
                }

                // se puede hacer un scroll al ultimo mensaje si hay mensajes
                if (!messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String message) {
        Log.d("ChatActivity", "sendMessage called with message: " + message);

        String messageId = UUID.randomUUID().toString();
        MessageModel messageModel = new MessageModel(messageId, senderId, senderName, message);

        // Save to sender's chat room
        dbReferenceSender.child(messageId).setValue(messageModel)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("ChatActivity", "Message saved to sender's room successfully");
                            // guardar el el chat del receptor
                            dbReferenceReceiver.child(messageId).setValue(messageModel);
                            // Update recent chats for both users (with text preview)
                            updateRecentChats(message);
                            // Send notification to receiver
                            Log.d("ChatActivity", "Calling sendNotificationToReceiver");
                            sendNotificationToReceiver(message);
                        } else {
                            Log.e("ChatActivity", "Failed to save message to sender's room");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Scroll hacia el ultimo mensaje
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageAndSendMessage(imageUri);
        }
    }

    private void uploadImageAndSendMessage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        try {
            // Convertir imagen a Base64
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Redimensionar imagen para reducir tamaño
            Bitmap resizedBitmap = resizeImage(bitmap, 800, 600);

            // Convertir a Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Enviar mensaje con imagen
            sendImageMessage(base64Image);

            Toast.makeText(this, "Image sent successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Método para redimensionar imagen
    private Bitmap resizeImage(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float aspectRatio = (float) width / height;

        if (width > height) {
            width = maxWidth;
            height = (int) (width / aspectRatio);
        } else {
            height = maxHeight;
            width = (int) (height * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    // Actualizar sendImageMessage para usar Base64
    private void sendImageMessage(String base64Image) {
        String messageId = UUID.randomUUID().toString();
        MessageModel messageModel = new MessageModel(messageId, senderId, senderName, base64Image, true);

        // Save to sender's chat room
        dbReferenceSender.child(messageId).setValue(messageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dbReferenceReceiver.child(messageId).setValue(messageModel);
                        // Update recent chats for both users (image preview)
                        updateRecentChats("Imagen");
                        sendNotificationToReceiver("Sent an image");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void saveRecentChat() {
        if (senderId == null || receiverId == null || receiverName == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String path = "recent_chats/" + senderId + "/chats";
        db.collection(path)
                .document(receiverId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", receiverId);
                        data.put("userName", receiverName);
                        data.put("lastMessage", "");
                        data.put("timestamp", System.currentTimeMillis());
                        document.getReference().set(data);
                    }
                });
    }

    // Update recent chat entries for both users with last message preview and timestamp
    private void updateRecentChats(String lastMessagePreview) {
        if (senderId == null || receiverId == null) return;
        long now = System.currentTimeMillis();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Sender's view (shows receiver info)
        Map<String, Object> senderEntry = new HashMap<>();
        senderEntry.put("userId", receiverId);
        senderEntry.put("userName", receiverName);
        senderEntry.put("lastMessage", lastMessagePreview);
        senderEntry.put("timestamp", now);
        firestore.collection("recent_chats")
                .document(senderId)
                .collection("chats")
                .document(receiverId)
                .set(senderEntry);

        // Receiver's view (shows sender info)
        String otherName = (senderName != null && !senderName.isEmpty()) ? senderName : (user != null ? user.getEmail() : "Usuario");
        Map<String, Object> receiverEntry = new HashMap<>();
        receiverEntry.put("userId", senderId);
        receiverEntry.put("userName", otherName);
        receiverEntry.put("lastMessage", lastMessagePreview);
        receiverEntry.put("timestamp", now);
        firestore.collection("recent_chats")
                .document(receiverId)
                .collection("chats")
                .document(senderId)
                .set(receiverEntry);
    }

    private void sendNotificationToReceiver(String message) {
        String finalSenderName;
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            finalSenderName = user.getDisplayName();
        } else if (senderName != null && !senderName.isEmpty()) {
            // Usar el senderName cargado de Firebase
            finalSenderName = senderName;
        } else if (user != null && user.getEmail() != null) {
            // Fallback al email
            finalSenderName = user.getEmail();
        } else {
            finalSenderName = "Unknown User";
        }

        Log.d("ChatActivity", "receiverId: " + receiverId);
        Log.d("ChatActivity", "finalSenderName: " + finalSenderName);
        Log.d("ChatActivity", "message: " + message);
        Log.d("ChatActivity", "notificationHelper null? " + (notificationHelper == null));

        if (notificationHelper != null && receiverId != null && finalSenderName != null) {
            Log.d("ChatActivity", "About to call notificationHelper.sendNotificationToUser");
            notificationHelper.sendNotificationToUser(receiverId, finalSenderName, message);
        } else {
            Log.e("ChatActivity", "Cannot send notification - missing data");
            if (notificationHelper == null) Log.e("ChatActivity", "notificationHelper is null");
            if (receiverId == null) Log.e("ChatActivity", "receiverId is null");
            if (finalSenderName == null) Log.e("ChatActivity", "finalSenderName is null");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationHelper != null) {
            notificationHelper.stopListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button press
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ChatActivity.this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
