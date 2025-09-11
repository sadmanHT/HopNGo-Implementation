package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Event tracking request")
public class EventRequest {

    @Schema(description = "Unique event identifier for deduplication", example = "evt_123e4567-e89b-12d3-a456-426614174000")
    @NotBlank(message = "Event ID is required")
    @Size(max = 36, message = "Event ID must not exceed 36 characters")
    private String eventId;

    @Schema(description = "Type of event", example = "page_view", allowableValues = {"page_view", "click", "form_submit", "purchase", "signup", "login", "logout", "search", "booking_start", "booking_complete", "payment_success", "payment_failed"})
    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type must not exceed 100 characters")
    private String eventType;

    @Schema(description = "Category of event", example = "user_interaction", allowableValues = {"user_interaction", "navigation", "conversion", "engagement", "error", "system"})
    @NotBlank(message = "Event category is required")
    @Size(max = 50, message = "Event category must not exceed 50 characters")
    private String eventCategory;

    @Schema(description = "User identifier (optional for anonymous events)", example = "user_123e4567-e89b-12d3-a456-426614174000")
    @Size(max = 36, message = "User ID must not exceed 36 characters")
    private String userId;

    @Schema(description = "Session identifier", example = "sess_123e4567-e89b-12d3-a456-426614174000")
    @Size(max = 36, message = "Session ID must not exceed 36 characters")
    private String sessionId;

    @Schema(description = "Page URL where event occurred", example = "https://hopngo.com/search?q=hotels")
    @Size(max = 2048, message = "Page URL must not exceed 2048 characters")
    private String pageUrl;

    @Schema(description = "Referrer URL", example = "https://google.com")
    @Size(max = 2048, message = "Referrer must not exceed 2048 characters")
    private String referrer;

    @Schema(description = "User agent string", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    @Size(max = 1000, message = "User agent must not exceed 1000 characters")
    private String userAgent;

    @Schema(description = "Custom event data as JSON object", example = "{\"product_id\": \"hotel_123\", \"price\": 150.00, \"currency\": \"USD\"}")
    private Map<String, Object> eventData;

    @Schema(description = "Additional metadata", example = "{\"client_version\": \"1.2.3\", \"platform\": \"web\"}")
    private Map<String, Object> metadata;

    @Schema(description = "Client timestamp in ISO 8601 format", example = "2024-01-15T10:30:00Z")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z?$", message = "Timestamp must be in ISO 8601 format")
    private String timestamp;

    // Constructors
    public EventRequest() {}

    public EventRequest(String eventId, String eventType, String eventCategory) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventCategory = eventCategory;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRequest that = (EventRequest) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "EventRequest{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventCategory='" + eventCategory + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", pageUrl='" + pageUrl + '\'' +
                '}';
    }
}