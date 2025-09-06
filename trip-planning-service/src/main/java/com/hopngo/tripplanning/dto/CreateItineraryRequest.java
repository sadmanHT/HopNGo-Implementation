package com.hopngo.tripplanning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to create a new itinerary")
public class CreateItineraryRequest {

    @Schema(description = "Title of the itinerary", example = "European Adventure")
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Number of days for the trip", example = "7")
    @NotNull(message = "Days is required")
    @Positive(message = "Days must be positive")
    private Integer days;

    @Schema(description = "Budget for the trip in cents", example = "150000")
    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private Integer budget;

    @Schema(description = "List of origin locations")
    private List<Map<String, Object>> origins;

    @Schema(description = "List of destination locations")
    private List<Map<String, Object>> destinations;

    @Schema(description = "Generated itinerary plan from AI service")
    private Map<String, Object> plan;

    // Default constructor
    public CreateItineraryRequest() {}

    // Constructor
    public CreateItineraryRequest(String title, Integer days, Integer budget,
                                 List<Map<String, Object>> origins,
                                 List<Map<String, Object>> destinations,
                                 Map<String, Object> plan) {
        this.title = title;
        this.days = days;
        this.budget = budget;
        this.origins = origins;
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

    public List<Map<String, Object>> getOrigins() {
        return origins;
    }

    public void setOrigins(List<Map<String, Object>> origins) {
        this.origins = origins;
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
        return "CreateItineraryRequest{" +
                "title='" + title + '\'' +
                ", days=" + days +
                ", budget=" + budget +
                ", origins=" + origins +
                ", destinations=" + destinations +
                ", plan=" + plan +
                '}';
    }
}