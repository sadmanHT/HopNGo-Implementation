package com.hopngo.tripplanning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Request to create a new saved route")
public class CreateSavedRouteRequest {

    @Schema(description = "Name of the saved route", example = "Daily Commute")
    @NotBlank(message = "Route name is required")
    private String name;

    @Schema(description = "List of waypoints with coordinates and metadata")
    @NotNull(message = "Waypoints are required")
    private List<Map<String, Object>> waypoints;

    @Schema(description = "Total route distance in kilometers", example = "15.5")
    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", message = "Distance must be non-negative")
    private BigDecimal distanceKm;

    @Schema(description = "Estimated travel duration in minutes", example = "45")
    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration must be non-negative")
    private Integer durationMin;

    @Schema(description = "Transportation mode", example = "driving", allowableValues = {"driving", "walking", "cycling"})
    @NotBlank(message = "Transportation mode is required")
    @Pattern(regexp = "^(driving|walking|cycling)$", message = "Mode must be driving, walking, or cycling")
    private String mode;

    // Default constructor
    public CreateSavedRouteRequest() {}

    // Constructor
    public CreateSavedRouteRequest(String name, List<Map<String, Object>> waypoints,
                                  BigDecimal distanceKm, Integer durationMin, String mode) {
        this.name = name;
        this.waypoints = waypoints;
        this.distanceKm = distanceKm;
        this.durationMin = durationMin;
        this.mode = mode;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CreateSavedRouteRequest{" +
                "name='" + name + '\'' +
                ", waypoints=" + waypoints +
                ", distanceKm=" + distanceKm +
                ", durationMin=" + durationMin +
                ", mode='" + mode + '\'' +
                '}';
    }
}