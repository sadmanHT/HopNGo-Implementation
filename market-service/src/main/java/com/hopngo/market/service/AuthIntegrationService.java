package com.hopngo.market.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class AuthIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthIntegrationService.class);
    
    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    
    public AuthIntegrationService(RestTemplate restTemplate, 
                                @Value("${auth.service.url:http://localhost:8081}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }
    
    /**
     * Check if a user is a verified provider
     */
    public boolean isVerifiedProvider(String userId) {
        try {
            String url = authServiceUrl + "/internal/users/" + userId + "/verification-status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> verificationStatus = response.getBody();
                Boolean verified = (Boolean) verificationStatus.get("verified");
                Boolean banned = (Boolean) verificationStatus.get("banned");
                Boolean suspended = (Boolean) verificationStatus.get("suspended");
                
                return Boolean.TRUE.equals(verified) && 
                       !Boolean.TRUE.equals(banned) && 
                       !Boolean.TRUE.equals(suspended);
            }
            
            logger.warn("Failed to get verification status for user {}: {}", userId, response.getStatusCode());
            return false;
            
        } catch (RestClientException e) {
            logger.error("Error checking verification status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user details from auth service
     */
    public Map<String, Object> getUserDetails(String userId) {
        try {
            String url = authServiceUrl + "/internal/users/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            
            logger.warn("Failed to get user details for user {}: {}", userId, response.getStatusCode());
            return null;
            
        } catch (RestClientException e) {
            logger.error("Error getting user details for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}