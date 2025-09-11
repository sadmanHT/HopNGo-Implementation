package com.hopngo.ai.dto;

import java.time.Instant;

/**
 * Response DTO for embedding generation
 */
public class EmbeddingResponse {
    
    private float[] embedding;
    private int dimensions;
    private String model;
    private String inputType; // "text" or "image"
    private long processingTimeMs;
    private Instant timestamp;
    
    // Constructors
    public EmbeddingResponse() {
        this.timestamp = Instant.now();
    }
    
    public EmbeddingResponse(float[] embedding, String model, String inputType) {
        this.embedding = embedding;
        this.dimensions = embedding != null ? embedding.length : 0;
        this.model = model;
        this.inputType = inputType;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public float[] getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
        this.dimensions = embedding != null ? embedding.length : 0;
    }
    
    public int getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getInputType() {
        return inputType;
    }
    
    public void setInputType(String inputType) {
        this.inputType = inputType;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}