package com.example.app_de_chat;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    private long timestamp;

    // Constructor vacío requerido para Firebase
    public MessageModel() {
    }

    public MessageModel(String messageId, String senderId, String senderName, String message) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
