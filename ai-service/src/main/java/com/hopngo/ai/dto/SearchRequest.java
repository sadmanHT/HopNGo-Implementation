package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequest {
    
    @NotBlank(message = "Query is required")
    private String query;
    
    private int maxResults = 20;
    private String category; // optional filter
    
    // Constructors
    public SearchRequest() {}
    
    public SearchRequest(String query, int maxResults, String category) {
        this.query = query;
        this.maxResults = maxResults;
        this.category = category;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}