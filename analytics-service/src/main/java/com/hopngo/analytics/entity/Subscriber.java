package com.hopngo.analytics.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "subscribers")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriberStatus status = SubscriberStatus.ACTIVE;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "tags")
    private String[] tags;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "utm_source", length = 100)
    private String utmSource;

    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 100)
    private String utmCampaign;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "subscribed_at", nullable = false)
    private OffsetDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private OffsetDateTime unsubscribedAt;

    @Column(name = "unsubscribe_token", unique = true, length = 255)
    private String unsubscribeToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public Subscriber() {
        this.subscribedAt = OffsetDateTime.now();
    }

    public Subscriber(String email) {
        this();
        this.email = email;
    }

    public Subscriber(String email, String source) {
        this(email);
        this.source = source;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SubscriberStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriberStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getUtmSource() {
        return utmSource;
    }

    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    public String getUtmMedium() {
        return utmMedium;
    }

    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    public String getUtmCampaign() {
        return utmCampaign;
    }

    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(OffsetDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public OffsetDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }

    public void setUnsubscribedAt(OffsetDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUnsubscribeToken() {
        return unsubscribeToken;
    }

    public void setUnsubscribeToken(String unsubscribeToken) {
        this.unsubscribeToken = unsubscribeToken;
    }

    // Business methods
    public boolean isActive() {
        return status == SubscriberStatus.ACTIVE;
    }

    public boolean isUnsubscribed() {
        return status == SubscriberStatus.UNSUBSCRIBED;
    }

    public boolean isBounced() {
        return status == SubscriberStatus.BOUNCED;
    }

    public void unsubscribe() {
        this.status = SubscriberStatus.UNSUBSCRIBED;
        this.unsubscribedAt = OffsetDateTime.now();
    }

    public void resubscribe() {
        this.status = SubscriberStatus.ACTIVE;
        this.unsubscribedAt = null;
        this.subscribedAt = OffsetDateTime.now();
    }

    public void markAsBounced() {
        this.status = SubscriberStatus.BOUNCED;
    }

    public List<String> getTagsList() {
        return tags != null ? Arrays.asList(tags) : List.of();
    }

    public void setTagsList(List<String> tagsList) {
        this.tags = tagsList != null ? tagsList.toArray(new String[0]) : null;
    }

    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        List<String> currentTags = getTagsList();
        if (!currentTags.contains(tag)) {
            currentTags.add(tag);
            setTagsList(currentTags);
        }
    }

    public void removeTag(String tag) {
        if (tag == null || tags == null) {
            return;
        }
        
        List<String> currentTags = getTagsList();
        currentTags.remove(tag);
        setTagsList(currentTags.isEmpty() ? null : currentTags);
    }

    public boolean hasTag(String tag) {
        return tag != null && getTagsList().contains(tag);
    }

    // Static factory methods
    // Factory methods for different sources
    public static Subscriber createFromFooter(String email, Long userId, String ipAddress, String userAgent) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setUserId(userId != null ? userId.toString() : null);
        subscriber.setSource("footer");
        subscriber.setSubscribedAt(OffsetDateTime.now());
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setIpAddress(ipAddress);
        subscriber.setUserAgent(userAgent);
        subscriber.setUnsubscribeToken(generateUnsubscribeToken());
        return subscriber;
    }

    // Overloaded method for footer subscription with minimal parameters
    public static Subscriber createFromFooter(String email, Long userId) {
        return createFromFooter(email, userId, null, null);
    }

    public static Subscriber createFromPopup(String email, Long userId, String ipAddress) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setUserId(userId != null ? userId.toString() : null);
        subscriber.setSource("popup");
        subscriber.setSubscribedAt(OffsetDateTime.now());
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setIpAddress(ipAddress);
        subscriber.setUnsubscribeToken(generateUnsubscribeToken());
        return subscriber;
    }

    // Overloaded method for popup subscription with minimal parameters
    public static Subscriber createFromPopup(String email, Long userId) {
        return createFromPopup(email, userId, null);
    }

    // Utility method to generate unsubscribe token
    private static String generateUnsubscribeToken() {
        return java.util.UUID.randomUUID().toString();
    }

    public static Subscriber createFromReferral(String email, String referrerUserId, String ipAddress) {
        Subscriber subscriber = new Subscriber(email, "referral");
        subscriber.setIpAddress(ipAddress);
        subscriber.addTag("referral-signup");
        // Store referrer info in metadata or separate field if needed
        return subscriber;
    }

    // Enum for subscriber status
    public enum SubscriberStatus {
        ACTIVE,
        UNSUBSCRIBED,
        BOUNCED
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", source='" + source + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", subscribedAt=" + subscribedAt +
                '}';
    }
}