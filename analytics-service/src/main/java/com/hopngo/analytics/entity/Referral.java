package com.hopngo.analytics.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "referrals")
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referrer_user_id", nullable = false, length = 36)
    private String referrerUserId;

    @Column(name = "referred_user_id", length = 36)
    private String referredUserId;

    @Column(name = "referral_code", nullable = false, unique = true, length = 50)
    private String referralCode;

    @Column(name = "referral_url", columnDefinition = "TEXT")
    private String referralUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "campaign", length = 100)
    private String campaign;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer_page", columnDefinition = "TEXT")
    private String referrerPage;

    @Column(name = "landing_page", columnDefinition = "TEXT")
    private String landingPage;

    @Column(name = "conversion_event", length = 100)
    private String conversionEvent;

    @Column(name = "conversion_value_minor")
    private Long conversionValueMinor = 0L;

    @Column(name = "points_awarded")
    private Integer pointsAwarded = 0;

    @Column(name = "points_pending")
    private Integer pointsPending = 0;

    @Column(name = "click_count")
    private Integer clickCount = 0;

    @Column(name = "conversion_count")
    private Integer conversionCount = 0;

    @Column(name = "last_clicked_at")
    private OffsetDateTime lastClickedAt;

    @Column(name = "last_converted_at")
    private OffsetDateTime lastConvertedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "converted_at")
    private OffsetDateTime convertedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public Referral() {}

    public Referral(String referrerUserId, String referralCode) {
        this.referrerUserId = referrerUserId;
        this.referralCode = referralCode;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferrerUserId() {
        return referrerUserId;
    }

    public void setReferrerUserId(String referrerUserId) {
        this.referrerUserId = referrerUserId;
    }

    public String getReferredUserId() {
        return referredUserId;
    }

    public void setReferredUserId(String referredUserId) {
        this.referredUserId = referredUserId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public String getReferralUrl() {
        return referralUrl;
    }

    public void setReferralUrl(String referralUrl) {
        this.referralUrl = referralUrl;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
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

    public String getReferrerPage() {
        return referrerPage;
    }

    public void setReferrerPage(String referrerPage) {
        this.referrerPage = referrerPage;
    }

    public String getLandingPage() {
        return landingPage;
    }

    public void setLandingPage(String landingPage) {
        this.landingPage = landingPage;
    }

    public String getConversionEvent() {
        return conversionEvent;
    }

    public void setConversionEvent(String conversionEvent) {
        this.conversionEvent = conversionEvent;
    }

    public Long getConversionValueMinor() {
        return conversionValueMinor;
    }

    public void setConversionValueMinor(Long conversionValueMinor) {
        this.conversionValueMinor = conversionValueMinor;
    }

    public Integer getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public Integer getPointsPending() {
        return pointsPending;
    }

    public void setPointsPending(Integer pointsPending) {
        this.pointsPending = pointsPending;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Integer getConversionCount() {
        return conversionCount;
    }

    public void setConversionCount(Integer conversionCount) {
        this.conversionCount = conversionCount;
    }

    public OffsetDateTime getLastClickedAt() {
        return lastClickedAt;
    }

    public void setLastClickedAt(OffsetDateTime lastClickedAt) {
        this.lastClickedAt = lastClickedAt;
    }

    public OffsetDateTime getLastConvertedAt() {
        return lastConvertedAt;
    }

    public void setLastConvertedAt(OffsetDateTime lastConvertedAt) {
        this.lastConvertedAt = lastConvertedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(OffsetDateTime convertedAt) {
        this.convertedAt = convertedAt;
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

    // Alias methods for compatibility
    public String getUserId() {
        return referrerUserId;
    }

    public void setUserId(String userId) {
        this.referrerUserId = userId;
    }

    // Utility methods for LocalDateTime conversion
    public void setLastClickedAt(java.time.LocalDateTime lastClickedAt) {
        this.lastClickedAt = lastClickedAt != null ? lastClickedAt.atOffset(java.time.ZoneOffset.UTC) : null;
    }

    public void setLastConvertedAt(java.time.LocalDateTime lastConvertedAt) {
        this.lastConvertedAt = lastConvertedAt != null ? lastConvertedAt.atOffset(java.time.ZoneOffset.UTC) : null;
    }

    // Business methods
    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return status == ReferralStatus.COMPLETED;
    }

    public boolean isPending() {
        return status == ReferralStatus.PENDING;
    }

    public void markAsCompleted(String conversionEvent, Long conversionValue, Integer pointsAwarded) {
        this.status = ReferralStatus.COMPLETED;
        this.conversionEvent = conversionEvent;
        this.conversionValueMinor = conversionValue;
        this.pointsAwarded = pointsAwarded;
        this.pointsPending = 0;
        this.convertedAt = OffsetDateTime.now();
    }

    public void markAsExpired() {
        this.status = ReferralStatus.EXPIRED;
        this.pointsPending = 0;
    }

    // Enum for referral status
    public enum ReferralStatus {
        PENDING,
        ACTIVE,
        COMPLETED,
        EXPIRED
    }

    @Override
    public String toString() {
        return "Referral{" +
                "id=" + id +
                ", referrerUserId='" + referrerUserId + '\'' +
                ", referredUserId='" + referredUserId + '\'' +
                ", referralCode='" + referralCode + '\'' +
                ", status=" + status +
                ", pointsAwarded=" + pointsAwarded +
                ", createdAt=" + createdAt +
                '}';
    }
}