package com.hopngo.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class KycDecisionRequest {
    
    @NotNull(message = "Decision is required")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Decision must be either APPROVE or REJECT")
    private String decision;
    
    private String rejectionReason;
    
    private String adminNotes;
    
    // Constructors
    public KycDecisionRequest() {}
    
    public KycDecisionRequest(String decision, String rejectionReason, String adminNotes) {
        this.decision = decision;
        this.rejectionReason = rejectionReason;
        this.adminNotes = adminNotes;
    }
    
    // Getters and Setters
    public String getDecision() {
        return decision;
    }
    
    public void setDecision(String decision) {
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
        return "APPROVE".equals(decision);
    }
    
    public boolean isRejection() {
        return "REJECT".equals(decision);
    }
    
    // Validation method
    public boolean isValid() {
        if (decision == null) {
            return false;
        }
        
        // If rejecting, rejection reason should be provided
        if (isRejection() && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "KycDecisionRequest{" +
                "decision='" + decision + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", adminNotes='" + adminNotes + '\'' +
                '}';
    }
}