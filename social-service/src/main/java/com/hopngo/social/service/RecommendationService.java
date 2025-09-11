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
public class RecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${ai-service.url:http://localhost:8085}")
    private String aiServiceUrl;
    
    public RecommendationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public UserRecommendationsResponse getUserRecommendations(String userId, int limit) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("limit", limit);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/ai/similarity/users";
            logger.debug("Calling AI service for user recommendations: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return parseUserRecommendationsResponse(body);
            } else {
                logger.warn("AI service returned non-success status: {}", response.getStatusCode());
                return createEmptyUserRecommendationsResponse();
            }
            
        } catch (Exception e) {
            logger.error("Error calling AI service for user recommendations", e);
            return createEmptyUserRecommendationsResponse();
        }
    }
    
    @SuppressWarnings("unchecked")
    private UserRecommendationsResponse parseUserRecommendationsResponse(Map<String, Object> body) {
        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) body.getOrDefault("recommendations", List.of());
        String algorithm = (String) body.getOrDefault("algorithm", "unknown");
        Boolean cached = (Boolean) body.getOrDefault("cached", false);
        Long processingTime = ((Number) body.getOrDefault("processingTime", 0)).longValue();
        
        List<RecommendedUser> users = recommendations.stream()
            .map(this::parseRecommendedUser)
            .toList();
        
        return new UserRecommendationsResponse(users, algorithm, cached, processingTime);
    }
    
    private RecommendedUser parseRecommendedUser(Map<String, Object> userMap) {
        String userId = (String) userMap.get("itemId");
        String type = (String) userMap.get("type");
        Double score = ((Number) userMap.getOrDefault("score", 0.0)).doubleValue();
        String reason = (String) userMap.getOrDefault("reason", "Similar interests");
        
        return new RecommendedUser(userId, type, score, reason);
    }
    
    private UserRecommendationsResponse createEmptyUserRecommendationsResponse() {
        return new UserRecommendationsResponse(List.of(), "fallback", false, 0L);
    }
    
    public static class UserRecommendationsResponse {
        private final List<RecommendedUser> recommendations;
        private final String algorithm;
        private final boolean cached;
        private final long processingTime;
        
        public UserRecommendationsResponse(List<RecommendedUser> recommendations, String algorithm, boolean cached, long processingTime) {
            this.recommendations = recommendations;
            this.algorithm = algorithm;
            this.cached = cached;
            this.processingTime = processingTime;
        }
        
        public List<RecommendedUser> getRecommendations() { return recommendations; }
        public String getAlgorithm() { return algorithm; }
        public boolean isCached() { return cached; }
        public long getProcessingTime() { return processingTime; }
    }
    
    public static class RecommendedUser {
        private final String userId;
        private final String type;
        private final double score;
        private final String reason;
        
        public RecommendedUser(String userId, String type, double score, String reason) {
            this.userId = userId;
            this.type = type;
            this.score = score;
            this.reason = reason;
        }
        
        public String getUserId() { return userId; }
        public String getType() { return type; }
        public double getScore() { return score; }
        public String getReason() { return reason; }
    }
}