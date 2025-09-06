package com.hopngo.chatservice.dto;

import java.time.LocalDateTime;

public class ChatMessageCreatedEvent {
    
    private String messageId;
    private String convoId;
    private String senderId;
    private String preview;
    private LocalDateTime sentAt;
    
    // Constructors
    public ChatMessageCreatedEvent() {}
    
    public ChatMessageCreatedEvent(String messageId, String convoId, String senderId, String preview, LocalDateTime sentAt) {
        this.messageId = messageId;
        this.convoId = convoId;
        this.senderId = senderId;
        this.preview = preview;
        this.sentAt = sentAt;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
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
    
    public String getPreview() {
        return preview;
    }
    
    public void setPreview(String preview) {
        this.preview = preview;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}