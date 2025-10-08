package com.example.app_de_chat;

public class RecentChatItem {
    private String userId;      // The other participant's UID
    private String userName;    // The other participant's display name
    private String lastMessage; // Last message preview (or "Imagen")
    private long timestamp;     // Last activity timestamp

    public RecentChatItem() {}

    public RecentChatItem(String userId, String userName, String lastMessage, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

