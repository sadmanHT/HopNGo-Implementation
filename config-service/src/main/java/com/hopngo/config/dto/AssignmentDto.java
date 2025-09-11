package com.hopngo.config.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class AssignmentDto {
    
    private Long id;
    
    @JsonProperty("experiment_key")
    private String experimentKey;
    
    @NotBlank(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("variant_name")
    private String variantName;
    
    @JsonProperty("assigned_at")
    private LocalDateTime assignedAt;
    
    // Default constructor
    public AssignmentDto() {}
    
    // Constructor with required fields
    public AssignmentDto(String experimentKey, String userId, String variantName) {
        this.experimentKey = experimentKey;
        this.userId = userId;
        this.variantName = variantName;
    }
    
    // Full constructor
    public AssignmentDto(Long id, String experimentKey, String userId, String variantName, LocalDateTime assignedAt) {
        this.id = id;
        this.experimentKey = experimentKey;
        this.userId = userId;
        this.variantName = variantName;
        this.assignedAt = assignedAt;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getExperimentKey() {
        return experimentKey;
    }
    
    public void setExperimentKey(String experimentKey) {
        this.experimentKey = experimentKey;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getVariantName() {
        return variantName;
    }
    
    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}