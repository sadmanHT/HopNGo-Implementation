package com.hopngo.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class BulkEmbeddingRequest {
    
    private String contentType = "all"; // "stay", "tour", "all"
    
    private List<String> contentIds; // Specific content IDs to process
    
    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 1000, message = "Batch size cannot exceed 1000")
    private int batchSize = 100;
    
    private boolean forceReindex = false; // Force reindex even if embeddings exist
    
    private String model = "default"; // Embedding model to use
    
    private Map<String, Object> filters; // Additional filters for content selection
    
    private boolean async = true; // Process asynchronously
    
    private String callbackUrl; // URL to notify when processing is complete
    
    // Constructors
    public BulkEmbeddingRequest() {}
    
    public BulkEmbeddingRequest(String contentType, int batchSize) {
        this.contentType = contentType;
        this.batchSize = batchSize;
    }
    
    public BulkEmbeddingRequest(List<String> contentIds, int batchSize) {
        this.contentIds = contentIds;
        this.batchSize = batchSize;
    }
    
    // Getters and Setters
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public List<String> getContentIds() {
        return contentIds;
    }
    
    public void setContentIds(List<String> contentIds) {
        this.contentIds = contentIds;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public boolean isForceReindex() {
        return forceReindex;
    }
    
    public void setForceReindex(boolean forceReindex) {
        this.forceReindex = forceReindex;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Map<String, Object> getFilters() {
        return filters;
    }
    
    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
    
    public boolean isAsync() {
        return async;
    }
    
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    public String getCallbackUrl() {
        return callbackUrl;
    }
    
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
    
    @Override
    public String toString() {
        return "BulkEmbeddingRequest{" +
                "contentType='" + contentType + '\'' +
                ", contentIds=" + (contentIds != null ? contentIds.size() + " items" : "null") +
                ", batchSize=" + batchSize +
                ", forceReindex=" + forceReindex +
                ", model='" + model + '\'' +
                ", async=" + async +
                '}';
    }
}