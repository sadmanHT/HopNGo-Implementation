package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Batch event tracking request")
public class BatchEventRequest {

    @Schema(description = "List of events to track", example = "[{\"eventId\": \"evt_1\", \"eventType\": \"page_view\", \"eventCategory\": \"navigation\"}, {\"eventId\": \"evt_2\", \"eventType\": \"click\", \"eventCategory\": \"user_interaction\"}]")
    @NotEmpty(message = "Events list cannot be empty")
    @Size(max = 100, message = "Cannot process more than 100 events in a single batch")
    @Valid
    private List<EventRequest> events;

    @Schema(description = "Batch identifier for tracking", example = "batch_123e4567-e89b-12d3-a456-426614174000")
    private String batchId;

    // Constructors
    public BatchEventRequest() {}

    public BatchEventRequest(List<EventRequest> events) {
        this.events = events;
    }

    public BatchEventRequest(List<EventRequest> events, String batchId) {
        this.events = events;
        this.batchId = batchId;
    }

    // Getters and Setters
    public List<EventRequest> getEvents() {
        return events;
    }

    public void setEvents(List<EventRequest> events) {
        this.events = events;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public String toString() {
        return "BatchEventRequest{" +
                "events=" + (events != null ? events.size() : 0) + " events" +
                ", batchId='" + batchId + '\'' +
                '}';
    }
}