package com.hopngo.admin.dto;

import com.hopngo.admin.entity.AdminAudit;

import java.time.LocalDateTime;
import java.util.Map;

public class AdminAuditResponse {
    
    private Long id;
    private String actorUserId;
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> details;
    private LocalDateTime createdAt;
    
    public AdminAuditResponse() {}
    
    public AdminAuditResponse(AdminAudit audit) {
        this.id = audit.getId();
        this.actorUserId = audit.getActorUserId().toString();
        this.action = audit.getAction();
        this.targetType = audit.getTargetType();
        this.targetId = audit.getTargetId().toString();
        this.details = audit.getDetails();
        this.createdAt = audit.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getActorUserId() {
        return actorUserId;
    }
    
    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}