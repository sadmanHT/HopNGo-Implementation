package com.hopngo.tripplanning.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for comment response
 */
public class CommentResponse {

    private UUID id;
    private UUID itineraryId;
    private String authorUserId;
    private String message;
    private LocalDateTime createdAt;

    // Constructors
    public CommentResponse() {}

    public CommentResponse(UUID id, UUID itineraryId, String authorUserId, String message, LocalDateTime createdAt) {
        this.id = id;
        this.itineraryId = itineraryId;
        this.authorUserId = authorUserId;
        this.message = message;
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

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CommentResponse{" +
                "id=" + id +
                ", itineraryId=" + itineraryId +
                ", authorUserId='" + authorUserId + '\'' +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}