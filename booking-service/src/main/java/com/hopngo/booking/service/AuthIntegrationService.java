package com.hopngo.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthIntegrationService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    public AuthIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Check if user is a verified provider
     */
    public boolean isVerifiedProvider(String userId) {
        try {
            logger.debug("Checking verification status for user: {}", userId);
            
            String url = authServiceUrl + "/api/v1/auth/internal/users/" + userId + "/verification-status";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "booking-service");
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean verifiedProvider = (Boolean) body.get("verifiedProvider");
                
                logger.debug("User {} verification status: {}", userId, verifiedProvider);
                return Boolean.TRUE.equals(verifiedProvider);
            }
            
            logger.warn("Failed to get verification status for user {}: {}", userId, response.getStatusCode());
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking verification status for user {}: {}", userId, e.getMessage());
            // In case of service unavailability, we should fail safely
            // For now, we'll return false to prevent unverified access
            return false;
        }
    }
    
    /**
     * Get user details from auth service
     */
    public Map<String, Object> getUserDetails(String userId) {
        try {
            logger.debug("Getting user details for user: {}", userId);
            
            String url = authServiceUrl + "/api/v1/auth/internal/users/" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "booking-service");
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            
            logger.warn("Failed to get user details for user {}: {}", userId, response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            logger.error("Error getting user details for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}