package com.hopngo.tripplanning.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_similarities", 
       uniqueConstraints = @UniqueConstraint(name = "unique_user_pair", columnNames = {"user_id_1", "user_id_2"}))
public class UserSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "User ID 1 is required")
    @Column(name = "user_id_1", nullable = false)
    private String userId1;

    @NotBlank(message = "User ID 2 is required")
    @Column(name = "user_id_2", nullable = false)
    private String userId2;

    @NotNull(message = "Similarity score is required")
    @DecimalMin(value = "0.0", message = "Similarity score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Similarity score must be between 0.0 and 1.0")
    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;

    // Default constructor
    public UserSimilarity() {}

    // Constructor with required fields
    public UserSimilarity(String userId1, String userId2, BigDecimal similarityScore) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.similarityScore = similarityScore;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId1() {
        return userId1;
    }

    public void setUserId1(String userId1) {
        this.userId1 = userId1;
    }

    public String getUserId2() {
        return userId2;
    }

    public void setUserId2(String userId2) {
        this.userId2 = userId2;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(BigDecimal similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    @Override
    public String toString() {
        return "UserSimilarity{" +
                "id=" + id +
                ", userId1='" + userId1 + '\'' +
                ", userId2='" + userId2 + '\'' +
                ", similarityScore=" + similarityScore +
                ", calculatedAt=" + calculatedAt +
                '}';
    }
}