package com.hopngo.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.multipart.MultipartFile;

public class ImageSearchRequest {
    
    private String imageUrl;
    
    private MultipartFile imageFile;
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    private int limit = 20;
    
    @Min(value = 0, message = "Threshold must be between 0 and 1")
    @Max(value = 1, message = "Threshold must be between 0 and 1")
    private double threshold = 0.7;
    
    private String contentType = "all"; // "stay", "tour", "all"
    
    private String userId; // For personalization
    private String query; // Text query for hybrid search
    
    // Constructors
    public ImageSearchRequest() {}
    
    public ImageSearchRequest(String imageUrl, int limit) {
        this.imageUrl = imageUrl;
        this.limit = limit;
    }
    
    public ImageSearchRequest(MultipartFile imageFile, int limit) {
        this.imageFile = imageFile;
        this.limit = limit;
    }
    
    // Getters and Setters
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public MultipartFile getImageFile() {
        return imageFile;
    }
    
    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    @Override
    public String toString() {
        return "ImageSearchRequest{" +
                "imageUrl='" + imageUrl + '\'' +
                ", imageFile=" + (imageFile != null ? imageFile.getOriginalFilename() : "null") +
                ", limit=" + limit +
                ", threshold=" + threshold +
                ", contentType='" + contentType + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}