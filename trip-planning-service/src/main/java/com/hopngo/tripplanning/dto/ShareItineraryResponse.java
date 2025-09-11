package com.hopngo.tripplanning.dto;

import com.hopngo.tripplanning.enums.ShareVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for sharing itinerary response
 */
public class ShareItineraryResponse {

    private UUID shareId;
    private String token;
    private String shareUrl;
    private ShareVisibility visibility;
    private Boolean canComment;
    private LocalDateTime createdAt;

    // Constructors
    public ShareItineraryResponse() {}

    public ShareItineraryResponse(UUID shareId, String token, String shareUrl, 
                                ShareVisibility visibility, Boolean canComment, LocalDateTime createdAt) {
        this.shareId = shareId;
        this.token = token;
        this.shareUrl = shareUrl;
        this.visibility = visibility;
        this.canComment = canComment;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getShareId() {
        return shareId;
    }

    public void setShareId(UUID shareId) {
        this.shareId = shareId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
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
    public String toString() {
        return "ShareItineraryResponse{" +
                "shareId=" + shareId +
                ", token='" + token + '\'' +
                ", shareUrl='" + shareUrl + '\'' +
                ", visibility=" + visibility +
                ", canComment=" + canComment +
                ", createdAt=" + createdAt +
                '}';
    }
}