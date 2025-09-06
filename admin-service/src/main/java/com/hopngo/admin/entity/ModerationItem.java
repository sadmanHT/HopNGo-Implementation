package com.hopngo.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "moderation_items")
public class ModerationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private ModerationItemType type;

    @NotNull
    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private ModerationStatus status = ModerationStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @NotNull
    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority;

    @Column(name = "content_details", columnDefinition = "TEXT")
    private String contentDetails;

    // Constructors
    public ModerationItem() {}

    public ModerationItem(ModerationItemType type, Long refId, String reason, Long reporterUserId) {
        this.type = type;
        this.refId = refId;
        this.reason = reason;
        this.reporterUserId = reporterUserId;
    }

    // Getters and Setters
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

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
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

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getContentDetails() {
        return contentDetails;
    }

    public void setContentDetails(String contentDetails) {
        this.contentDetails = contentDetails;
    }

    // Alias methods for compatibility
    public void setReferenceId(String referenceId) {
        this.refId = Long.valueOf(referenceId);
    }

    public void setReportedBy(String reportedBy) {
        this.reporterUserId = Long.valueOf(reportedBy);
    }

    public void setAssigneeUserIdFromString(String assigneeUserId) {
        this.assigneeUserId = assigneeUserId != null ? Long.valueOf(assigneeUserId) : null;
    }

    public void setReporterUserIdFromString(String reporterUserId) {
        this.reporterUserId = Long.valueOf(reporterUserId);
    }

    public enum ModerationItemType {
        POST, COMMENT, LISTING, REVIEW, USER, TRIP, MESSAGE, USER_PROFILE
    }

    public enum ModerationStatus {
        OPEN, APPROVED, REJECTED, REMOVED, PENDING
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}