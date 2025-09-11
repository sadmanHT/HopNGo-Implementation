package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class SimilarityRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private String itemId;
    private String itemType; // "stay" or "tour"
    
    @Positive(message = "Limit must be positive")
    private int limit = 10;
    
    private List<String> excludeIds;
    private String location; // lat,lng format
    private Double maxDistance; // in km
    
    // Constructors
    public SimilarityRequest() {}
    
    public SimilarityRequest(String userId, int limit) {
        this.userId = userId;
        this.limit = limit;
    }
    
    public SimilarityRequest(String userId, String itemId, String itemType, int limit) {
        this.userId = userId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.limit = limit;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getItemType() {
        return itemType;
    }
    
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public List<String> getExcludeIds() {
        return excludeIds;
    }
    
    public void setExcludeIds(List<String> excludeIds) {
        this.excludeIds = excludeIds;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Double getMaxDistance() {
        return maxDistance;
    }
    
    public void setMaxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
    }
}