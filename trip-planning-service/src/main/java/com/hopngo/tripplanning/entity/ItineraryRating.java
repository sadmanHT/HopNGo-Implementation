package com.hopngo.tripplanning.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "itinerary_ratings")
public class ItineraryRating {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Itinerary ID is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Column(nullable = false)
    private Integer rating;

    @NotBlank(message = "Interaction type is required")
    @Size(max = 50, message = "Interaction type must not exceed 50 characters")
    @Column(name = "interaction_type", nullable = false, length = 50)
    private String interactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor
    public ItineraryRating() {}

    // Constructor with required fields
    public ItineraryRating(String userId, Itinerary itinerary, Integer rating, String interactionType) {
        this.userId = userId;
        this.itinerary = itinerary;
        this.rating = rating;
        this.interactionType = interactionType;
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

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ItineraryRating{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", itineraryId=" + (itinerary != null ? itinerary.getId() : null) +
                ", rating=" + rating +
                ", interactionType='" + interactionType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // Enum for interaction types
    public enum InteractionType {
        CREATED("created"),
        VIEWED("viewed"),
        SAVED("saved"),
        SHARED("shared"),
        RATED("rated");

        private final String value;

        InteractionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static InteractionType fromValue(String value) {
            for (InteractionType type : InteractionType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown interaction type: " + value);
        }
    }
}