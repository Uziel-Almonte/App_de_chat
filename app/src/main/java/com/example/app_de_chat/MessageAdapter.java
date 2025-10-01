package com.example.app_de_chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private Context context;
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
        if (messageModelList.get(position).getSenderId().equals(FirebaseAuth.getInstance().getUid())) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.message_row_sent, parent, false);
            return new MessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.message_row_received, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);
        holder.bind(messageModel);
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderName, timestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderName = itemView.findViewById(R.id.senderName);
            timestamp = itemView.findViewById(R.id.timestamp);
        }

        public void bind(MessageModel message) {
            if (messageText != null) {
                messageText.setText(message.getMessage());
            }

            // mandar nombre del sender (solo para mensajes recibidos)
            if (senderName != null && message.getSenderName() != null) {
                senderName.setText(message.getSenderName());
            }

            // Formatear timestamp
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String time = sdf.format(new Date(message.getTimestamp()));
                timestamp.setText(time);
            }
        }
    }
}
