package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Event tracking response")
public class EventResponse {

    @Schema(description = "Event ID that was processed", example = "evt_123e4567-e89b-12d3-a456-426614174000")
    private String eventId;

    @Schema(description = "Processing status", example = "SUCCESS", allowableValues = {"SUCCESS", "DUPLICATE", "FAILED", "FILTERED"})
    private String status;

    @Schema(description = "Status message", example = "Event processed successfully")
    private String message;

    @Schema(description = "Server timestamp when event was processed", example = "2024-01-15T10:30:00.123Z")
    private OffsetDateTime processedAt;

    @Schema(description = "Validation errors if any")
    private List<String> errors;

    // Constructors
    public EventResponse() {}

    public EventResponse(String eventId, String status, String message) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.processedAt = OffsetDateTime.now();
    }

    public EventResponse(String eventId, String status, String message, List<String> errors) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.processedAt = OffsetDateTime.now();
    }

    // Static factory methods
    public static EventResponse success(String eventId) {
        return new EventResponse(eventId, "SUCCESS", "Event processed successfully");
    }

    public static EventResponse duplicate(String eventId) {
        return new EventResponse(eventId, "DUPLICATE", "Event already exists");
    }

    public static EventResponse failed(String eventId, String message) {
        return new EventResponse(eventId, "FAILED", message);
    }

    public static EventResponse failed(String eventId, String message, List<String> errors) {
        return new EventResponse(eventId, "FAILED", message, errors);
    }

    public static EventResponse filtered(String eventId, String reason) {
        return new EventResponse(eventId, "FILTERED", "Event filtered: " + reason);
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "EventResponse{" +
                "eventId='" + eventId + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}