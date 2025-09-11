package com.hopngo.social.service;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Generate embeddings for post content and upsert to Qdrant
     */
    public void processPostEmbedding(String postId, String text, List<String> tags, 
                                   String userId, Double lat, Double lng) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("id", postId);
            request.put("text", text);
            request.put("metadata", createPostMetadata(postId, tags, userId, lat, lng));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/ai/embeddings/upsert";
            logger.debug("Upserting post embedding to AI service: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully upserted embedding for post: {}", postId);
            } else {
                logger.warn("AI embedding service returned non-success status: {} for post: {}", 
                          response.getStatusCode(), postId);
            }
            
        } catch (Exception e) {
            logger.error("Error upserting post embedding for post: {}", postId, e);
            // Don't throw exception - embedding is not critical for post creation
        }
    }
    
    /**
     * Delete embedding from Qdrant when post is deleted
     */
    public void deletePostEmbedding(String postId) {
        try {
            String url = aiServiceUrl + "/ai/embeddings/" + postId;
            logger.debug("Deleting post embedding from AI service: {}", url);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url, HttpMethod.DELETE, null, Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted embedding for post: {}", postId);
            } else {
                logger.warn("AI embedding service returned non-success status: {} for post deletion: {}", 
                          response.getStatusCode(), postId);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting post embedding for post: {}", postId, e);
            // Don't throw exception - embedding deletion is not critical
        }
    }
    
    private Map<String, Object> createPostMetadata(String postId, List<String> tags, 
                                                  String userId, Double lat, Double lng) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "social_post");
        metadata.put("post_id", postId);
        metadata.put("user_id", userId);
        metadata.put("created_at", System.currentTimeMillis());
        
        if (tags != null && !tags.isEmpty()) {
            metadata.put("tags", tags);
        }
        
        if (lat != null && lng != null) {
            Map<String, Double> location = new HashMap<>();
            location.put("lat", lat);
            location.put("lng", lng);
            metadata.put("location", location);
        }
        
        return metadata;
    }
}