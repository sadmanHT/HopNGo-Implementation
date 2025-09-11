package com.hopngo.support.dto;

import com.hopngo.support.entity.CannedReply;

import java.time.LocalDateTime;

public class CannedReplyResponse {

    private Long id;
    private String title;
    private String body;
    private String category;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long usageCount;

    // Constructors
    public CannedReplyResponse() {}

    public CannedReplyResponse(Long id, String title, String body, String category, 
                              String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.category = category;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Factory method from entity
    public static CannedReplyResponse from(CannedReply cannedReply) {
        if (cannedReply == null) {
            return null;
        }
        
        CannedReplyResponse response = new CannedReplyResponse();
        response.setId(cannedReply.getId());
        response.setTitle(cannedReply.getTitle());
        response.setBody(cannedReply.getBody());
        response.setCategory(cannedReply.getCategory());
        response.setCreatedBy(cannedReply.getCreatedBy());
        response.setCreatedAt(cannedReply.getCreatedAt());
        response.setUpdatedAt(cannedReply.getUpdatedAt());
        response.setUsageCount(0L); // Default usage count, can be set from service layer
        
        return response;
    }

    // Factory method without body (for list views)
    public static CannedReplyResponse fromWithoutBody(CannedReply cannedReply) {
        if (cannedReply == null) {
            return null;
        }
        
        CannedReplyResponse response = from(cannedReply);
        response.setBody(null); // Remove body for list views
        
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    // Helper methods
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }

    public String getCategoryOrDefault() {
        return hasCategory() ? category : "General";
    }

    public boolean isSystemCreated() {
        return "SYSTEM".equals(createdBy);
    }

    public String getBodyPreview() {
        if (body == null) {
            return null;
        }
        return body.length() > 100 ? body.substring(0, 100) + "..." : body;
    }

    @Override
    public String toString() {
        return "CannedReplyResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", usageCount=" + usageCount +
                '}';
    }
}