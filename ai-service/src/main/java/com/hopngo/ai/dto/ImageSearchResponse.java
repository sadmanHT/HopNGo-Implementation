package com.hopngo.ai.dto;

import java.util.List;

public class ImageSearchResponse {
    
    private List<SearchResult> results;
    private int totalResults;
    private String processingTime;
    
    // Constructors
    public ImageSearchResponse() {}
    
    public ImageSearchResponse(List<SearchResult> results, int totalResults, String processingTime) {
        this.results = results;
        this.totalResults = totalResults;
        this.processingTime = processingTime;
    }
    
    // Getters and Setters
    public List<SearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public String getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }
    
    // Inner class for search results
    public static class SearchResult {
        private String type; // 'place', 'post', 'listing'
        private String id;
        private double score;
        private String title;
        private String description;
        private String imageUrl;
        private String location;
        
        // Constructors
        public SearchResult() {}
        
        public SearchResult(String type, String id, double score, String title, 
                          String description, String imageUrl, String location) {
            this.type = type;
            this.id = id;
            this.score = score;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.location = location;
        }
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
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
    }
}