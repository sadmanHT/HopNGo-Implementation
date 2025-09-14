package com.hopngo.tripplanning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;

@Service
public class AIServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceClient.class);
    
    @Value("${ai.service.url:http://localhost:8088}")
    private String aiServiceUrl;
    
    @Value("${ai.service.timeout:30000}")
    private int timeoutMs;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public AIServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get next best destination suggestion from AI service
     */
    public String getNextDestinationSuggestion(String currentDestination, List<String> previousDestinations, 
                                              String travelStyle, Integer budget, Integer days) {
        try {
            logger.info("Requesting next destination suggestion for current: {}, style: {}, budget: {}, days: {}", 
                    currentDestination, travelStyle, budget, days);
            
            // Prepare the prompt for AI service
            String prompt = buildDestinationPrompt(currentDestination, previousDestinations, travelStyle, budget, days);
            
            // Create request payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", prompt);
            requestBody.put("conversationId", UUID.randomUUID().toString());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make the API call
            String url = aiServiceUrl + "/api/ai/chat";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseAIResponse(response.getBody());
            } else {
                logger.warn("AI service returned non-OK status: {}", response.getStatusCode());
                return getFallbackDestinationSuggestion(currentDestination, travelStyle);
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error calling AI service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return getFallbackDestinationSuggestion(currentDestination, travelStyle);
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling AI service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return getFallbackDestinationSuggestion(currentDestination, travelStyle);
        } catch (ResourceAccessException e) {
            logger.error("Timeout or connection error calling AI service: {}", e.getMessage());
            return getFallbackDestinationSuggestion(currentDestination, travelStyle);
        } catch (Exception e) {
            logger.error("Unexpected error calling AI service", e);
            return getFallbackDestinationSuggestion(currentDestination, travelStyle);
        }
    }
    
    /**
     * Build a comprehensive prompt for destination suggestion
     */
    private String buildDestinationPrompt(String currentDestination, List<String> previousDestinations, 
                                        String travelStyle, Integer budget, Integer days) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("As a travel expert, suggest the next best destination for a traveler. ");
        
        if (currentDestination != null && !currentDestination.trim().isEmpty()) {
            prompt.append("They are currently in or planning to visit: ").append(currentDestination).append(". ");
        }
        
        if (previousDestinations != null && !previousDestinations.isEmpty()) {
            prompt.append("They have previously visited: ")
                  .append(String.join(", ", previousDestinations))
                  .append(". ");
        }
        
        if (travelStyle != null && !travelStyle.trim().isEmpty()) {
            prompt.append("Their travel style is: ").append(travelStyle).append(". ");
        }
        
        if (budget != null && budget > 0) {
            prompt.append("Their budget is approximately $").append(budget).append(". ");
        }
        
        if (days != null && days > 0) {
            prompt.append("They plan to travel for ").append(days).append(" days. ");
        }
        
        prompt.append("Please suggest ONE specific destination that would be perfect for their next trip. ");
        prompt.append("Consider factors like travel distance, cultural diversity, seasonal weather, and unique experiences. ");
        prompt.append("Provide just the destination name and a brief 2-3 sentence explanation of why it's perfect for them. ");
        prompt.append("Format: 'Destination: [City, Country] - [Brief explanation]'");
        
        return prompt.toString();
    }
    
    /**
     * Parse AI service response to extract destination suggestion
     */
    private String parseAIResponse(String responseBody) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Try different possible response formats
            String aiMessage = null;
            
            // Check for 'response' field
            if (jsonResponse.has("response")) {
                aiMessage = jsonResponse.get("response").asText();
            }
            // Check for 'message' field
            else if (jsonResponse.has("message")) {
                aiMessage = jsonResponse.get("message").asText();
            }
            // Check for 'content' field
            else if (jsonResponse.has("content")) {
                aiMessage = jsonResponse.get("content").asText();
            }
            // Check for 'data' field with nested content
            else if (jsonResponse.has("data") && jsonResponse.get("data").has("response")) {
                aiMessage = jsonResponse.get("data").get("response").asText();
            }
            
            if (aiMessage != null && !aiMessage.trim().isEmpty()) {
                // Clean up the response
                aiMessage = aiMessage.trim();
                
                // If response is too long, truncate it
                if (aiMessage.length() > 500) {
                    aiMessage = aiMessage.substring(0, 497) + "...";
                }
                
                logger.debug("Successfully parsed AI response: {}", aiMessage);
                return aiMessage;
            } else {
                logger.warn("AI response does not contain expected message field");
                return "Unable to get destination suggestion from AI service";
            }
            
        } catch (Exception e) {
            logger.error("Error parsing AI service response: {}", e.getMessage());
            logger.debug("Raw response body: {}", responseBody);
            return "Error processing AI destination suggestion";
        }
    }
    
    /**
     * Provide fallback destination suggestions when AI service is unavailable
     */
    private String getFallbackDestinationSuggestion(String currentDestination, String travelStyle) {
        logger.info("Providing fallback destination suggestion for current: {}, style: {}", 
                currentDestination, travelStyle);
        
        // Simple rule-based fallback suggestions
        Map<String, List<String>> fallbackSuggestions = new HashMap<>();
        
        // Popular destinations by travel style
        fallbackSuggestions.put("adventure", Arrays.asList(
                "Destination: Queenstown, New Zealand - Perfect for adventure seekers with bungee jumping, skydiving, and stunning landscapes.",
                "Destination: Interlaken, Switzerland - Offers incredible mountain adventures, paragliding, and scenic hiking trails.",
                "Destination: Costa Rica - Amazing for zip-lining, wildlife watching, and volcano exploration."
        ));
        
        fallbackSuggestions.put("cultural", Arrays.asList(
                "Destination: Kyoto, Japan - Rich cultural heritage with ancient temples, traditional gardens, and authentic experiences.",
                "Destination: Florence, Italy - Renaissance art, historic architecture, and world-class museums await.",
                "Destination: Istanbul, Turkey - Where East meets West with incredible history, architecture, and cuisine."
        ));
        
        fallbackSuggestions.put("relaxation", Arrays.asList(
                "Destination: Maldives - Crystal clear waters, overwater bungalows, and ultimate tropical relaxation.",
                "Destination: Santorini, Greece - Stunning sunsets, beautiful beaches, and peaceful island atmosphere.",
                "Destination: Bali, Indonesia - Perfect blend of beaches, spas, and serene cultural experiences."
        ));
        
        fallbackSuggestions.put("budget", Arrays.asList(
                "Destination: Prague, Czech Republic - Beautiful architecture, rich history, and very affordable prices.",
                "Destination: Vietnam - Incredible food, stunning landscapes, and excellent value for money.",
                "Destination: Portugal - Charming cities, beautiful coastline, and budget-friendly European destination."
        ));
        
        // Default suggestions
        List<String> defaultSuggestions = Arrays.asList(
                "Destination: Barcelona, Spain - Vibrant culture, stunning architecture, and Mediterranean charm.",
                "Destination: Tokyo, Japan - Modern metropolis with incredible food, technology, and traditional culture.",
                "Destination: Cape Town, South Africa - Dramatic landscapes, wine regions, and diverse experiences."
        );
        
        // Select appropriate suggestions based on travel style
        List<String> suggestions = fallbackSuggestions.getOrDefault(
                travelStyle != null ? travelStyle.toLowerCase() : "default", 
                defaultSuggestions
        );
        
        // Return a random suggestion
        Random random = new Random();
        String suggestion = suggestions.get(random.nextInt(suggestions.size()));
        
        logger.debug("Selected fallback suggestion: {}", suggestion);
        return suggestion + " (Note: AI service temporarily unavailable, showing curated suggestion)";
    }
    
    /**
     * Get travel tips from AI service
     */
    public String getTravelTips(String destination, String travelStyle, Integer budget) {
        try {
            logger.info("Requesting travel tips for destination: {}, style: {}, budget: {}", 
                    destination, travelStyle, budget);
            
            String prompt = buildTravelTipsPrompt(destination, travelStyle, budget);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", prompt);
            requestBody.put("conversationId", UUID.randomUUID().toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = aiServiceUrl + "/api/ai/chat";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseAIResponse(response.getBody());
            } else {
                return "Unable to get travel tips at this time. Please try again later.";
            }
            
        } catch (Exception e) {
            logger.error("Error getting travel tips from AI service", e);
            return "Travel tips temporarily unavailable. Please check back later.";
        }
    }
    
    /**
     * Build prompt for travel tips
     */
    private String buildTravelTipsPrompt(String destination, String travelStyle, Integer budget) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Provide 3-5 practical travel tips for visiting ").append(destination).append(". ");
        
        if (travelStyle != null) {
            prompt.append("The traveler prefers ").append(travelStyle).append(" style travel. ");
        }
        
        if (budget != null && budget > 0) {
            prompt.append("Their budget is around $").append(budget).append(". ");
        }
        
        prompt.append("Include tips about local customs, must-see attractions, food recommendations, and money-saving advice. ");
        prompt.append("Keep each tip concise and actionable.");
        
        return prompt.toString();
    }
    
    /**
     * Check if AI service is available
     */
    public boolean isAIServiceAvailable() {
        try {
            String url = aiServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.debug("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}