package com.hopngo.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TicketPriorityUpdateRequest {
    
    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "Priority must be one of: LOW, MEDIUM, HIGH, URGENT")
    private String priority;
    
    private String reason;
    
    // Constructors
    public TicketPriorityUpdateRequest() {}
    
    public TicketPriorityUpdateRequest(String priority) {
        this.priority = priority;
    }
    
    public TicketPriorityUpdateRequest(String priority, String reason) {
        this.priority = priority;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
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
    
    public boolean isUrgent() {
        return "URGENT".equals(priority);
    }
    
    public boolean isHigh() {
        return "HIGH".equals(priority);
    }
    
    public boolean isEscalating() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }
    
    public int getPriorityLevel() {
        switch (priority) {
            case "LOW": return 1;
            case "MEDIUM": return 2;
            case "HIGH": return 3;
            case "URGENT": return 4;
            default: return 0;
        }
    }
    
    @Override
    public String toString() {
        return "TicketPriorityUpdateRequest{" +
                "priority='" + priority + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}