package com.hopngo.tripplanning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Itinerary response")
public class ItineraryResponse {

    @Schema(description = "Unique identifier of the itinerary")
    private UUID id;

    @Schema(description = "User ID who owns this itinerary")
    private String userId;

    @Schema(description = "Title of the itinerary", example = "European Adventure")
    private String title;

    @Schema(description = "Number of days for the trip", example = "7")
    private Integer days;

    @Schema(description = "Budget for the trip in cents", example = "150000")
    private Integer budget;

    @Schema(description = "List of origin locations")
    private List<Map<String, Object>> origins;

    @Schema(description = "List of destination locations")
    private List<Map<String, Object>> destinations;

    @Schema(description = "Generated itinerary plan from AI service")
    private Map<String, Object> plan;

    @Schema(description = "Timestamp when the itinerary was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when the itinerary was last updated")
    private Instant updatedAt;

    // Default constructor
    public ItineraryResponse() {}

    // Constructor
    public ItineraryResponse(UUID id, String userId, String title, Integer days, Integer budget,
                            List<Map<String, Object>> origins, List<Map<String, Object>> destinations,
                            Map<String, Object> plan, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.days = days;
        this.budget = budget;
        this.origins = origins;
        this.destinations = destinations;
        this.plan = plan;
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
        return "ItineraryResponse{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", days=" + days +
                ", budget=" + budget +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}