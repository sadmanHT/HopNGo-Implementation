package com.hopngo.chatservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "messages")
public class Message {
    
    @Id
    private String id;
    
    @Indexed
    private String convoId;
    
    @Indexed
    private String senderId;
    
    private String body;
    
    private String mediaUrl;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime deliveredAt;
    
    private List<String> readBy;
    
    // Constructors
    public Message() {
        this.sentAt = LocalDateTime.now();
        this.readBy = new ArrayList<>();
    }
    
    public Message(String convoId, String senderId, String body) {
        this();
        this.convoId = convoId;
        this.senderId = senderId;
        this.body = body;
    }
    
    public Message(String convoId, String senderId, String body, String mediaUrl) {
        this(convoId, senderId, body);
        this.mediaUrl = mediaUrl;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getConvoId() {
        return convoId;
    }
    
    public void setConvoId(String convoId) {
        this.convoId = convoId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public String getMediaUrl() {
        return mediaUrl;
    }
    
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public List<String> getReadBy() {
        return readBy;
    }
    
    public void setReadBy(List<String> readBy) {
        this.readBy = readBy;
    }
    
    // Helper methods
    public void markAsRead(String userId) {
        if (!readBy.contains(userId)) {
            readBy.add(userId);
        }
    }
    
    public boolean isReadBy(String userId) {
        return readBy.contains(userId);
    }
    
    public String getPreview() {
        if (body != null && body.length() > 100) {
            return body.substring(0, 100) + "...";
        }
        return body;
    }
}