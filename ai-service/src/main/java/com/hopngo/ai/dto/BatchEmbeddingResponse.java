package com.hopngo.ai.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for batch embedding generation
 */
public class BatchEmbeddingResponse {
    
    private List<EmbeddingResponse> embeddings;
    private int totalCount;
    private String model;
    private long totalProcessingTimeMs;
    private Instant timestamp;
    
    // Constructors
    public BatchEmbeddingResponse() {
        this.timestamp = Instant.now();
    }
    
    public BatchEmbeddingResponse(List<EmbeddingResponse> embeddings, String model) {
        this.embeddings = embeddings;
        this.totalCount = embeddings != null ? embeddings.size() : 0;
        this.model = model;
        this.timestamp = Instant.now();
        
        // Calculate total processing time
        if (embeddings != null) {
            this.totalProcessingTimeMs = embeddings.stream()
                .mapToLong(EmbeddingResponse::getProcessingTimeMs)
                .sum();
        }
    }
    
    // Getters and setters
    public List<EmbeddingResponse> getEmbeddings() {
        return embeddings;
    }
    
    public void setEmbeddings(List<EmbeddingResponse> embeddings) {
        this.embeddings = embeddings;
        this.totalCount = embeddings != null ? embeddings.size() : 0;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs;
    }
    
    public void setTotalProcessingTimeMs(long totalProcessingTimeMs) {
        this.totalProcessingTimeMs = totalProcessingTimeMs;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}