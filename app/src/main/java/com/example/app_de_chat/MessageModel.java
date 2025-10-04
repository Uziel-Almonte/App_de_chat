package com.example.app_de_chat;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    private long timestamp;
    private String imageUrl;
    private String messageType; // entre dos tipos: "text" o "image"
    private String base64Image;     // Para imágenes Base64

    // Constructor vacío requerido para Firebase
    public MessageModel() {
    }

    public MessageModel(String messageId, String senderId, String senderName, String message) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.messageType = "text";
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor para imágenes Base64
    public MessageModel(String messageId, String senderId, String senderName, String base64Image, boolean isImage) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.base64Image = base64Image;
        this.messageType = "image";
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }
}
