package com.hopngo.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiEmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiEmbeddingService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${ai-service.url:http://localhost:8085}")
    private String aiServiceUrl;
    
    public AiEmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Generate embeddings for listing content and upsert to Qdrant
     */
    public void processListingEmbedding(UUID listingId, String title, String description, 
                                      String category, List<String> amenities, 
                                      BigDecimal basePrice, String currency,
                                      Double lat, Double lng, String vendorId) {
        try {
            // Combine title and description for embedding
            String text = title + ". " + (description != null ? description : "");
            
            Map<String, Object> request = new HashMap<>();
            request.put("id", listingId.toString());
            request.put("text", text);
            request.put("metadata", createListingMetadata(listingId, title, category, 
                                                         amenities, basePrice, currency, 
                                                         lat, lng, vendorId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/ai/embeddings/upsert";
            logger.debug("Upserting listing embedding to AI service: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully upserted embedding for listing: {}", listingId);
            } else {
                logger.warn("AI embedding service returned non-success status: {} for listing: {}", 
                          response.getStatusCode(), listingId);
            }
            
        } catch (Exception e) {
            logger.error("Error upserting listing embedding for listing: {}", listingId, e);
            // Don't throw exception - embedding is not critical for listing creation
        }
    }
    
    /**
     * Generate embeddings for booking content and upsert to Qdrant
     */
    public void processBookingEmbedding(UUID bookingId, String userId, UUID listingId,
                                      String listingTitle, LocalDate startDate, 
                                      LocalDate endDate, Integer guests, 
                                      String specialRequests, BigDecimal totalAmount) {
        try {
            // Create text content for booking embedding
            String text = "Booking for " + listingTitle;
            if (specialRequests != null && !specialRequests.trim().isEmpty()) {
                text += ". Special requests: " + specialRequests;
            }
            
            Map<String, Object> request = new HashMap<>();
            request.put("id", bookingId.toString());
            request.put("text", text);
            request.put("metadata", createBookingMetadata(bookingId, userId, listingId,
                                                         startDate, endDate, guests, 
                                                         totalAmount));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/ai/embeddings/upsert";
            logger.debug("Upserting booking embedding to AI service: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully upserted embedding for booking: {}", bookingId);
            } else {
                logger.warn("AI embedding service returned non-success status: {} for booking: {}", 
                          response.getStatusCode(), bookingId);
            }
            
        } catch (Exception e) {
            logger.error("Error upserting booking embedding for booking: {}", bookingId, e);
            // Don't throw exception - embedding is not critical for booking creation
        }
    }
    
    /**
     * Delete embedding from Qdrant when listing is deleted
     */
    public void deleteListingEmbedding(UUID listingId) {
        try {
            String url = aiServiceUrl + "/ai/embeddings/" + listingId.toString();
            logger.debug("Deleting listing embedding from AI service: {}", url);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url, HttpMethod.DELETE, null, Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted embedding for listing: {}", listingId);
            } else {
                logger.warn("AI embedding service returned non-success status: {} for listing deletion: {}", 
                          response.getStatusCode(), listingId);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting listing embedding for listing: {}", listingId, e);
            // Don't throw exception - embedding deletion is not critical
        }
    }
    
    /**
     * Delete embedding from Qdrant when booking is cancelled
     */
    public void deleteBookingEmbedding(UUID bookingId) {
        try {
            String url = aiServiceUrl + "/ai/embeddings/" + bookingId.toString();
            logger.debug("Deleting booking embedding from AI service: {}", url);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url, HttpMethod.DELETE, null, Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted embedding for booking: {}", bookingId);
            } else {
                logger.warn("AI embedding service returned non-success status: {} for booking deletion: {}", 
                          response.getStatusCode(), bookingId);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting booking embedding for booking: {}", bookingId, e);
            // Don't throw exception - embedding deletion is not critical
        }
    }
    
    private Map<String, Object> createListingMetadata(UUID listingId, String title, 
                                                     String category, List<String> amenities,
                                                     BigDecimal basePrice, String currency,
                                                     Double lat, Double lng, String vendorId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "listing");
        metadata.put("listing_id", listingId.toString());
        metadata.put("title", title);
        metadata.put("category", category);
        metadata.put("vendor_id", vendorId);
        metadata.put("created_at", System.currentTimeMillis());
        
        if (amenities != null && !amenities.isEmpty()) {
            metadata.put("amenities", amenities);
        }
        
        if (basePrice != null) {
            metadata.put("base_price", basePrice.doubleValue());
            metadata.put("currency", currency);
        }
        
        if (lat != null && lng != null) {
            Map<String, Double> location = new HashMap<>();
            location.put("lat", lat);
            location.put("lng", lng);
            metadata.put("location", location);
        }
        
        return metadata;
    }
    
    private Map<String, Object> createBookingMetadata(UUID bookingId, String userId, 
                                                     UUID listingId, LocalDate startDate,
                                                     LocalDate endDate, Integer guests,
                                                     BigDecimal totalAmount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "booking");
        metadata.put("booking_id", bookingId.toString());
        metadata.put("user_id", userId);
        metadata.put("listing_id", listingId.toString());
        metadata.put("start_date", startDate.toString());
        metadata.put("end_date", endDate.toString());
        metadata.put("guests", guests);
        metadata.put("created_at", System.currentTimeMillis());
        
        if (totalAmount != null) {
            metadata.put("total_amount", totalAmount.doubleValue());
        }
        
        return metadata;
    }
}