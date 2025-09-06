package com.hopngo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hopngo.auth.entity.KycRequest;

import java.time.LocalDateTime;
import java.util.Map;

public class KycResponseDto {
    
    private Long id;
    
    @JsonProperty("user_id")
    private Long userId;
    
    private KycRequest.KycStatus status;
    
    private Map<String, Object> fields;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("verified_provider")
    private Boolean verifiedProvider;
    
    @JsonProperty("rejection_reason")
    private String rejectionReason;
    
    // Constructors
    public KycResponseDto() {}
    
    public KycResponseDto(Long id, Long userId, KycRequest.KycStatus status, 
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Static factory methods
    public static KycResponseDto fromKycRequest(KycRequest kycRequest) {
        KycResponseDto dto = new KycResponseDto();
        dto.setId(kycRequest.getId());
        dto.setUserId(kycRequest.getUserId());
        dto.setStatus(kycRequest.getStatus());
        dto.setFields(kycRequest.getFields());
        dto.setCreatedAt(kycRequest.getCreatedAt());
        dto.setUpdatedAt(kycRequest.getUpdatedAt());
        return dto;
    }
    
    public static KycResponseDto fromKycRequestWithFlags(KycRequest kycRequest, Boolean verifiedProvider) {
        KycResponseDto dto = fromKycRequest(kycRequest);
        dto.setVerifiedProvider(verifiedProvider);
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public KycRequest.KycStatus getStatus() {
        return status;
    }
    
    public void setStatus(KycRequest.KycStatus status) {
        this.status = status;
    }
    
    public Map<String, Object> getFields() {
        return fields;
    }
    
    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
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
    
    public Boolean getVerifiedProvider() {
        return verifiedProvider;
    }
    
    public void setVerifiedProvider(Boolean verifiedProvider) {
        this.verifiedProvider = verifiedProvider;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    // Helper methods
    public boolean isPending() {
        return KycRequest.KycStatus.PENDING.equals(this.status);
    }
    
    public boolean isApproved() {
        return KycRequest.KycStatus.APPROVED.equals(this.status);
    }
    
    public boolean isRejected() {
        return KycRequest.KycStatus.REJECTED.equals(this.status);
    }
}