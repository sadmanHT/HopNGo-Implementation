package com.hopngo.tripplanning.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to update an existing itinerary")
public class ItineraryUpdateRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Schema(description = "Updated title for the trip", example = "European Adventure - Updated")
    private String title;

    @Min(value = 1, message = "Days must be at least 1")
    @Max(value = 30, message = "Days must not exceed 30")
    @Schema(description = "Updated number of days for the trip", example = "10")
    private Integer days;

    @Min(value = 1, message = "Budget must be greater than 0")
    @Schema(description = "Updated budget for the trip in cents", example = "200000")
    private Integer budget;

    @Size(max = 10, message = "Maximum 10 destinations allowed")
    @Schema(description = "Updated list of destinations to visit")
    private List<Map<String, Object>> destinations;

    @Schema(description = "Updated itinerary plan")
    private Map<String, Object> plan;

    // Default constructor
    public ItineraryUpdateRequest() {}

    // Constructor
    public ItineraryUpdateRequest(String title, Integer days, Integer budget,
                                 List<Map<String, Object>> destinations, Map<String, Object> plan) {
        this.title = title;
        this.days = days;
        this.budget = budget;
        this.destinations = destinations;
        this.plan = plan;
    }

    // Getters and Setters
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

    public List<Map<String, Object>> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Map<String, Object>> destinations) {
        this.destinations = destinations;
    }

    public Map<String, Object> getPlan() {
        return plan;
    }

    public void setPlan(Map<String, Object> plan) {
        this.plan = plan;
    }

    @Override
    public String toString() {
        return "ItineraryUpdateRequest{" +
                "title='" + title + '\'' +
                ", days=" + days +
                ", budget=" + budget +
                ", destinations=" + destinations +
                ", plan=" + plan +
                '}';
    }
}