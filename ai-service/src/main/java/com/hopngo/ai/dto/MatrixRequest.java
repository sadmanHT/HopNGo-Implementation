package com.hopngo.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request for distance/duration matrix calculation")
public class MatrixRequest {
    
    @NotEmpty(message = "Coordinates list cannot be empty")
    @Size(min = 2, max = 25, message = "Must have between 2 and 25 coordinates")
    @Schema(description = "List of coordinates in format 'lat,lng'", 
            example = "[\"23.8103,90.4125\", \"23.7275,90.4077\", \"23.7644,90.3897\"]")
    private List<String> coordinates;
    
    @Schema(description = "Transportation mode", example = "driving", 
            allowableValues = {"driving", "walking", "cycling"})
    private String mode = "driving";
    
    @Schema(description = "Sources indices (if not provided, all coordinates are used as sources)", 
            example = "[0, 1]")
    private List<Integer> sources;
    
    @Schema(description = "Destinations indices (if not provided, all coordinates are used as destinations)", 
            example = "[1, 2]")
    private List<Integer> destinations;
    
    @Schema(description = "Annotations to include in response", 
            example = "[\"duration\", \"distance\"]",
            allowableValues = {"duration", "distance", "datasources"})
    private List<String> annotations = List.of("duration", "distance");
    
    // Constructors
    public MatrixRequest() {}
    
    public MatrixRequest(List<String> coordinates, String mode) {
        this.coordinates = coordinates;
        this.mode = mode;
    }
    
    // Getters and Setters
    public List<String> getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(List<String> coordinates) {
        this.coordinates = coordinates;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public List<Integer> getSources() {
        return sources;
    }
    
    public void setSources(List<Integer> sources) {
        this.sources = sources;
    }
    
    public List<Integer> getDestinations() {
        return destinations;
    }
    
    public void setDestinations(List<Integer> destinations) {
        this.destinations = destinations;
    }
    
    public List<String> getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
}