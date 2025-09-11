package com.hopngo.tripplanning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Saved route response")
public class SavedRouteResponse {

    @Schema(description = "Unique identifier of the saved route")
    private UUID id;

    @Schema(description = "User ID who owns this saved route")
    private String userId;

    @Schema(description = "Name of the saved route", example = "Daily Commute")
    private String name;

    @Schema(description = "List of waypoints with coordinates and metadata")
    private List<Map<String, Object>> waypoints;

    @Schema(description = "Total route distance in kilometers", example = "15.5")
    private BigDecimal distanceKm;

    @Schema(description = "Estimated travel duration in minutes", example = "45")
    private Integer durationMin;

    @Schema(description = "Transportation mode", example = "driving")
    private String mode;

    @Schema(description = "Timestamp when the route was saved")
    private Instant createdAt;

    @Schema(description = "Timestamp when the route was last updated")
    private Instant updatedAt;

    // Default constructor
    public SavedRouteResponse() {}

    // Constructor
    public SavedRouteResponse(UUID id, String userId, String name, List<Map<String, Object>> waypoints,
                             BigDecimal distanceKm, Integer durationMin, String mode,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.waypoints = waypoints;
        this.distanceKm = distanceKm;
        this.durationMin = durationMin;
        this.mode = mode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public List<Map<String, Object>> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Map<String, Object>> waypoints) {
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
        return "SavedRouteResponse{" +
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