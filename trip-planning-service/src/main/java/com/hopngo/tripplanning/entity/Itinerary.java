package com.hopngo.tripplanning.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String title;

    @NotNull(message = "Days is required")
    @Min(value = 1, message = "Days must be at least 1")
    @Max(value = 30, message = "Days must not exceed 30")
    @Column(nullable = false)
    private Integer days;

    @NotNull(message = "Budget is required")
    @Min(value = 1, message = "Budget must be greater than 0")
    @Column(nullable = false)
    private Integer budget;

    @Column(columnDefinition = "TEXT")
    private String origins;

    @Column(columnDefinition = "TEXT")
    private String destinations;

    @Column(columnDefinition = "TEXT")
    private String plan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Default constructor
    public Itinerary() {}

    // Constructor with required fields
    public Itinerary(String userId, String title, Integer days, Integer budget,
                     String origins, String destinations) {
        this.userId = userId;
        this.title = title;
        this.days = days;
        this.budget = budget;
        this.origins = origins;
        this.destinations = destinations;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public String getOrigins() {
        return origins;
    }

    public void setOrigins(String origins) {
        this.origins = origins;
    }

    public String getDestinations() {
        return destinations;
    }

    public void setDestinations(String destinations) {
        this.destinations = destinations;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
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

    @Override
    public String toString() {
        return "Itinerary{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", days=" + days +
                ", budget=" + budget +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}