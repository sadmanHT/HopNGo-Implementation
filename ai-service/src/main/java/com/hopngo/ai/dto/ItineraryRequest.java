package com.hopngo.ai.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class ItineraryRequest {
    
    @NotBlank(message = "Origin is required")
    private String origin;
    
    @NotEmpty(message = "At least one destination is required")
    private List<String> destinations;
    
    @Min(value = 1, message = "Days must be at least 1")
    @Max(value = 30, message = "Days cannot exceed 30")
    private int days;
    
    @Min(value = 1, message = "Budget must be positive")
    private int budget; // in cents
    
    private List<String> interests;
    
    // Constructors
    public ItineraryRequest() {}
    
    public ItineraryRequest(String origin, List<String> destinations, int days, int budget, List<String> interests) {
        this.origin = origin;
        this.destinations = destinations;
        this.days = days;
        this.budget = budget;
        this.interests = interests;
    }
    
    // Getters and Setters
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    
    public List<String> getDestinations() {
        return destinations;
    }
    
    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }
    
    public int getDays() {
        return days;
    }
    
    public void setDays(int days) {
        this.days = days;
    }
    
    public int getBudget() {
        return budget;
    }
    
    public void setBudget(int budget) {
        this.budget = budget;
    }
    
    public List<String> getInterests() {
        return interests;
    }
    
    public void setInterests(List<String> interests) {
        this.interests = interests;
    }
}