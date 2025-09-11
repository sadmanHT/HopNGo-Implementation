package com.hopngo.config.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Map;

public class ExperimentVariantDto {
    
    private Long id;
    
    @NotBlank(message = "Variant name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @Min(value = 0, message = "Weight percentage must be at least 0")
    @Max(value = 100, message = "Weight percentage must not exceed 100")
    @JsonProperty("weight_pct")
    private Integer weightPct;
    
    private Map<String, Object> payload;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public ExperimentVariantDto() {}
    
    // Constructor with required fields
    public ExperimentVariantDto(String name, Integer weightPct) {
        this.name = name;
        this.weightPct = weightPct;
    }
    
    // Full constructor
    public ExperimentVariantDto(Long id, String name, Integer weightPct, Map<String, Object> payload,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.weightPct = weightPct;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getWeightPct() {
        return weightPct;
    }
    
    public void setWeightPct(Integer weightPct) {
        this.weightPct = weightPct;
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