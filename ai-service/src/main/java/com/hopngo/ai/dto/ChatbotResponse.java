package com.hopngo.ai.dto;

import java.util.List;

public class ChatbotResponse {
    
    private String response;
    private String conversationId;
    private List<String> suggestions;
    private String context;
    private boolean requiresFollowUp;
    
    // Constructors
    public ChatbotResponse() {}
    
    public ChatbotResponse(String response, String conversationId, List<String> suggestions, 
                          String context, boolean requiresFollowUp) {
        this.response = response;
        this.conversationId = conversationId;
        this.suggestions = suggestions;
        this.context = context;
        this.requiresFollowUp = requiresFollowUp;
    }
    
    // Getters and Setters
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public boolean isRequiresFollowUp() {
        return requiresFollowUp;
    }
    
    public void setRequiresFollowUp(boolean requiresFollowUp) {
        this.requiresFollowUp = requiresFollowUp;
    }
}