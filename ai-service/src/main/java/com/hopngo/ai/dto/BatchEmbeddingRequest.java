package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for batch embedding generation
 */
public class BatchEmbeddingRequest {
    
    @NotEmpty(message = "Texts list cannot be empty")
    @Size(max = 100, message = "Cannot process more than 100 texts in a single batch")
    private List<String> texts;
    
    private String model; // Optional model specification
    
    // Constructors
    public BatchEmbeddingRequest() {}
    
    public BatchEmbeddingRequest(List<String> texts) {
        this.texts = texts;
    }
    
    public BatchEmbeddingRequest(List<String> texts, String model) {
        this.texts = texts;
        this.model = model;
    }
    
    // Getters and setters
    public List<String> getTexts() {
        return texts;
    }
    
    public void setTexts(List<String> texts) {
        this.texts = texts;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
}