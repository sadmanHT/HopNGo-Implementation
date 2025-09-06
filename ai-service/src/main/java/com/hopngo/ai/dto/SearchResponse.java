package com.hopngo.ai.dto;

import java.util.List;

public class SearchResponse {
    
    private List<RankedResult> results;
    private int totalResults;
    private String query;
    private String processingTime;
    
    // Constructors
    public SearchResponse() {}
    
    public SearchResponse(List<RankedResult> results, int totalResults, String query, String processingTime) {
        this.results = results;
        this.totalResults = totalResults;
        this.query = query;
        this.processingTime = processingTime;
    }
    
    // Getters and Setters
    public List<RankedResult> getResults() {
        return results;
    }
    
    public void setResults(List<RankedResult> results) {
        this.results = results;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }
    
    // Inner class for ranked results
    public static class RankedResult {
        private String id;
        private String title;
        private String description;
        private String category;
        private double relevanceScore;
        private String url;
        private String imageUrl;
        private String location;
        
        // Constructors
        public RankedResult() {}
        
        public RankedResult(String id, String title, String description, String category, 
                          double relevanceScore, String url, String imageUrl, String location) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.relevanceScore = relevanceScore;
            this.url = url;
            this.imageUrl = imageUrl;
            this.location = location;
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
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
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public double getRelevanceScore() {
            return relevanceScore;
        }
        
        public void setRelevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
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