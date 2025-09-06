package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class ImageSearchRequest {
    
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    
    private int maxResults = 10;
    
    // Constructors
    public ImageSearchRequest() {}
    
    public ImageSearchRequest(String imageUrl, int maxResults) {
        this.imageUrl = imageUrl;
        this.maxResults = maxResults;
    }
    
    // Getters and Setters
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}