package com.hopngo.config.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public class FeatureFlagDto {
    
    private Long id;
    
    @NotBlank(message = "Feature flag key is required")
    @Size(min = 1, max = 100, message = "Key must be between 1 and 100 characters")
    private String key;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private boolean enabled;
    
    private Map<String, Object> payload;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public FeatureFlagDto() {}
    
    // Constructor with required fields
    public FeatureFlagDto(String key, boolean enabled) {
        this.key = key;
        this.enabled = enabled;
    }
    
    // Full constructor
    public FeatureFlagDto(Long id, String key, String description, boolean enabled, 
                         Map<String, Object> payload, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.key = key;
        this.description = description;
        this.enabled = enabled;
        this.payload = payload;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}