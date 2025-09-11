package com.hopngo.config.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hopngo.config.entity.ExperimentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class ExperimentDto {
    
    private Long id;
    
    @NotBlank(message = "Experiment key is required")
    @Size(min = 1, max = 100, message = "Key must be between 1 and 100 characters")
    private String key;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private ExperimentStatus status = ExperimentStatus.DRAFT;
    
    @Min(value = 0, message = "Traffic percentage must be at least 0")
    @Max(value = 100, message = "Traffic percentage must not exceed 100")
    @JsonProperty("traffic_pct")
    private Integer trafficPct = 100;
    
    @Valid
    @NotEmpty(message = "At least one variant is required")
    private List<ExperimentVariantDto> variants;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public ExperimentDto() {}
    
    // Constructor with required fields
    public ExperimentDto(String key, List<ExperimentVariantDto> variants) {
        this.key = key;
        this.variants = variants;
    }
    
    // Full constructor
    public ExperimentDto(Long id, String key, String description, ExperimentStatus status,
                        Integer trafficPct, List<ExperimentVariantDto> variants,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.key = key;
        this.description = description;
        this.status = status;
        this.trafficPct = trafficPct;
        this.variants = variants;
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
    
    public ExperimentStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }
    
    public Integer getTrafficPct() {
        return trafficPct;
    }
    
    public void setTrafficPct(Integer trafficPct) {
        this.trafficPct = trafficPct;
    }
    
    public List<ExperimentVariantDto> getVariants() {
        return variants;
    }
    
    public void setVariants(List<ExperimentVariantDto> variants) {
        this.variants = variants;
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