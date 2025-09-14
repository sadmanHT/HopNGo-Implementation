package com.hopngo.ai.controller;

import com.hopngo.ai.dto.*;
import com.hopngo.ai.service.ModerationService;
import com.hopngo.ai.service.ExternalAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import io.github.resilience4j.ratelimiter.annotation.RateLimiter; // Disabled for minimal profile
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Service", description = "AI-powered services for travel planning and search")
public class AiController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiController.class);
    private final Random random = new Random(42); // Seedable random for deterministic responses
    
    @Autowired
    private ModerationService moderationService;
    
    @Autowired
    private ExternalAiService externalAiService;
    
    @PostMapping("/itinerary")
    @Operation(summary = "Generate travel itinerary", description = "Creates a structured travel itinerary based on destinations, budget, and interests")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    @Cacheable(value = "itineraries", key = "#request.origin + '_' + #request.destinations + '_' + #request.days + '_' + #request.budget")
    public ResponseEntity<ItineraryResponse> generateItinerary(
            @Valid @RequestBody ItineraryRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        // Generate deterministic mock itinerary
        String itineraryId = UUID.nameUUIDFromBytes((request.getOrigin() + request.getDestinations().toString()).getBytes()).toString();
        
        String title = "Amazing " + request.getDays() + "-Day Journey";
        String description = "Explore " + String.join(", ", request.getDestinations()) + " starting from " + request.getOrigin();
        
        List<ItineraryResponse.DayPlan> dayPlans = new ArrayList<>();
        int budgetPerDay = request.getBudget() / request.getDays();
        
        for (int day = 1; day <= request.getDays(); day++) {
            String location = request.getDestinations().get((day - 1) % request.getDestinations().size());
            String dayTitle = "Day " + day + " in " + location;
            
            List<ItineraryResponse.Activity> activities = generateMockActivities(location, request.getInterests(), budgetPerDay);
            
            dayPlans.add(new ItineraryResponse.DayPlan(day, location, dayTitle, activities, budgetPerDay));
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generatedAt", LocalDateTime.now().toString());
        metadata.put("userId", userId);
        metadata.put("interests", request.getInterests());
        
        ItineraryResponse response = new ItineraryResponse(
            itineraryId, title, description, request.getDays(), 
            request.getBudget(), dayPlans, metadata
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/image-search")
    @Operation(summary = "Search by image", description = "Find similar places, posts, or listings based on an image")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    public ResponseEntity<ImageSearchResponse> searchByImage(
            @Valid @RequestBody ImageSearchRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            // Use external AI service for image search
            ImageSearchResponse response = externalAiService.searchByImage(
                request.getImageFile(), 
                request.getQuery(), 
                userId
            ).get(2, java.util.concurrent.TimeUnit.SECONDS);
            
            // Clear session data to ensure statelessness
            externalAiService.clearSessionData(userId);
            
            return ResponseEntity.ok(response);
            
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Timeout in image search for user: {}", userId, e);
            return ResponseEntity.status(408).build(); // Request Timeout
        } catch (Exception e) {
            logger.error("Error in image search for user: {}", userId, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/image-search/upload")
    @Operation(summary = "Search by uploaded image", description = "Find similar places, posts, or listings based on an uploaded image")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    public ResponseEntity<ImageSearchResponse> searchByUploadedImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "maxResults", defaultValue = "10") int maxResults,
            @RequestHeader("X-User-Id") String userId) {
        
        // Mock processing of uploaded image
        ImageSearchRequest request = new ImageSearchRequest("uploaded://" + image.getOriginalFilename(), maxResults);
        return searchByImage(request, userId);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Descriptive search", description = "Search for travel content using natural language descriptions")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    @Cacheable(value = "searches", key = "#request.query + '_' + #request.category")
    public ResponseEntity<SearchResponse> descriptiveSearch(
            @Valid @RequestBody SearchRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        List<SearchResponse.RankedResult> results = new ArrayList<>();
        
        // Generate mock ranked results
        String[] categories = {"attraction", "restaurant", "hotel", "activity", "transport"};
        String[] locations = {"Paris", "Tokyo", "New York", "London", "Sydney", "Rome", "Barcelona"};
        
        for (int i = 0; i < Math.min(request.getMaxResults(), 20); i++) {
            String category = categories[i % categories.length];
            String location = locations[i % locations.length];
            double score = 1.0 - (i * 0.03); // Decreasing relevance
            
            results.add(new SearchResponse.RankedResult(
                UUID.randomUUID().toString(),
                "Amazing " + category + " in " + location,
                "Perfect match for your search: " + request.getQuery(),
                category,
                score,
                "https://example.com/" + category + "/" + i,
                "https://example.com/image" + i + ".jpg",
                location
            ));
        }
        
        SearchResponse response = new SearchResponse(
            results, results.size(), request.getQuery(), "200ms"
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/chatbot")
    @Operation(summary = "AI Chatbot", description = "Get location-aware travel suggestions and answers")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    public ResponseEntity<ChatbotResponse> chatbot(
            @Valid @RequestBody ChatbotRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            // Use external AI service for chatbot response
            ChatbotResponse response = externalAiService.getChatbotResponse(request, userId)
                .get(2, java.util.concurrent.TimeUnit.SECONDS);
            
            // Clear session data to ensure statelessness
            externalAiService.clearSessionData(userId);
            
            return ResponseEntity.ok(response);
            
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Timeout in chatbot for user: {}", userId, e);
            return ResponseEntity.status(408).build(); // Request Timeout
        } catch (Exception e) {
            logger.error("Error in chatbot for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/weather")
    @Operation(summary = "Weather information", description = "Get current weather and forecast for a location")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    @Cacheable(value = "weather", key = "#lat + '_' + #lng")
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng,
            @RequestHeader("X-User-Id") String userId) {
        
        // Generate mock weather data
        WeatherResponse.CurrentWeather current = new WeatherResponse.CurrentWeather(
            22.5, 25.0, 65, 12.5, "NW", "Partly Cloudy", "partly-cloudy", 10.0, 6.5
        );
        
        List<WeatherResponse.DailyForecast> forecast = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String date = LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            forecast.add(new WeatherResponse.DailyForecast(
                date, 25.0 + i, 15.0 + i, "Sunny", "sunny", 10 + (i * 5), 10.0 + i
            ));
        }
        
        WeatherResponse response = new WeatherResponse(
            current, forecast, "Location (" + lat + ", " + lng + ")", 
            "UTC", LocalDateTime.now().toString()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/moderateContent")
    @Operation(summary = "Content moderation", description = "Analyze content for toxicity, NSFW, spam, and other policy violations")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    public ResponseEntity<ModerationResponse> moderateContent(
            @Valid @RequestBody ModerationRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        // Set user ID from header if not provided in request
        if (request.getUserId() == null) {
            request.setUserId(userId);
        }
        
        ModerationResponse response = moderationService.moderateContent(request);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/embeddings")
    @Operation(summary = "Generate embeddings", description = "Generate vector embeddings for text content using AI models")
    // @RateLimiter(name = "ai-endpoints") // Disabled for minimal profile
    @Cacheable(value = "embeddings", key = "#request.text.hashCode()")
    public ResponseEntity<EmbeddingsResponse> generateEmbeddings(
            @Valid @RequestBody EmbeddingsRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        // Generate deterministic mock embeddings based on text content
        List<Double> embedding = generateDeterministicEmbedding(request.getText());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", "mock-embedding-model-v1");
        metadata.put("dimensions", embedding.size());
        metadata.put("textLength", request.getText().length());
        metadata.put("generatedAt", LocalDateTime.now().toString());
        metadata.put("userId", userId);
        
        EmbeddingsResponse response = new EmbeddingsResponse(
            embedding, metadata, "200ms"
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Helper methods
    private List<ItineraryResponse.Activity> generateMockActivities(String location, List<String> interests, int budget) {
        List<ItineraryResponse.Activity> activities = new ArrayList<>();
        
        String[] activityTypes = {"sightseeing", "dining", "shopping", "cultural", "adventure"};
        String[] times = {"09:00", "12:00", "15:00", "18:00", "20:00"};
        
        for (int i = 0; i < 3; i++) {
            String type = activityTypes[i % activityTypes.length];
            String name = "Visit " + location + " " + type.substring(0, 1).toUpperCase() + type.substring(1);
            
            activities.add(new ItineraryResponse.Activity(
                name,
                "Enjoy " + type + " experience in " + location,
                times[i % times.length],
                type,
                budget / 4, // Quarter of daily budget per activity
                location
            ));
        }
        
        return activities;
    }
    
    private String generateMockChatResponse(String message, String location) {
        String[] responses = {
            "That's a great question! Based on your location" + (location != null ? " in " + location : "") + ", I'd recommend...",
            "I can help you with that! Here are some suggestions for" + (location != null ? " " + location : " your area") + "...",
            "Interesting! Let me share some local insights" + (location != null ? " about " + location : "") + "...",
            "Perfect timing to ask! Here's what I know" + (location != null ? " about " + location : "") + "..."
        };
        
        return responses[Math.abs(message.hashCode()) % responses.length];
    }
    
    /**
     * Generate deterministic embeddings based on text content
     * This creates a 1536-dimensional vector (OpenAI embedding size) with deterministic values
     */
    private List<Double> generateDeterministicEmbedding(String text) {
        List<Double> embedding = new ArrayList<>();
        
        // Use text hash as seed for deterministic generation
        Random textRandom = new Random(text.hashCode());
        
        // Generate 1536 dimensions (standard embedding size)
        for (int i = 0; i < 1536; i++) {
            // Generate values between -1 and 1 with normal distribution
            double value = textRandom.nextGaussian() * 0.3; // Scale to reasonable range
            embedding.add(Math.max(-1.0, Math.min(1.0, value))); // Clamp to [-1, 1]
        }
        
        // Normalize the vector to unit length for cosine similarity
        double magnitude = Math.sqrt(embedding.stream().mapToDouble(d -> d * d).sum());
        if (magnitude > 0) {
            embedding = embedding.stream().map(d -> d / magnitude).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        return embedding;
    }
}