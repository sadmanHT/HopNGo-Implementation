package com.hopngo.admin.service;

import com.hopngo.admin.entity.ModerationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class IntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${integration.auth-service.url}")
    private String authServiceUrl;
    
    @Value("${integration.social-service.url}")
    private String socialServiceUrl;
    
    @Value("${integration.booking-service.url}")
    private String bookingServiceUrl;
    
    @Value("${integration.market-service.url}")
    private String marketServiceUrl;
    
    public IntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public boolean removeContent(ModerationItem.ModerationItemType type, Long refId) {
        try {
            String serviceUrl = getServiceUrlForType(type);
            String endpoint = getRemoveEndpointForType(type, refId.toString());
            String fullUrl = serviceUrl + endpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            // TODO: Add service-to-service authentication header
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                logger.info("Successfully removed content {} of type {}", refId, type);
            } else {
                logger.warn("Failed to remove content {} of type {}: {}", refId, type, response.getStatusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error removing content {} of type {}", refId, type, e);
            return false;
        }
    }
    
    public void removeSocialPost(Long postId, String reason) {
        try {
            String url = socialServiceUrl + "/internal/posts/" + postId.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            requestBody.put("removedAt", System.currentTimeMillis());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully removed social post {}", postId);
            } else {
                logger.warn("Failed to remove social post {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Failed to remove social post");
            }
            
        } catch (Exception e) {
            logger.error("Error removing social post {}", postId, e);
            throw new RuntimeException("Social post removal failed", e);
        }
    }
    
    public void removeSocialComment(Long commentId, String reason) {
        try {
            String url = socialServiceUrl + "/internal/comments/" + commentId.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            requestBody.put("removedAt", System.currentTimeMillis());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully removed social comment {}", commentId);
            } else {
                logger.warn("Failed to remove social comment {}: {}", commentId, response.getStatusCode());
                throw new RuntimeException("Failed to remove social comment");
            }
            
        } catch (Exception e) {
            logger.error("Error removing social comment {}", commentId, e);
            throw new RuntimeException("Social comment removal failed", e);
        }
    }
    
    public void removeMarketListing(Long listingId, String reason) {
        try {
            String url = marketServiceUrl + "/internal/listings/" + listingId.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            requestBody.put("removedAt", System.currentTimeMillis());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully removed market listing {}", listingId);
            } else {
                logger.warn("Failed to remove market listing {}: {}", listingId, response.getStatusCode());
                throw new RuntimeException("Failed to remove market listing");
            }
            
        } catch (Exception e) {
            logger.error("Error removing market listing {}", listingId, e);
            throw new RuntimeException("Market listing removal failed", e);
        }
    }
    
    public void removeBookingTrip(Long tripId, String reason) {
        try {
            String url = bookingServiceUrl + "/internal/trips/" + tripId.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            requestBody.put("removedAt", System.currentTimeMillis());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully removed booking trip {}", tripId);
            } else {
                logger.warn("Failed to remove booking trip {}: {}", tripId, response.getStatusCode());
                throw new RuntimeException("Failed to remove booking trip");
            }
            
        } catch (Exception e) {
            logger.error("Error removing booking trip {}", tripId, e);
            throw new RuntimeException("Booking trip removal failed", e);
        }
    }
    
    public boolean banUser(String userId, String reason) {
        try {
            String url = authServiceUrl + "/internal/users/" + userId + "/ban";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            // TODO: Add service-to-service authentication header
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            requestBody.put("bannedAt", System.currentTimeMillis());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Void.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                logger.info("Successfully banned user {}", userId);
            } else {
                logger.warn("Failed to ban user {}: {}", userId, response.getStatusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error banning user {}", userId, e);
            return false;
        }
    }
    
    private String getServiceUrlForType(ModerationItem.ModerationItemType type) {
        return switch (type) {
            case POST -> socialServiceUrl;
            case COMMENT -> socialServiceUrl;
            case TRIP -> bookingServiceUrl;
            case LISTING -> marketServiceUrl;
            case MESSAGE -> socialServiceUrl; // Assuming messages are handled by social service
            case USER_PROFILE -> authServiceUrl;
        };
    }
    
    private String getRemoveEndpointForType(ModerationItem.ModerationItemType type, String refId) {
        return switch (type) {
            case POST -> "/internal/posts/" + refId;
            case COMMENT -> "/internal/comments/" + refId;
            case TRIP -> "/internal/trips/" + refId;
            case LISTING -> "/internal/listings/" + refId;
            case MESSAGE -> "/internal/messages/" + refId;
            case USER_PROFILE -> "/internal/users/" + refId + "/profile";
        };
    }
}