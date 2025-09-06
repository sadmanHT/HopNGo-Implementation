package com.hopngo.chatservice.dto;

import com.hopngo.chatservice.model.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateConversationRequest {
    
    @NotNull(message = "Conversation type is required")
    private ConversationType type;
    
    @NotEmpty(message = "Member IDs cannot be empty")
    @Size(min = 1, max = 50, message = "Conversation must have between 1 and 50 members")
    private List<String> memberIds;
    
    @Size(max = 100, message = "Conversation name cannot exceed 100 characters")
    private String name;
    
    // Constructors
    public CreateConversationRequest() {}
    
    public CreateConversationRequest(ConversationType type, List<String> memberIds) {
        this.type = type;
        this.memberIds = memberIds;
    }
    
    public CreateConversationRequest(ConversationType type, List<String> memberIds, String name) {
        this.type = type;
        this.memberIds = memberIds;
        this.name = name;
    }
    
    // Getters and Setters
    public ConversationType getType() {
        return type;
    }
    
    public void setType(ConversationType type) {
        this.type = type;
    }
    
    public List<String> getMemberIds() {
        return memberIds;
    }
    
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}