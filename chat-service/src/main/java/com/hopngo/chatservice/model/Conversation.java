package com.hopngo.chatservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "conversations")
public class Conversation {
    
    @Id
    private String id;
    
    private ConversationType type;
    
    @Indexed
    private List<String> memberIds;
    
    private LocalDateTime createdAt;
    
    private String name; // Optional: for group conversations
    
    private String lastMessageId;
    
    private LocalDateTime lastMessageAt;
    
    // Constructors
    public Conversation() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Conversation(ConversationType type, List<String> memberIds) {
        this();
        this.type = type;
        this.memberIds = memberIds;
    }
    
    public Conversation(ConversationType type, List<String> memberIds, String name) {
        this(type, memberIds);
        this.name = name;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLastMessageId() {
        return lastMessageId;
    }
    
    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}