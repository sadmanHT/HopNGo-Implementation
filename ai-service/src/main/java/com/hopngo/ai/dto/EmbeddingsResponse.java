package com.hopngo.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for text embeddings generation
 */
public class EmbeddingsResponse {
    
    private List<Double> embedding;
    private Map<String, Object> metadata;
    private String processingTime;
    
    // Constructors
    public EmbeddingsResponse() {}
    
    public EmbeddingsResponse(List<Double> embedding, Map<String, Object> metadata, String processingTime) {
        this.embedding = embedding;
        this.metadata = metadata;
        this.processingTime = processingTime;
    }
    
    // Getters and setters
    public List<Double> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }
}