package com.hopngo.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing distance/duration matrix")
public class MatrixResponse {
    
    @Schema(description = "Duration matrix in seconds (sources x destinations)")
    private List<List<Double>> durations;
    
    @Schema(description = "Distance matrix in meters (sources x destinations)")
    private List<List<Double>> distances;
    
    @Schema(description = "Sources used in the matrix calculation")
    private List<RouteResponse.Waypoint> sources;
    
    @Schema(description = "Destinations used in the matrix calculation")
    private List<RouteResponse.Waypoint> destinations;
    
    @Schema(description = "Status code of the matrix request")
    private String code;
    
    // Constructors
    public MatrixResponse() {}
    
    public MatrixResponse(List<List<Double>> durations, List<List<Double>> distances, String code) {
        this.durations = durations;
        this.distances = distances;
        this.code = code;
    }
    
    // Getters and Setters
    public List<List<Double>> getDurations() {
        return durations;
    }
    
    public void setDurations(List<List<Double>> durations) {
        this.durations = durations;
    }
    
    public List<List<Double>> getDistances() {
        return distances;
    }
    
    public void setDistances(List<List<Double>> distances) {
        this.distances = distances;
    }
    
    public List<RouteResponse.Waypoint> getSources() {
        return sources;
    }
    
    public void setSources(List<RouteResponse.Waypoint> sources) {
        this.sources = sources;
    }
    
    public List<RouteResponse.Waypoint> getDestinations() {
        return destinations;
    }
    
    public void setDestinations(List<RouteResponse.Waypoint> destinations) {
        this.destinations = destinations;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    // Convenience methods for frontend
    @Schema(description = "Duration matrix in minutes")
    public List<List<Double>> getDurationsMin() {
        if (durations == null) return null;
        return durations.stream()
                .map(row -> row.stream()
                        .map(duration -> duration != null ? duration / 60.0 : null)
                        .toList())
                .toList();
    }
    
    @Schema(description = "Distance matrix in kilometers")
    public List<List<Double>> getDistancesKm() {
        if (distances == null) return null;
        return distances.stream()
                .map(row -> row.stream()
                        .map(distance -> distance != null ? distance / 1000.0 : null)
                        .toList())
                .toList();
    }
}