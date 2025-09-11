package com.hopngo.ai.dto;

import java.util.List;
import java.util.Map;

public class SimilarityResponse {
    
    private List<SimilarItem> recommendations;
    private String algorithm;
    private String cacheStatus;
    private long processingTimeMs;
    private Map<String, Object> metadata;
    
    // Constructors
    public SimilarityResponse() {}
    
    public SimilarityResponse(List<SimilarItem> recommendations, String algorithm) {
        this.recommendations = recommendations;
        this.algorithm = algorithm;
    }
    
    // Getters and Setters
    public List<SimilarItem> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<SimilarItem> recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    public String getCacheStatus() {
        return cacheStatus;
    }
    
    public void setCacheStatus(String cacheStatus) {
        this.cacheStatus = cacheStatus;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // Inner class for similar items
    public static class SimilarItem {
        private String id;
        private String type; // "user", "stay", "tour"
        private String title;
        private String description;
        private String imageUrl;
        private String location;
        private Double price;
        private Double rating;
        private Integer reviewCount;
        private Double similarityScore;
        private String reason; // Why this was recommended
        private Map<String, Object> attributes;
        
        // Constructors
        public SimilarItem() {}
        
        public SimilarItem(String id, String type, String title, Double similarityScore) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.similarityScore = similarityScore;
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public Double getPrice() {
            return price;
        }
        
        public void setPrice(Double price) {
            this.price = price;
        }
        
        public Double getRating() {
            return rating;
        }
        
        public void setRating(Double rating) {
            this.rating = rating;
        }
        
        public Integer getReviewCount() {
            return reviewCount;
        }
        
        public void setReviewCount(Integer reviewCount) {
            this.reviewCount = reviewCount;
        }
        
        public Double getSimilarityScore() {
            return similarityScore;
        }
        
        public void setSimilarityScore(Double similarityScore) {
            this.similarityScore = similarityScore;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
    }
}