package com.hopngo.auth.dto;

public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserDto user;
    private boolean requires2FA = false; // Flag for 2FA requirement
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String accessToken, String refreshToken, UserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }
    
    public AuthResponse(String accessToken, String refreshToken, UserDto user, boolean requires2FA) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.requires2FA = requires2FA;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
    
    public boolean isRequires2FA() {
        return requires2FA;
    }
    
    public void setRequires2FA(boolean requires2FA) {
        this.requires2FA = requires2FA;
    }
    
    @Override
    public String toString() {
        return "AuthResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", user=" + user +
                '}';
    }
}