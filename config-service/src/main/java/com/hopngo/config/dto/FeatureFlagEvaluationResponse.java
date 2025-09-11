package com.hopngo.config.dto;

import java.util.Map;

public class FeatureFlagEvaluationResponse {
    
    private String userId;
    private Map<String, Boolean> flags;
    private long evaluatedAt;
    
    // Constructors
    public FeatureFlagEvaluationResponse() {
        this.evaluatedAt = System.currentTimeMillis();
    }
    
    public FeatureFlagEvaluationResponse(String userId, Map<String, Boolean> flags) {
        this.userId = userId;
        this.flags = flags;
        this.evaluatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Map<String, Boolean> getFlags() {
        return flags;
    }
    
    public void setFlags(Map<String, Boolean> flags) {
        this.flags = flags;
    }
    
    public long getEvaluatedAt() {
        return evaluatedAt;
    }
    
    public void setEvaluatedAt(long evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
    
    @Override
    public String toString() {
        return "FeatureFlagEvaluationResponse{" +
                "userId='" + userId + '\'' +
                ", flags=" + flags +
                ", evaluatedAt=" + evaluatedAt +
                '}';
    }
}