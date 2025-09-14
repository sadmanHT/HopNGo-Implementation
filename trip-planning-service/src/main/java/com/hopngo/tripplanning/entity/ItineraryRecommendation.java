package com.hopngo.tripplanning.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "itinerary_recommendations")
public class ItineraryRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Recommended itinerary ID is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_itinerary_id", nullable = false)
    private Itinerary recommendedItinerary;

    @NotNull(message = "Recommendation score is required")
    @DecimalMin(value = "0.0", message = "Recommendation score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Recommendation score must be between 0.0 and 1.0")
    @Column(name = "recommendation_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal recommendationScore;

    @Size(max = 500, message = "Recommendation reason must not exceed 500 characters")
    @Column(name = "recommendation_reason", length = 500)
    private String recommendationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull(message = "Expires at is required")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            // Default expiration: 7 days from creation
            expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60);
        }
    }

    // Default constructor
    public ItineraryRecommendation() {}

    // Constructor with required fields
    public ItineraryRecommendation(String userId, Itinerary recommendedItinerary, 
                                 BigDecimal recommendationScore) {
        this.userId = userId;
        this.recommendedItinerary = recommendedItinerary;
        this.recommendationScore = recommendationScore;
    }

    // Constructor with reason
    public ItineraryRecommendation(String userId, Itinerary recommendedItinerary, 
                                 BigDecimal recommendationScore, String recommendationReason) {
        this.userId = userId;
        this.recommendedItinerary = recommendedItinerary;
        this.recommendationScore = recommendationScore;
        this.recommendationReason = recommendationReason;
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

    public Itinerary getRecommendedItinerary() {
        return recommendedItinerary;
    }

    public void setRecommendedItinerary(Itinerary recommendedItinerary) {
        this.recommendedItinerary = recommendedItinerary;
    }

    public BigDecimal getRecommendationScore() {
        return recommendationScore;
    }

    public void setRecommendationScore(BigDecimal recommendationScore) {
        this.recommendationScore = recommendationScore;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Check if this recommendation has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "ItineraryRecommendation{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", recommendedItineraryId=" + (recommendedItinerary != null ? recommendedItinerary.getId() : null) +
                ", recommendationScore=" + recommendationScore +
                ", recommendationReason='" + recommendationReason + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}