package com.hopngo.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a user session with device tracking and security features
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "device_info")
    private String deviceInfo;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;
    
    @Column(name = "active", nullable = false)
    private boolean active;
    
    @Column(name = "two_factor_verified")
    private boolean twoFactorVerified;
    
    @Column(name = "two_factor_verified_at")
    private LocalDateTime twoFactorVerifiedAt;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    // Default constructor
    public UserSession() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isTwoFactorVerified() {
        return twoFactorVerified;
    }
    
    public void setTwoFactorVerified(boolean twoFactorVerified) {
        this.twoFactorVerified = twoFactorVerified;
    }
    
    public LocalDateTime getTwoFactorVerifiedAt() {
        return twoFactorVerifiedAt;
    }
    
    public void setTwoFactorVerifiedAt(LocalDateTime twoFactorVerifiedAt) {
        this.twoFactorVerifiedAt = twoFactorVerifiedAt;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}