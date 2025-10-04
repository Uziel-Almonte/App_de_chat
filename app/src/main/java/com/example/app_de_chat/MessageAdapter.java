package com.example.app_de_chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
    private static Context context;
    private final List<MessageModel> messageModelList;
    private String currentUserId;

    public MessageAdapter(Context context) {
        this.context = context;
        this.messageModelList = new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void add(MessageModel messageModel) {
        for (MessageModel existing : messageModelList) {
            if (existing.getMessageId().equals(messageModel.getMessageId())) {
                return;
            }
        }

        messageModelList.add(messageModel);
        notifyItemInserted(messageModelList.size() - 1);
    }

    public void remove(MessageModel messageModel) {
        int position = messageModelList.indexOf(messageModel);
        if (position != -1) {
            messageModelList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        messageModelList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageModelList.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);
        boolean isImage = "image".equals(message.getMessageType());

        if (isSent && isImage) {
            return VIEW_TYPE_SENT_IMAGE;
        } else if (isSent) {
            return VIEW_TYPE_SENT;
        } else if (isImage) {
            return VIEW_TYPE_RECEIVED_IMAGE;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case VIEW_TYPE_SENT:
                view = inflater.inflate(R.layout.message_row_sent, parent, false);
                break;
            case VIEW_TYPE_RECEIVED:
                view = inflater.inflate(R.layout.message_row_received, parent, false);
                break;
            case VIEW_TYPE_SENT_IMAGE:
                view = inflater.inflate(R.layout.message_row_sent_image, parent, false);
                break;
            case VIEW_TYPE_RECEIVED_IMAGE:
                view = inflater.inflate(R.layout.message_row_received_image, parent, false);
                break;
            default:
                view = inflater.inflate(R.layout.message_row_received, parent, false);
        }

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);
        holder.bind(messageModel, context);
    }

    @Override
    public int getItemCount() {

        return messageModelList.size();

    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderName, timestamp;
        ImageView messageImage; // For image messages



        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderName = itemView.findViewById(R.id.senderName);
            timestamp = itemView.findViewById(R.id.timestamp);
            messageImage = itemView.findViewById(R.id.messageImage);
        }

        public void bind(MessageModel message, Context context) {
            // Handle image/text message display logic
            if ("image".equals(message.getMessageType())) {
                if (messageImage != null) {
                    messageImage.setVisibility(View.VISIBLE);

                    // Verificar si es Base64 o URL
                    if (message.getBase64Image() != null && !message.getBase64Image().isEmpty()) {
                        // Decodificar Base64
                        try {
                            byte[] decodedBytes = Base64.decode(message.getBase64Image(), Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            messageImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            messageImage.setImageResource(R.drawable.ic_menu_gallery);
                        }
                    } else if (message.getImageUrl() != null) {
                        // Usar Glide para URLs normales
                        Glide.with(context)
                                .load(message.getImageUrl())
                                .placeholder(R.drawable.ic_menu_gallery)
                                .error(R.drawable.ic_menu_gallery)
                                .into(messageImage);
                    }
                }
                if (messageText != null) {
                    messageText.setVisibility(View.GONE);
                }
            } else {
                // Handle text message
                if (messageText != null && message.getMessage() != null) {
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.getMessage());
                }
                if (messageImage != null) {
                    messageImage.setVisibility(View.GONE);
                }
            }

            // Set sender name y timestamp
            if (senderName != null && message.getSenderName() != null) {
                senderName.setText(message.getSenderName());
            }

            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String time = sdf.format(new Date(message.getTimestamp()));
                timestamp.setText(time);
            }
        }
    }
}
