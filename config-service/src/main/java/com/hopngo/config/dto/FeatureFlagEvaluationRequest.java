package com.hopngo.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public class FeatureFlagEvaluationRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotEmpty(message = "At least one feature flag key is required")
    private List<String> flagKeys;
    
    private Map<String, Object> context;
    
    // Constructors
    public FeatureFlagEvaluationRequest() {}
    
    public FeatureFlagEvaluationRequest(String userId, List<String> flagKeys) {
        this.userId = userId;
        this.flagKeys = flagKeys;
    }
    
    public FeatureFlagEvaluationRequest(String userId, List<String> flagKeys, Map<String, Object> context) {
        this.userId = userId;
        this.flagKeys = flagKeys;
        this.context = context;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public List<String> getFlagKeys() {
        return flagKeys;
    }
    
    public void setFlagKeys(List<String> flagKeys) {
        this.flagKeys = flagKeys;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    @Override
    public String toString() {
        return "FeatureFlagEvaluationRequest{" +
                "userId='" + userId + '\'' +
                ", flagKeys=" + flagKeys +
                ", context=" + context +
                '}';
    }
}