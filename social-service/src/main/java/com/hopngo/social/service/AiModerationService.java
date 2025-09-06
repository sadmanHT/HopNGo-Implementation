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
public class AiModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiModerationService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${ai-service.url:http://localhost:8085}")
    private String aiServiceUrl;
    
    @Value("${moderation.toxicity-threshold:0.7}")
    private double toxicityThreshold;
    
    @Value("${moderation.nsfw-threshold:0.8}")
    private double nsfwThreshold;
    
    @Value("${moderation.spam-threshold:0.6}")
    private double spamThreshold;
    
    public AiModerationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public ModerationResult moderateContent(String text, List<String> mediaUrls) {
        try {
            Map<String, Object> request = new HashMap<>();
            if (text != null && !text.trim().isEmpty()) {
                request.put("text", text);
            }
            if (mediaUrls != null && !mediaUrls.isEmpty()) {
                request.put("mediaUrls", mediaUrls);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/ai/moderateContent";
            logger.debug("Calling AI moderation service: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return parseResponse(body);
            } else {
                logger.warn("AI moderation service returned non-success status: {}", response.getStatusCode());
                return createDefaultAllowResult();
            }
            
        } catch (Exception e) {
            logger.error("Error calling AI moderation service", e);
            // Fail open - allow content if AI service is unavailable
            return createDefaultAllowResult();
        }
    }
    
    private ModerationResult parseResponse(Map<String, Object> body) {
        double toxicityScore = getDoubleValue(body, "toxicityScore", 0.0);
        double nsfwScore = getDoubleValue(body, "nsfwScore", 0.0);
        double spamScore = getDoubleValue(body, "spamScore", 0.0);
        
        String decision = (String) body.getOrDefault("decision", "ALLOW");
        @SuppressWarnings("unchecked")
        List<String> reasons = (List<String>) body.getOrDefault("reasons", List.of());
        
        return new ModerationResult(
            toxicityScore,
            nsfwScore,
            spamScore,
            ModerationDecision.valueOf(decision),
            reasons
        );
    }
    
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    private ModerationResult createDefaultAllowResult() {
        return new ModerationResult(
            0.0, 0.0, 0.0,
            ModerationDecision.ALLOW,
            List.of()
        );
    }
    
    public boolean shouldFlag(ModerationResult result) {
        return result.getToxicityScore() >= toxicityThreshold ||
               result.getNsfwScore() >= nsfwThreshold ||
               result.getSpamScore() >= spamThreshold ||
               result.getDecision() == ModerationDecision.FLAG ||
               result.getDecision() == ModerationDecision.BLOCK;
    }
    
    public static class ModerationResult {
        private final double toxicityScore;
        private final double nsfwScore;
        private final double spamScore;
        private final ModerationDecision decision;
        private final List<String> reasons;
        
        public ModerationResult(double toxicityScore, double nsfwScore, double spamScore,
                              ModerationDecision decision, List<String> reasons) {
            this.toxicityScore = toxicityScore;
            this.nsfwScore = nsfwScore;
            this.spamScore = spamScore;
            this.decision = decision;
            this.reasons = reasons;
        }
        
        public double getToxicityScore() { return toxicityScore; }
        public double getNsfwScore() { return nsfwScore; }
        public double getSpamScore() { return spamScore; }
        public ModerationDecision getDecision() { return decision; }
        public List<String> getReasons() { return reasons; }
    }
    
    public enum ModerationDecision {
        ALLOW, FLAG, BLOCK
    }
}