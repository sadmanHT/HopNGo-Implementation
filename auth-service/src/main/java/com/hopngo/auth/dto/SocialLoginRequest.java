package com.hopngo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class SocialLoginRequest {
    
    @NotBlank(message = "Provider is required")
    private String provider; // "google", "facebook", etc.
    
    @NotBlank(message = "Access token is required")
    private String accessToken;
    
    private String idToken; // Optional, for additional verification
    
    public SocialLoginRequest() {}
    
    public SocialLoginRequest(String provider, String accessToken) {
        this.provider = provider;
        this.accessToken = accessToken;
    }
    
    public SocialLoginRequest(String provider, String accessToken, String idToken) {
        this.provider = provider;
        this.accessToken = accessToken;
        this.idToken = idToken;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    
    @Override
    public String toString() {
        return "SocialLoginRequest{" +
                "provider='" + provider + '\'' +
                ", accessToken='[PROTECTED]'" +
                ", idToken='[PROTECTED]'" +
                '}';
    }
}