package com.hopngo.tripplanning.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to generate a trip itinerary")
public class TripPlanRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Schema(description = "Title for the trip", example = "European Adventure")
    private String title;

    @NotNull(message = "Origin is required")
    @Schema(description = "Starting location for the trip")
    private Map<String, Object> origin;

    @NotEmpty(message = "At least one destination is required")
    @Size(max = 10, message = "Maximum 10 destinations allowed")
    @Schema(description = "List of destinations to visit")
    private List<Map<String, Object>> destinations;

    @NotNull(message = "Days is required")
    @Min(value = 1, message = "Days must be at least 1")
    @Max(value = 30, message = "Days must not exceed 30")
    @Schema(description = "Number of days for the trip", example = "7")
    private Integer days;

    @NotNull(message = "Budget is required")
    @Min(value = 1, message = "Budget must be greater than 0")
    @Schema(description = "Budget for the trip in cents", example = "150000")
    private Integer budget;

    @Schema(description = "List of interests for personalized recommendations")
    private List<String> interests;

    // Default constructor
    public TripPlanRequest() {}

    // Constructor
    public TripPlanRequest(String title, Map<String, Object> origin, List<Map<String, Object>> destinations,
                          Integer days, Integer budget, List<String> interests) {
        this.title = title;
        this.origin = origin;
        this.destinations = destinations;
        this.days = days;
        this.budget = budget;
        this.interests = interests;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, Object> getOrigin() {
        return origin;
    }

    public void setOrigin(Map<String, Object> origin) {
        this.origin = origin;
    }

    public List<Map<String, Object>> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Map<String, Object>> destinations) {
        this.destinations = destinations;
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

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    @Override
    public String toString() {
        return "TripPlanRequest{" +
                "title='" + title + '\'' +
                ", origin=" + origin +
                ", destinations=" + destinations +
                ", days=" + days +
                ", budget=" + budget +
                ", interests=" + interests +
                '}';
    }
}