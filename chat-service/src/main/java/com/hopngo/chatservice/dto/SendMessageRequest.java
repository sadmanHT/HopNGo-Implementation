package com.hopngo.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {
    
    @NotNull(message = "Conversation ID is required")
    @NotBlank(message = "Conversation ID cannot be blank")
    private String convoId;
    
    @NotNull(message = "Message body is required")
    @NotBlank(message = "Message body cannot be blank")
    @Size(max = 2000, message = "Message body cannot exceed 2000 characters")
    private String body;
    
    @Size(max = 500, message = "Media URL cannot exceed 500 characters")
    private String mediaUrl;
    
    // Constructors
    public SendMessageRequest() {}
    
    public SendMessageRequest(String convoId, String body) {
        this.convoId = convoId;
        this.body = body;
    }
    
    public SendMessageRequest(String convoId, String body, String mediaUrl) {
        this.convoId = convoId;
        this.body = body;
        this.mediaUrl = mediaUrl;
    }
    
    // Getters and Setters
    public String getConvoId() {
        return convoId;
    }
    
    public void setConvoId(String convoId) {
        this.convoId = convoId;
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
}