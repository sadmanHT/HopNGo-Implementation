package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Base class for all domain events consumed from other services
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BookingEvent.class, name = "booking"),
    @JsonSubTypes.Type(value = PaymentEvent.class, name = "payment"),
    @JsonSubTypes.Type(value = UserEvent.class, name = "user"),
    @JsonSubTypes.Type(value = ChatEvent.class, name = "chat")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DomainEvent {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("aggregateId")
    private String aggregateId;
    
    @JsonProperty("aggregateType")
    private String aggregateType;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;
    
    @JsonProperty("version")
    private Integer version;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Default constructor
    public DomainEvent() {
    }
    
    // Constructor
    public DomainEvent(String eventId, String eventType, String aggregateId, 
                      String aggregateType, String userId, String sessionId, 
                      OffsetDateTime timestamp, Integer version, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.userId = userId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.version = version;
        this.metadata = metadata;
    }
    
    // Abstract method to convert to analytics event
    public abstract EventRequest toAnalyticsEvent();
    
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
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
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
    
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "DomainEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                ", version=" + version +
                ", metadata=" + metadata +
                '}';
    }
}