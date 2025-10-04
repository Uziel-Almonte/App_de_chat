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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    String receiverId, receiverName, senderRoom, receiverRoom;
    DatabaseReference dbReferenceSender, dbReferenceReceiver, userReference;
    String senderId, senderName, senderImage;

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
        
        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);

        // inicializar variables del sender
        senderId = FirebaseAuth.getInstance().getUid();
        userReference = FirebaseDatabase.getInstance().getReference("users");


        // Initialize image picker button
        imagePickerBtn = findViewById(R.id.imagePickerIcon);

        // Set image picker click listener
        imagePickerBtn.setOnClickListener(v -> openImagePicker());

        // recibir informacion del receptor
        receiverId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("userName");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(receiverName);
        }

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
    }

    private void loadSenderInfo() {
        userReference.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    senderName = snapshot.child("name").getValue(String.class);
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
        String messageId = UUID.randomUUID().toString();
        MessageModel messageModel = new MessageModel(messageId, senderId, senderName, message);

        // Save to sender's chat room
        dbReferenceSender.child(messageId).setValue(messageModel)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // guardar el el chat del receptor
                            dbReferenceReceiver.child(messageId).setValue(messageModel);
                            // Send notification to receiver
                            sendNotificationToReceiver(message);
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
                        sendNotificationToReceiver("Sent an image");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void sendNotificationToReceiver(String message) {
        if (notificationHelper != null && receiverId != null && senderName != null) {
            notificationHelper.sendNotificationToUser(receiverId, senderName, message);
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
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ChatActivity.this, LoginActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
