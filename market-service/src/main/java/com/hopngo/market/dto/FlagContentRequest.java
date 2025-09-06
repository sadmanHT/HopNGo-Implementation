package com.hopngo.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FlagContentRequest {
    
    @NotBlank(message = "Reason is required")
    @Size(min = 3, max = 100, message = "Reason must be between 3 and 100 characters")
    private String reason;
    
    @Size(max = 500, message = "Details cannot exceed 500 characters")
    private String details;
    
    // Constructors
    public FlagContentRequest() {}
    
    public FlagContentRequest(String reason, String details) {
        this.reason = reason;
        this.details = details;
    }
    
    // Getters and Setters
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}