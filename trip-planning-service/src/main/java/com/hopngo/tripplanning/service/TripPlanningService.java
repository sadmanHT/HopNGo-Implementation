package com.hopngo.tripplanning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.tripplanning.dto.*;
import com.hopngo.tripplanning.entity.Itinerary;
import com.hopngo.tripplanning.mapper.ItineraryMapper;
import com.hopngo.tripplanning.repository.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TripPlanningService {

    private static final Logger logger = LoggerFactory.getLogger(TripPlanningService.class);

    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;
    private final RecommendationService recommendationService;
    private final AIServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public TripPlanningService(ItineraryRepository itineraryRepository,
                             ItineraryMapper itineraryMapper,
                             RecommendationService recommendationService,
                             AIServiceClient aiServiceClient,
                             ObjectMapper objectMapper) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryMapper = itineraryMapper;
        this.recommendationService = recommendationService;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Plan a new trip with AI assistance and personalization
     */
    @Transactional
    public ItineraryResponse planTrip(TripPlanRequest request, String userId) {
        logger.info("Planning trip for user: {} with title: {}", userId, request.getTitle());
        
        try {
            // Validate request
            validateTripPlanRequest(request);
            
            // Get AI suggestions for next destinations if requested
            String aiSuggestion = null;
            // Note: AI suggestions feature temporarily disabled - method not available in current TripPlanRequest
            // if (request.isIncludeAISuggestions()) {
            //     aiSuggestion = getAIDestinationSuggestion(request, userId);
            // }
            
            // Create base itinerary
            Itinerary itinerary = createBaseItinerary(request, userId);
            
            // Add AI suggestion to title if available
            if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
                String enhancedTitle = itinerary.getTitle() + 
                    " - AI Suggestion: " + aiSuggestion;
                itinerary.setTitle(enhancedTitle);
            }
            
            // Save the itinerary
            Itinerary savedItinerary = itineraryRepository.save(itinerary);
            logger.info("Created itinerary with ID: {} for user: {}", savedItinerary.getId(), userId);
            
            // Record user interaction for recommendation engine
            recordUserInteraction(userId, savedItinerary, "created", 5);
            
            // Update user preferences based on this itinerary
            recommendationService.updateUserPreferences(userId, savedItinerary, "created");
            
            return itineraryMapper.toResponse(savedItinerary);
            
        } catch (Exception e) {
            logger.error("Error planning trip for user: {}", userId, e);
            throw new RuntimeException("Failed to plan trip: " + e.getMessage(), e);
        }
    }

    /**
     * Get all itineraries for a user with personalized recommendations
     */
    @Transactional(readOnly = true)
    public ItineraryListResponse getUserItineraries(String userId, Pageable pageable, boolean includeRecommendations) {
        logger.info("Getting itineraries for user: {}, includeRecommendations: {}", userId, includeRecommendations);
        
        try {
            // Get user's own itineraries
            Page<Itinerary> userItineraries = itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            List<ItineraryResponse> itineraryResponses = userItineraries.getContent().stream()
                    .map(itineraryMapper::toResponse)
                    .collect(Collectors.toList());
            
            // Get personalized recommendations if requested
            List<ItineraryResponse> recommendations = new ArrayList<>();
            if (includeRecommendations) {
                try {
                    recommendations = recommendationService.getRecommendationsForUser(userId, 5);
                    logger.debug("Found {} recommendations for user: {}", recommendations.size(), userId);
                } catch (Exception e) {
                    logger.warn("Failed to get recommendations for user: {}", userId, e);
                }
            }
            
            return new ItineraryListResponse(
                    itineraryResponses,
                    recommendations,
                    userItineraries.getTotalElements(),
                    userItineraries.getTotalPages(),
                    userItineraries.getNumber(),
                    userItineraries.getSize()
            );
            
        } catch (Exception e) {
            logger.error("Error getting itineraries for user: {}", userId, e);
            throw new RuntimeException("Failed to get user itineraries: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific itinerary by ID
     */
    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(UUID itineraryId, String userId) {
        logger.info("Getting itinerary: {} for user: {}", itineraryId, userId);
        
        Optional<Itinerary> itinerary = itineraryRepository.findById(itineraryId);
        if (itinerary.isEmpty()) {
            throw new RuntimeException("Itinerary not found with ID: " + itineraryId);
        }
        
        Itinerary foundItinerary = itinerary.get();
        
        // Check if itinerary belongs to the user (public access check disabled for now)
        if (!foundItinerary.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: Itinerary does not belong to the user");
        }
        
        // Record view interaction if not the owner
        if (!foundItinerary.getUserId().equals(userId)) {
            recordUserInteraction(userId, foundItinerary, "viewed", null);
        }
        
        return itineraryMapper.toResponse(foundItinerary);
    }

    /**
     * Update an existing itinerary
     */
    @Transactional
    public ItineraryResponse updateItinerary(UUID itineraryId, TripPlanRequest request, String userId) {
        logger.info("Updating itinerary: {} for user: {}", itineraryId, userId);
        
        Optional<Itinerary> existingItinerary = itineraryRepository.findById(itineraryId);
        if (existingItinerary.isEmpty()) {
            throw new RuntimeException("Itinerary not found with ID: " + itineraryId);
        }
        
        Itinerary itinerary = existingItinerary.get();
        
        // Check ownership
        if (!itinerary.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: User can only update their own itineraries");
        }
        
        // Update itinerary fields
        updateItineraryFields(itinerary, request);
        
        // Get AI suggestions if requested
        // Note: AI suggestions feature temporarily disabled - method not available in current TripPlanRequest
        // if (request.isIncludeAISuggestions()) {
        //     String aiSuggestion = getAIDestinationSuggestion(request, userId);
        //     if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
        //         String enhancedTitle = itinerary.getTitle() + 
        //             " - Updated AI Suggestion: " + aiSuggestion;
        //         itinerary.setTitle(enhancedTitle);
        //     }
        // }
        
        itinerary.setUpdatedAt(Instant.now());
        
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        logger.info("Updated itinerary: {} for user: {}", itineraryId, userId);
        
        // Record interaction
        recordUserInteraction(userId, savedItinerary, "modified", null);
        
        return itineraryMapper.toResponse(savedItinerary);
    }

    /**
     * Delete an itinerary
     */
    @Transactional
    public void deleteItinerary(UUID itineraryId, String userId) {
        logger.info("Deleting itinerary: {} for user: {}", itineraryId, userId);
        
        Optional<Itinerary> itinerary = itineraryRepository.findById(itineraryId);
        if (itinerary.isEmpty()) {
            throw new RuntimeException("Itinerary not found with ID: " + itineraryId);
        }
        
        // Check ownership
        if (!itinerary.get().getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: User can only delete their own itineraries");
        }
        
        itineraryRepository.deleteById(itineraryId);
        logger.info("Deleted itinerary: {} for user: {}", itineraryId, userId);
    }

    /**
     * Rate an itinerary
     */
    @Transactional
    public void rateItinerary(UUID itineraryId, String userId, int rating) {
        logger.info("User: {} rating itinerary: {} with rating: {}", userId, itineraryId, rating);
        
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        Optional<Itinerary> itinerary = itineraryRepository.findById(itineraryId);
        if (itinerary.isEmpty()) {
            throw new RuntimeException("Itinerary not found with ID: " + itineraryId);
        }
        
        // Record rating interaction
        recordUserInteraction(userId, itinerary.get(), "rated", rating);
        
        // Update user preferences based on rating
        if (rating >= 4) {
            recommendationService.updateUserPreferences(userId, itinerary.get(), "rated");
        }
    }

    /**
     * Get AI-powered destination suggestions
     */
    public String getNextDestinationSuggestion(String userId, String currentDestination) {
        logger.info("Getting AI destination suggestion for user: {}, current: {}", userId, currentDestination);
        
        try {
            // Get user's travel history
            List<String> previousDestinations = getUserTravelHistory(userId);
            
            // Determine travel style from user preferences
            String travelStyle = getUserTravelStyle(userId);
            
            // Get average budget from user's itineraries
            Integer averageBudget = getUserAverageBudget(userId);
            
            // Get average trip duration
            Integer averageDays = getUserAverageDays(userId);
            
            return aiServiceClient.getNextDestinationSuggestion(
                    currentDestination, previousDestinations, travelStyle, averageBudget, averageDays);
            
        } catch (Exception e) {
            logger.error("Error getting AI destination suggestion for user: {}", userId, e);
            return "Unable to get destination suggestion at this time. Please try again later.";
        }
    }

    /**
     * Record user interaction with an itinerary (public method for controller)
     */
    @Transactional
    public void recordItineraryInteraction(String userId, UUID itineraryId, Integer rating, String interactionType) {
        logger.info("Recording interaction for user: {}, itinerary: {}, type: {}, rating: {}", 
                userId, itineraryId, interactionType, rating);
        
        try {
            // Verify the itinerary exists and belongs to the user
            Optional<Itinerary> itineraryOpt = itineraryRepository.findById(itineraryId);
            if (itineraryOpt.isEmpty()) {
                throw new IllegalArgumentException("Itinerary not found: " + itineraryId);
            }
            
            Itinerary itinerary = itineraryOpt.get();
            if (!itinerary.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Access denied: Itinerary does not belong to the user");
            }
            
            // Record the interaction
            recordUserInteraction(userId, itinerary, interactionType, rating);
            
        } catch (Exception e) {
            logger.error("Failed to record itinerary interaction: user={}, itinerary={}, type={}, rating={}", 
                    userId, itineraryId, interactionType, rating, e);
            throw e;
        }
    }

    /**
     * Get travel tips for a destination
     */
    public String getTravelTips(String destination, String userId) {
        logger.info("Getting travel tips for destination: {} for user: {}", destination, userId);
        
        try {
            String travelStyle = getUserTravelStyle(userId);
            Integer averageBudget = getUserAverageBudget(userId);
            
            return aiServiceClient.getTravelTips(destination, travelStyle, averageBudget);
            
        } catch (Exception e) {
            logger.error("Error getting travel tips for destination: {} for user: {}", destination, userId, e);
            return "Travel tips temporarily unavailable. Please try again later.";
        }
    }

    // Private helper methods

    private void validateTripPlanRequest(TripPlanRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (request.getDestinations() == null || request.getDestinations().isEmpty()) {
            throw new IllegalArgumentException("At least one destination is required");
        }
        if (request.getDays() == null || request.getDays() <= 0) {
            throw new IllegalArgumentException("Days must be a positive number");
        }
        if (request.getBudget() == null || request.getBudget() <= 0) {
            throw new IllegalArgumentException("Budget must be a positive number");
        }
    }

    private Itinerary createBaseItinerary(TripPlanRequest request, String userId) {
        Itinerary itinerary = new Itinerary();
        itinerary.setUserId(userId);
        itinerary.setTitle(request.getTitle());
        itinerary.setDays(request.getDays());
        itinerary.setBudget(request.getBudget());
        
        // Convert origin and destinations to JSON strings
        try {
            if (request.getOrigin() != null) {
                // Wrap single origin in a List to match ItineraryResponse expectations
                List<Map<String, Object>> originsList = List.of(request.getOrigin());
                itinerary.setOrigins(objectMapper.writeValueAsString(originsList));
            }
            if (request.getDestinations() != null) {
                itinerary.setDestinations(objectMapper.writeValueAsString(request.getDestinations()));
            }
        } catch (Exception e) {
            logger.error("Error serializing origin/destinations to JSON", e);
        }
        
        itinerary.setCreatedAt(Instant.now());
        itinerary.setUpdatedAt(Instant.now());
        return itinerary;
    }

    private void updateItineraryFields(Itinerary itinerary, TripPlanRequest request) {
        if (request.getTitle() != null) {
            itinerary.setTitle(request.getTitle());
        }
        if (request.getDays() != null) {
            itinerary.setDays(request.getDays());
        }
        if (request.getBudget() != null) {
            itinerary.setBudget(request.getBudget());
        }
        
        // Update origin and destinations as JSON strings
        try {
            if (request.getOrigin() != null) {
                // Wrap single origin in a List to match ItineraryResponse expectations
                List<Map<String, Object>> originsList = List.of(request.getOrigin());
                itinerary.setOrigins(objectMapper.writeValueAsString(originsList));
            }
            if (request.getDestinations() != null) {
                itinerary.setDestinations(objectMapper.writeValueAsString(request.getDestinations()));
            }
        } catch (Exception e) {
            logger.error("Error serializing origin/destinations to JSON", e);
        }
        // Note: Description and public fields not available in current TripPlanRequest
        // These would need to be added to TripPlanRequest if needed
    }

    private String getAIDestinationSuggestion(TripPlanRequest request, String userId) {
        try {
            List<String> previousDestinations = getUserTravelHistory(userId);
            String travelStyle = getUserTravelStyle(userId);
            
            // Get first destination from the list
            String destination = request.getDestinations() != null && !request.getDestinations().isEmpty() 
                ? request.getDestinations().get(0).toString() : "";
            
            return aiServiceClient.getNextDestinationSuggestion(
                    destination, previousDestinations, travelStyle, 
                    request.getBudget(), request.getDays());
        } catch (Exception e) {
            logger.warn("Failed to get AI destination suggestion for user: {}", userId, e);
            return null;
        }
    }

    private void recordUserInteraction(String userId, Itinerary itinerary, String interactionType, Integer rating) {
        try {
            recommendationService.recordInteraction(userId, itinerary.getId(), interactionType, rating);
        } catch (Exception e) {
            logger.warn("Failed to record user interaction: user={}, itinerary={}, type={}", 
                    userId, itinerary.getId(), interactionType, e);
        }
    }

    private List<String> getUserTravelHistory(String userId) {
        try {
            return itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(10))
                    .getContent().stream()
                    .map(Itinerary::getDestinations)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to get travel history for user: {}", userId, e);
            return new ArrayList<>();
        }
    }

    private String getUserTravelStyle(String userId) {
        try {
            // Simple heuristic based on user's budget patterns
            Integer avgBudget = getUserAverageBudget(userId);
            if (avgBudget == null) return "moderate";
            
            if (avgBudget < 500) return "budget";
            if (avgBudget < 1500) return "moderate";
            if (avgBudget < 3000) return "comfortable";
            return "luxury";
        } catch (Exception e) {
            logger.warn("Failed to determine travel style for user: {}", userId, e);
            return "moderate";
        }
    }

    private Integer getUserAverageBudget(String userId) {
        try {
            List<Itinerary> recentItineraries = itineraryRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(5))
                    .getContent();
            
            if (recentItineraries.isEmpty()) {
                return null;
            }
            
            return (int) recentItineraries.stream()
                    .mapToInt(Itinerary::getBudget)
                    .average()
                    .orElse(1000.0);
        } catch (Exception e) {
            logger.warn("Failed to calculate average budget for user: {}", userId, e);
            return 1000;
        }
    }

    private Integer getUserAverageDays(String userId) {
        try {
            List<Itinerary> recentItineraries = itineraryRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(5))
                    .getContent();
            
            if (recentItineraries.isEmpty()) {
                return null;
            }
            
            return (int) recentItineraries.stream()
                    .mapToInt(Itinerary::getDays)
                    .average()
                    .orElse(7.0);
        } catch (Exception e) {
            logger.warn("Failed to calculate average days for user: {}", userId, e);
            return 7;
        }
    }
}