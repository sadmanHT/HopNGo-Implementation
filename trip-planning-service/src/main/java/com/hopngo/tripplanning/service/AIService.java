package com.hopngo.tripplanning.service;

import com.hopngo.tripplanning.dto.TripPlanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;
    private final Duration timeout;

    public AIService(RestTemplate restTemplate,
                    @Value("${app.ai-service.url}") String aiServiceUrl,
                    @Value("${app.ai-service.timeout:30000}") long timeoutMs) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /**
     * Generate itinerary using AI service with fallback to mock response
     */
    public Map<String, Object> generateItinerary(TripPlanRequest request) {
        logger.info("Generating itinerary for trip: {}", request.getTitle());
        
        try {
            // Prepare request for AI service
            Map<String, Object> aiRequest = createAIRequest(request);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-User-Id", "trip-planning-service"); // Service-to-service call
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(aiRequest, headers);
            
            // Call AI service with timeout
            logger.debug("Calling AI service at: {}/ai/itinerary", aiServiceUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiServiceUrl + "/ai/itinerary", 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully received AI-generated itinerary for trip: {}", request.getTitle());
                return response.getBody();
            } else {
                logger.warn("AI service returned non-success status: {}, falling back to mock", response.getStatusCode());
                return generateMockItinerary(request);
            }
            
        } catch (ResourceAccessException e) {
            logger.warn("AI service timeout or connection error, falling back to mock response: {}", e.getMessage());
            return generateMockItinerary(request);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.warn("AI service HTTP error ({}), falling back to mock response: {}", e.getStatusCode(), e.getMessage());
            return generateMockItinerary(request);
        } catch (Exception e) {
            logger.error("Unexpected error calling AI service, falling back to mock response", e);
            return generateMockItinerary(request);
        }
    }

    /**
     * Create request payload for AI service
     */
    private Map<String, Object> createAIRequest(TripPlanRequest request) {
        Map<String, Object> aiRequest = new HashMap<>();
        
        // Map TripPlanRequest to AI service format
        aiRequest.put("origin", request.getOrigin());
        aiRequest.put("destinations", request.getDestinations());
        aiRequest.put("days", request.getDays());
        aiRequest.put("budget", request.getBudget()); // Budget in cents
        aiRequest.put("interests", request.getInterests() != null ? request.getInterests() : new java.util.ArrayList<>());
        
        return aiRequest;
    }

    /**
     * Generate a mock itinerary response for testing
     * This will be replaced with actual AI service integration
     */
    private Map<String, Object> generateMockItinerary(TripPlanRequest request) {
        logger.info("Generating mock itinerary for trip: {}", request.getTitle());
        
        Map<String, Object> itinerary = new HashMap<>();
        itinerary.put("title", request.getTitle());
        itinerary.put("totalDays", request.getDays());
        itinerary.put("estimatedBudget", request.getBudget());
        
        // Generate mock daily activities
        Map<String, Object>[] dailyPlans = new Map[request.getDays()];
        for (int i = 0; i < request.getDays(); i++) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("day", i + 1);
            dayPlan.put("date", "2024-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i % 28) + 1));
            dayPlan.put("location", i < request.getDestinations().size() ? 
                request.getDestinations().get(i) : request.getDestinations().get(0));
            
            // Mock activities
            Map<String, Object>[] activities = new Map[3];
            activities[0] = Map.of("time", "09:00", "activity", "Morning sightseeing", "duration", "3 hours");
            activities[1] = Map.of("time", "14:00", "activity", "Lunch and local exploration", "duration", "2 hours");
            activities[2] = Map.of("time", "19:00", "activity", "Evening entertainment", "duration", "2 hours");
            
            dayPlan.put("activities", activities);
            dayPlan.put("estimatedCost", request.getBudget() / request.getDays());
            
            dailyPlans[i] = dayPlan;
        }
        
        itinerary.put("dailyPlans", dailyPlans);
        itinerary.put("generatedBy", "HopNGo AI Assistant (Mock)");
        itinerary.put("generatedAt", java.time.OffsetDateTime.now().toString());
        
        return itinerary;
    }
}