package com.hopngo.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TicketStatusUpdateRequest {
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED", message = "Status must be one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED")
    private String status;
    
    private String reason;
    
    // Constructors
    public TicketStatusUpdateRequest() {}
    
    public TicketStatusUpdateRequest(String status) {
        this.status = status;
    }
    
    public TicketStatusUpdateRequest(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    // Helper methods
    public boolean hasReason() {
        return reason != null && !reason.trim().isEmpty();
    }
    
    public boolean isClosing() {
        return "CLOSED".equals(status);
    }
    
    public boolean isResolving() {
        return "RESOLVED".equals(status);
    }
    
    public boolean isReopening() {
        return "OPEN".equals(status) || "IN_PROGRESS".equals(status);
    }
    
    @Override
    public String toString() {
        return "TicketStatusUpdateRequest{" +
                "status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}