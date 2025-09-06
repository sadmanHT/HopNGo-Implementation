package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatbotRequest {
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String location;
    private String itineraryId;
    private String profile;
    private String conversationId;
    
    // Constructors
    public ChatbotRequest() {}
    
    public ChatbotRequest(String message, String location, String itineraryId, String profile, String conversationId) {
        this.message = message;
        this.location = location;
        this.itineraryId = itineraryId;
        this.profile = profile;
        this.conversationId = conversationId;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getProfile() {
        return profile;
    }
    
    public void setProfile(String profile) {
        this.profile = profile;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}