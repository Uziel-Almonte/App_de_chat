package com.example.app_de_chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentChatsAdapter extends RecyclerView.Adapter<RecentChatsAdapter.RecentChatViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RecentChatItem item);
    }

    private final List<RecentChatItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<RecentChatItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat, parent, false);
        return new RecentChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentChatViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecentChatViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView lastMessageView;
        TextView timeView;

        public RecentChatViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_user_name);
            lastMessageView = itemView.findViewById(R.id.text_last_message);
            timeView = itemView.findViewById(R.id.text_time);
        }

        void bind(RecentChatItem item, OnItemClickListener listener) {
            nameView.setText(item.getUserName() != null ? item.getUserName() : item.getUserId());
            String preview = item.getLastMessage();
            if (preview == null || preview.isEmpty()) preview = "Conversación";
            lastMessageView.setText(preview);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeView.setText(sdf.format(new Date(item.getTimestamp())));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}

