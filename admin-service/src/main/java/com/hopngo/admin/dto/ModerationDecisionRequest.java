package com.hopngo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ModerationDecisionRequest {
    
    @NotBlank(message = "Decision note is required")
    @Size(max = 1000, message = "Decision note must not exceed 1000 characters")
    private String decisionNote;
    
    public ModerationDecisionRequest() {}
    
    public ModerationDecisionRequest(String decisionNote) {
        this.decisionNote = decisionNote;
    }
    
    public String getDecisionNote() {
        return decisionNote;
    }
    
    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}