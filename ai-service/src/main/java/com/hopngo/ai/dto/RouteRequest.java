package com.hopngo.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request for route calculation")
public class RouteRequest {
    
    @NotBlank(message = "From coordinate is required")
    @Pattern(regexp = "^-?\\d+\\.\\d+,-?\\d+\\.\\d+$", message = "From must be in format 'lat,lng'")
    @Schema(description = "Starting point coordinates in format 'lat,lng'", example = "23.8103,90.4125")
    private String from;
    
    @NotBlank(message = "To coordinate is required")
    @Pattern(regexp = "^-?\\d+\\.\\d+,-?\\d+\\.\\d+$", message = "To must be in format 'lat,lng'")
    @Schema(description = "Destination coordinates in format 'lat,lng'", example = "23.7275,90.4077")
    private String to;
    
    @Pattern(regexp = "^(driving|walking|cycling)$", message = "Mode must be driving, walking, or cycling")
    @Schema(description = "Transportation mode", example = "driving", allowableValues = {"driving", "walking", "cycling"})
    private String mode = "driving";
    
    @Schema(description = "Whether to include alternative routes", example = "false")
    private boolean alternatives = false;
    
    @Schema(description = "Whether to include step-by-step instructions", example = "true")
    private boolean steps = true;
    
    @Schema(description = "Geometry format for the route", example = "geojson", allowableValues = {"geojson", "polyline", "polyline6"})
    private String geometries = "geojson";
    
    // Constructors
    public RouteRequest() {}
    
    public RouteRequest(String from, String to, String mode) {
        this.from = from;
        this.to = to;
        this.mode = mode;
    }
    
    // Getters and Setters
    public String getFrom() {
        return from;
    }
    
    public void setFrom(String from) {
        this.from = from;
    }
    
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public boolean isAlternatives() {
        return alternatives;
    }
    
    public void setAlternatives(boolean alternatives) {
        this.alternatives = alternatives;
    }
    
    public boolean isSteps() {
        return steps;
    }
    
    public void setSteps(boolean steps) {
        this.steps = steps;
    }
    
    public String getGeometries() {
        return geometries;
    }
    
    public void setGeometries(String geometries) {
        this.geometries = geometries;
    }
}