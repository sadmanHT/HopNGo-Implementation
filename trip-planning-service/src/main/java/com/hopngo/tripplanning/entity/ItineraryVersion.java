package com.hopngo.tripplanning.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing itinerary version history
 */
@Entity
@Table(name = "itinerary_versions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"itinerary_id", "version"}))
public class ItineraryVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(name = "version", nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plan", nullable = false, columnDefinition = "jsonb")
    private JsonNode plan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "author_user_id", nullable = false)
    private String authorUserId;

    // Constructors
    public ItineraryVersion() {}

    public ItineraryVersion(Itinerary itinerary, Integer version, JsonNode plan, String authorUserId) {
        this.itinerary = itinerary;
        this.version = version;
        this.plan = plan;
        this.authorUserId = authorUserId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItineraryVersion)) return false;
        ItineraryVersion that = (ItineraryVersion) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ItineraryVersion{" +
                "id=" + id +
                ", version=" + version +
                ", authorUserId='" + authorUserId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}