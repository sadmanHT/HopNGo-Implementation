package com.hopngo.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "admin_audit")
public class AdminAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @NotNull
    @Column(nullable = false, length = 100)
    private String action;

    @NotNull
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @NotNull
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public AdminAudit() {}

    public AdminAudit(Long actorUserId, String action, String targetType, Long targetId, Map<String, Object> details) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
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

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Alias methods for compatibility
    public void setUserId(String userId) {
        this.actorUserId = Long.valueOf(userId);
    }

    public void setResourceType(String resourceType) {
        this.targetType = resourceType;
    }

    public void setResourceId(String resourceId) {
        this.targetId = Long.valueOf(resourceId);
    }

    public void setTimestamp(java.time.LocalDateTime timestamp) {
        this.createdAt = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    public String getUserId() {
        return this.actorUserId != null ? this.actorUserId.toString() : null;
    }

    public void setDetails(String details) {
        // Convert string to Map if needed - for now just store as single entry
        this.details = details != null ? Map.of("content", details) : null;
    }

    public boolean contains(String key) {
        return this.details != null && this.details.containsKey(key);
    }
}