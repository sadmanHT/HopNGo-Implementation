package com.hopngo.tripplanning.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_routes")
public class SavedRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank(message = "Route name is required")
    @Size(max = 500, message = "Route name must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String name;

    @NotBlank(message = "Waypoints are required")
    @Column(columnDefinition = "jsonb", nullable = false)
    private String waypoints;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", message = "Distance must be non-negative")
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration must be non-negative")
    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @NotBlank(message = "Transportation mode is required")
    @Pattern(regexp = "^(driving|walking|cycling)$", message = "Mode must be driving, walking, or cycling")
    @Column(nullable = false, length = 50)
    private String mode;

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
    public SavedRoute() {}

    // Constructor with required fields
    public SavedRoute(String userId, String name, String waypoints, 
                     BigDecimal distanceKm, Integer durationMin, String mode) {
        this.userId = userId;
        this.name = name;
        this.waypoints = waypoints;
        this.distanceKm = distanceKm;
        this.durationMin = durationMin;
        this.mode = mode;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(String waypoints) {
        this.waypoints = waypoints;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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
        return "SavedRoute{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", distanceKm=" + distanceKm +
                ", durationMin=" + durationMin +
                ", mode='" + mode + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}