package com.hopngo.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for generating text embeddings
 */
public class EmbeddingsRequest {
    
    @NotBlank(message = "Text content is required")
    @Size(max = 8000, message = "Text content must not exceed 8000 characters")
    private String text;
    
    private String model; // Optional model specification
    
    // Constructors
    public EmbeddingsRequest() {}
    
    public EmbeddingsRequest(String text) {
        this.text = text;
    }
    
    public EmbeddingsRequest(String text, String model) {
        this.text = text;
        this.model = model;
    }
    
    // Getters and setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
}