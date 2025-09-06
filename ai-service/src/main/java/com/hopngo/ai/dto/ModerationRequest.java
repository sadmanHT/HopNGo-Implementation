package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ModerationRequest {
    
    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;
    
    @NotNull(message = "Content type is required")
    private String contentType; // "text", "image", "mixed"
    
    private List<String> mediaUrls;
    
    private String userId;
    
    private String contextType; // "post", "review", "listing", "comment"
    
    // Constructors
    public ModerationRequest() {}
    
    public ModerationRequest(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }
    
    public ModerationRequest(String content, String contentType, List<String> mediaUrls, String userId, String contextType) {
        this.content = content;
        this.contentType = contentType;
        this.mediaUrls = mediaUrls;
        this.userId = userId;
        this.contextType = contextType;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public List<String> getMediaUrls() {
        return mediaUrls;
    }
    
    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getContextType() {
        return contextType;
    }
    
    public void setContextType(String contextType) {
        this.contextType = contextType;
    }
}