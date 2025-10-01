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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    long timestamp;

    ImageView sendBtn;
    EditText messageText;
    RecyclerView recyclerView;

    MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        

        // inicializar variables del sender
        senderId = FirebaseAuth.getInstance().getUid();
        userReference = FirebaseDatabase.getInstance().getReference("users");

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
