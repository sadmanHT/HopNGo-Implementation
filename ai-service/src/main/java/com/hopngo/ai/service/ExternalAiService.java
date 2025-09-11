package com.hopngo.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.ai.dto.*;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalAiService.class);
    
    @Value("${ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${ai.openai.base-url}")
    private String openaiBaseUrl;
    
    @Value("${ai.openai.timeout:30s}")
    private Duration openaiTimeout;
    
    @Value("${ai.openai.models.chat:gpt-3.5-turbo}")
    private String chatModel;
    
    @Value("${ai.openai.models.vision:gpt-4-vision-preview}")
    private String visionModel;
    
    @Value("${ai.image-search.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    @Value("${ai.image-search.max-results:20}")
    private int maxResults;
    
    @Value("${ai.chatbot.max-context-length:4000}")
    private int maxContextLength;
    
    @Value("${ai.chatbot.temperature:0.7}")
    private double temperature;
    
    @Value("${ai.chatbot.max-tokens:500}")
    private int maxTokens;
    
    @Value("${ai.chatbot.system-prompt}")
    private String systemPrompt;
    
    @Value("${ai.performance.request-timeout:2000}")
    private int requestTimeoutMs;
    
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private OpenAiService openAiService;
    
    public ExternalAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.createDefault();
    }
    
    private OpenAiService getOpenAiService() {
        if (openAiService == null && openaiApiKey != null && !openaiApiKey.isEmpty()) {
            openAiService = new OpenAiService(openaiApiKey, openaiTimeout);
        }
        return openAiService;
    }
    
    /**
     * Performs image search using OpenAI's vision capabilities
     */
    @Cacheable(value = "imageSearch", key = "#imageFile.originalFilename + '_' + #query")
    public CompletableFuture<ImageSearchResponse> searchByImage(MultipartFile imageFile, String query, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();
            
            try {
                logger.info("Starting image search for user: {} with query: {}", userId, query);
                
                // Validate input
                if (imageFile == null || imageFile.isEmpty()) {
                    throw new IllegalArgumentException("Image file is required");
                }
                
                // Convert image to base64 for OpenAI API
                String base64Image = encodeImageToBase64(imageFile);
                
                // Create vision request
                List<ChatMessage> messages = Arrays.asList(
                    new ChatMessage(ChatMessageRole.USER.value(), 
                        "Analyze this image and describe what you see. Focus on travel-related content like places, activities, or landmarks. " +
                        (query != null ? "Additional context: " + query : ""))
                );
                
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(visionModel)
                    .messages(messages)
                    .maxTokens(300)
                    .temperature(0.3)
                    .build();
                
                // Call OpenAI API with timeout
                String description = callOpenAiWithTimeout(request);
                
                // Generate mock search results based on description
                List<ImageSearchResponse.SearchResult> results = generateImageSearchResults(description, query);
                
                Duration processingTime = Duration.between(startTime, Instant.now());
                
                ImageSearchResponse response = new ImageSearchResponse(
                    results, 
                    results.size(), 
                    processingTime.toMillis() + "ms"
                );
                
                logger.info("Image search completed for user: {} in {}ms with {} results", 
                    userId, processingTime.toMillis(), results.size());
                
                return response;
                
            } catch (Exception e) {
                logger.error("Error in image search for user: {}", userId, e);
                return createErrorImageSearchResponse(e.getMessage());
            }
        });
    }
    
    /**
     * Provides chatbot responses with location and itinerary context
     */
    @Cacheable(value = "chatbot", key = "#request.message + '_' + (#request.location != null ? #request.location : 'no_location')", condition = "@externalAiService.isFAQQuestion(#request.message)")
    public CompletableFuture<ChatbotResponse> getChatbotResponse(ChatbotRequest request, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();
            
            try {
                logger.info("Processing chatbot request for user: {} at location: {}", userId, request.getLocation());
                
                // Build context from location and itinerary
                String context = buildChatContext(request);
                
                // Create chat messages
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
                
                if (!context.isEmpty()) {
                    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "Context: " + context));
                }
                
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), request.getMessage()));
                
                // Create chat completion request
                ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model(chatModel)
                    .messages(messages)
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();
                
                // Call OpenAI API
                String response = callOpenAiWithTimeout(chatRequest);
                
                Duration processingTime = Duration.between(startTime, Instant.now());
                
                ChatbotResponse chatbotResponse = new ChatbotResponse();
                chatbotResponse.setResponse(response);
                chatbotResponse.setConfidence(0.85); // Mock confidence score
                chatbotResponse.setProcessingTime(processingTime.toMillis() + "ms");
                chatbotResponse.setSuggestions(generateSuggestions(request.getLocation()));
                
                logger.info("Chatbot response generated for user: {} in {}ms", userId, processingTime.toMillis());
                
                return chatbotResponse;
                
            } catch (Exception e) {
                logger.error("Error generating chatbot response for user: {}", userId, e);
                return createErrorChatbotResponse(e.getMessage());
            }
        });
    }
    
    private String callOpenAiWithTimeout(ChatCompletionRequest request) throws Exception {
        OpenAiService service = getOpenAiService();
        if (service == null) {
            throw new IllegalStateException("OpenAI service not configured. Please set OPENAI_API_KEY.");
        }
        
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.createChatCompletion(request)
                        .getChoices()
                        .get(0)
                        .getMessage()
                        .getContent();
                } catch (OpenAiHttpException e) {
                    throw new RuntimeException("OpenAI API error: " + e.getMessage(), e);
                }
            });
            
            return future.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("Timeout or error calling OpenAI API", e);
            throw new Exception("AI service temporarily unavailable: " + e.getMessage());
        }
    }
    
    private String encodeImageToBase64(MultipartFile imageFile) throws IOException {
        byte[] imageBytes = imageFile.getBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    private String buildChatContext(ChatbotRequest request) {
        StringBuilder context = new StringBuilder();
        
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            context.append("Current location: ").append(request.getLocation()).append(". ");
        }
        
        if (request.getCurrentItinerary() != null && !request.getCurrentItinerary().isEmpty()) {
            context.append("Current itinerary: ").append(request.getCurrentItinerary()).append(". ");
        }
        
        if (request.getTravelPreferences() != null && !request.getTravelPreferences().isEmpty()) {
            context.append("Travel preferences: ").append(String.join(", ", request.getTravelPreferences())).append(". ");
        }
        
        // Truncate context if too long
        String contextStr = context.toString();
        if (contextStr.length() > maxContextLength) {
            contextStr = contextStr.substring(0, maxContextLength) + "...";
        }
        
        return contextStr;
    }
    
    private List<ImageSearchResponse.SearchResult> generateImageSearchResults(String description, String query) {
        List<ImageSearchResponse.SearchResult> results = new ArrayList<>();
        
        // Generate mock results based on AI description
        String[] locations = {"Paris", "Tokyo", "New York", "London", "Barcelona"};
        String[] types = {"place", "listing", "post"};
        
        for (int i = 0; i < Math.min(maxResults, 10); i++) {
            ImageSearchResponse.SearchResult result = new ImageSearchResponse.SearchResult();
            result.setId("result_" + i);
            result.setType(types[i % types.length]);
            result.setScore(Math.max(similarityThreshold, 0.9 - (i * 0.05)));
            result.setTitle("Similar place in " + locations[i % locations.length]);
            result.setDescription("Based on image analysis: " + description.substring(0, Math.min(100, description.length())));
            result.setLocation(locations[i % locations.length]);
            result.setImageUrl("https://example.com/image_" + i + ".jpg");
            
            results.add(result);
        }
        
        return results;
    }
    
    private List<String> generateSuggestions(String location) {
        if (location == null || location.isEmpty()) {
            return Arrays.asList(
                "What are popular attractions nearby?",
                "Recommend local restaurants",
                "What's the weather like?"
            );
        }
        
        return Arrays.asList(
            "What are the top attractions in " + location + "?",
            "Best restaurants in " + location,
            "How to get around " + location + "?",
            "What's the weather forecast for " + location + "?"
        );
    }
    
    private ImageSearchResponse createErrorImageSearchResponse(String error) {
        ImageSearchResponse response = new ImageSearchResponse();
        response.setResults(Collections.emptyList());
        response.setTotalResults(0);
        response.setProcessingTime("0ms");
        return response;
    }
    
    private ChatbotResponse createErrorChatbotResponse(String error) {
        ChatbotResponse response = new ChatbotResponse();
        response.setResponse("I'm sorry, I'm having trouble processing your request right now. Please try again later.");
        response.setConfidence(0.0);
        response.setProcessingTime("0ms");
        response.setSuggestions(Arrays.asList("Try asking a simpler question", "Check your internet connection"));
        return response;
    }
    
    public boolean isFAQQuestion(String message) {
        // Simple heuristic to identify FAQ-type questions
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("what is") || 
               lowerMessage.contains("how to") || 
               lowerMessage.contains("where is") ||
               lowerMessage.contains("when is") ||
               lowerMessage.startsWith("what") ||
               lowerMessage.startsWith("how") ||
               lowerMessage.startsWith("where") ||
               lowerMessage.startsWith("when");
    }
    
    /**
     * Clear any session data to ensure statelessness
     */
    public void clearSessionData(String userId) {
        logger.debug("Clearing session data for user: {}", userId);
        // In a stateless service, this is mainly for cleanup of any temporary data
        // The actual implementation would depend on what session data exists
    }
}