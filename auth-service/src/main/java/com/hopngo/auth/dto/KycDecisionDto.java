package com.hopngo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class KycDecisionDto {
    
    @NotNull(message = "Decision is required")
    private Decision decision;
    
    @JsonProperty("rejection_reason")
    private String rejectionReason;
    
    @JsonProperty("admin_notes")
    private String adminNotes;
    
    // Constructors
    public KycDecisionDto() {}
    
    public KycDecisionDto(Decision decision) {
        this.decision = decision;
    }
    
    public KycDecisionDto(Decision decision, String rejectionReason) {
        this.decision = decision;
        this.rejectionReason = rejectionReason;
    }
    
    // Getters and Setters
    public Decision getDecision() {
        return decision;
    }
    
    public void setDecision(Decision decision) {
        this.decision = decision;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    // Helper methods
    public boolean isApproval() {
        return Decision.APPROVE.equals(this.decision);
    }
    
    public boolean isRejection() {
        return Decision.REJECT.equals(this.decision);
    }
    
    // Validation method
    public boolean isValid() {
        if (decision == null) {
            return false;
        }
        
        // If rejecting, rejection reason should be provided
        if (Decision.REJECT.equals(decision) && 
            (rejectionReason == null || rejectionReason.trim().isEmpty())) {
            return false;
        }
        
        return true;
    }
    
    // Decision enum
    public enum Decision {
        APPROVE, REJECT
    }
}