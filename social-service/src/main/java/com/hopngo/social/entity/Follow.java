package com.hopngo.social.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;



import java.time.LocalDateTime;

@Document(collection = "follows")
public class Follow {
    
    @Id
    private String id;
    
    private String followerId; // User who is following
    
    private String followingId; // User being followed
    
    private LocalDateTime createdAt;
    
    // Constructors
    public Follow() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Follow(String followerId, String followingId) {
        this();
        this.followerId = followerId;
        this.followingId = followingId;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFollowerId() {
        return followerId;
    }
    
    public void setFollowerId(String followerId) {
        this.followerId = followerId;
    }
    
    public String getFollowingId() {
        return followingId;
    }
    
    public void setFollowingId(String followingId) {
        this.followingId = followingId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}