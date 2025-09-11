package com.hopngo.booking.entity;

import com.hopngo.booking.entity.base.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
@Table(name = "review_flags")
public class ReviewFlag extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
    
    @Column(name = "reporter_user_id", nullable = false)
    private String reporterUserId;
    
    @NotBlank(message = "Reason cannot be blank")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Column(nullable = false)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewFlagStatus status = ReviewFlagStatus.OPEN;
    
    @Size(max = 1000, message = "Decision note cannot exceed 1000 characters")
    @Column(name = "decision_note")
    private String decisionNote;
    
    @Column(name = "resolved_by_user_id")
    private String resolvedByUserId;
    
    // Constructors
    public ReviewFlag() {}
    
    public ReviewFlag(Review review, String reporterUserId, String reason) {
        this.review = review;
        this.reporterUserId = reporterUserId;
        this.reason = reason;
    }
    
    // Business methods
    public boolean canBeResolved() {
        return status == ReviewFlagStatus.OPEN;
    }
    
    public void resolve(String resolvedByUserId, String decisionNote, ReviewFlagStatus newStatus) {
        if (!canBeResolved()) {
            throw new IllegalStateException("Flag cannot be resolved in current status: " + status);
        }
        if (newStatus == ReviewFlagStatus.OPEN) {
            throw new IllegalArgumentException("Cannot resolve flag to OPEN status");
        }
        
        this.status = newStatus;
        this.resolvedByUserId = resolvedByUserId;
        this.decisionNote = decisionNote;
    }
    
    public boolean isReportedBy(String userId) {
        return this.reporterUserId.equals(userId);
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Review getReview() {
        return review;
    }
    
    public void setReview(Review review) {
        this.review = review;
    }
    
    public String getReporterUserId() {
        return reporterUserId;
    }
    
    public void setReporterUserId(String reporterUserId) {
        this.reporterUserId = reporterUserId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public ReviewFlagStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReviewFlagStatus status) {
        this.status = status;
    }
    
    public String getDecisionNote() {
        return decisionNote;
    }
    
    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
    
    public String getResolvedByUserId() {
        return resolvedByUserId;
    }
    
    public void setResolvedByUserId(String resolvedByUserId) {
        this.resolvedByUserId = resolvedByUserId;
    }
}