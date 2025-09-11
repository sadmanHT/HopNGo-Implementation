package com.hopngo.support.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "canned_replies")
@EntityListeners(AuditingEntityListener.class)
public class CannedReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Body is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public CannedReply() {}

    public CannedReply(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public CannedReply(String title, String body, String category) {
        this.title = title;
        this.body = body;
        this.category = category;
    }

    public CannedReply(String title, String body, String category, String createdBy) {
        this.title = title;
        this.body = body;
        this.category = category;
        this.createdBy = createdBy;
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

    // Helper methods
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }

    public String getCategoryOrDefault() {
        return hasCategory() ? category : "General";
    }

    public boolean isSystemCreated() {
        return "system".equalsIgnoreCase(createdBy);
    }

    @Override
    public String toString() {
        return "CannedReply{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}