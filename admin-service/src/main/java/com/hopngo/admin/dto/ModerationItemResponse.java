package com.hopngo.admin.dto;

import com.hopngo.admin.entity.ModerationItem;
import com.hopngo.admin.entity.ModerationItem.ModerationStatus;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;

import java.time.LocalDateTime;

public class ModerationItemResponse {
    
    private Long id;
    private ModerationItemType type;
    private String refId;
    private ModerationStatus status;
    private String reason;
    private String reporterUserId;
    private String assigneeUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String decisionNote;
    
    public ModerationItemResponse() {}
    
    public ModerationItemResponse(ModerationItem item) {
        this.id = item.getId();
        this.type = item.getType();
        this.refId = item.getRefId().toString();
        this.status = item.getStatus();
        this.reason = item.getReason();
        this.reporterUserId = item.getReporterUserId().toString();
        this.assigneeUserId = item.getAssigneeUserId() != null ? item.getAssigneeUserId().toString() : null;
        this.createdAt = item.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        this.updatedAt = item.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        this.decisionNote = item.getDecisionNote();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ModerationItemType getType() {
        return type;
    }
    
    public void setType(ModerationItemType type) {
        this.type = type;
    }
    
    public String getRefId() {
        return refId;
    }
    
    public void setRefId(String refId) {
        this.refId = refId;
    }
    
    public ModerationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ModerationStatus status) {
        this.status = status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getReporterUserId() {
        return reporterUserId;
    }
    
    public void setReporterUserId(String reporterUserId) {
        this.reporterUserId = reporterUserId;
    }
    
    public String getAssigneeUserId() {
        return assigneeUserId;
    }
    
    public void setAssigneeUserId(String assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
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
    
    public String getDecisionNote() {
        return decisionNote;
    }
    
    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}