package com.hopngo.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WebPushSubscriptionRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Endpoint is required")
    private String endpoint;
    
    @NotBlank(message = "P256DH key is required")
    private String p256dh;
    
    @NotBlank(message = "Auth key is required")
    private String auth;
    
    private String userAgent;
    
    // Constructors
    public WebPushSubscriptionRequest() {}
    
    public WebPushSubscriptionRequest(String userId, String endpoint, String p256dh, String auth) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getP256dh() {
        return p256dh;
    }
    
    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }
    
    public String getAuth() {
        return auth;
    }
    
    public void setAuth(String auth) {
        this.auth = auth;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}