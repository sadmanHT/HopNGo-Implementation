package com.hopngo.tripplanning.dto;

import com.hopngo.tripplanning.enums.ShareVisibility;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for sharing itinerary request
 */
public class ShareItineraryRequest {

    @NotNull(message = "Visibility is required")
    private ShareVisibility visibility;

    private Boolean canComment = false;

    // Constructors
    public ShareItineraryRequest() {}

    public ShareItineraryRequest(ShareVisibility visibility, Boolean canComment) {
        this.visibility = visibility;
        this.canComment = canComment;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "ShareItineraryRequest{" +
                "visibility=" + visibility +
                ", canComment=" + canComment +
                '}';
    }
}