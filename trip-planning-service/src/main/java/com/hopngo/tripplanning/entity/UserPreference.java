package com.hopngo.tripplanning.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank(message = "Preference type is required")
    @Size(max = 100, message = "Preference type must not exceed 100 characters")
    @Column(name = "preference_type", nullable = false, length = 100)
    private String preferenceType;

    @NotBlank(message = "Preference value is required")
    @Size(max = 500, message = "Preference value must not exceed 500 characters")
    @Column(name = "preference_value", nullable = false, length = 500)
    private String preferenceValue;

    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    @Column(precision = 3, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor
    public UserPreference() {}

    // Constructor with required fields
    public UserPreference(String userId, String preferenceType, String preferenceValue) {
        this.userId = userId;
        this.preferenceType = preferenceType;
        this.preferenceValue = preferenceValue;
    }

    // Constructor with weight
    public UserPreference(String userId, String preferenceType, String preferenceValue, BigDecimal weight) {
        this.userId = userId;
        this.preferenceType = preferenceType;
        this.preferenceValue = preferenceValue;
        this.weight = weight;
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

    public String getPreferenceType() {
        return preferenceType;
    }

    public void setPreferenceType(String preferenceType) {
        this.preferenceType = preferenceType;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    public void setPreferenceValue(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
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
        return "UserPreference{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", preferenceType='" + preferenceType + '\'' +
                ", preferenceValue='" + preferenceValue + '\'' +
                ", weight=" + weight +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}