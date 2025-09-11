package com.hopngo.tripplanning.entity;

import com.hopngo.tripplanning.enums.ShareVisibility;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing itinerary sharing configuration
 */
@Entity
@Table(name = "itinerary_shares")
public class ItineraryShare {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private ShareVisibility visibility;

    @Column(name = "can_comment", nullable = false)
    private Boolean canComment = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ItineraryShare() {}

    public ItineraryShare(Itinerary itinerary, String token, ShareVisibility visibility, Boolean canComment) {
        this.itinerary = itinerary;
        this.token = token;
        this.visibility = visibility;
        this.canComment = canComment;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ShareVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ShareVisibility visibility) {
        this.visibility = visibility;
    }

    public Boolean getCanComment() {
        return canComment;
    }

    public void setCanComment(Boolean canComment) {
        this.canComment = canComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItineraryShare)) return false;
        ItineraryShare that = (ItineraryShare) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ItineraryShare{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", visibility=" + visibility +
                ", canComment=" + canComment +
                ", createdAt=" + createdAt +
                '}';
    }
}