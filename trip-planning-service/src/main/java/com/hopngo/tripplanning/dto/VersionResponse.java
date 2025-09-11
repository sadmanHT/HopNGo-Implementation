package com.hopngo.tripplanning.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for itinerary version response
 */
public class VersionResponse {

    private UUID id;
    private UUID itineraryId;
    private Integer version;
    private JsonNode plan;
    private String authorUserId;
    private LocalDateTime createdAt;

    // Constructors
    public VersionResponse() {}

    public VersionResponse(UUID id, UUID itineraryId, Integer version, JsonNode plan, 
                         String authorUserId, LocalDateTime createdAt) {
        this.id = id;
        this.itineraryId = itineraryId;
        this.version = version;
        this.plan = plan;
        this.authorUserId = authorUserId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(UUID itineraryId) {
        this.itineraryId = itineraryId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public JsonNode getPlan() {
        return plan;
    }

    public void setPlan(JsonNode plan) {
        this.plan = plan;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "VersionResponse{" +
                "id=" + id +
                ", itineraryId=" + itineraryId +
                ", version=" + version +
                ", authorUserId='" + authorUserId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}