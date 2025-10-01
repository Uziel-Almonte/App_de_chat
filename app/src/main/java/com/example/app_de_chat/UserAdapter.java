package com.example.app_de_chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;
    private Context context;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter() {
        this.userList = new ArrayList<>();
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.userList = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView textEmail;
        private TextView textName;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textEmail = itemView.findViewById(R.id.text_user_email);
            textName = itemView.findViewById(R.id.text_user_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        listener.onUserClick(userList.get(getAdapterPosition()));
                    }
                }
            });
        }

        public void bind(User user) {
            textEmail.setText(user.getEmail());
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                textName.setText(displayName);
                textName.setVisibility(View.VISIBLE);
            } else {
                textName.setVisibility(View.GONE);
            }
        }
    }
}
