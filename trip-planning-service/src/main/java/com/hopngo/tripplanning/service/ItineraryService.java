package com.hopngo.tripplanning.service;

import com.hopngo.tripplanning.dto.CreateItineraryRequest;
import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.dto.ItineraryUpdateRequest;
import com.hopngo.tripplanning.dto.TripPlanRequest;
import com.hopngo.tripplanning.entity.Itinerary;
import com.hopngo.tripplanning.mapper.ItineraryMapper;
import com.hopngo.tripplanning.repository.ItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
@Transactional
public class ItineraryService {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);

    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;
    private final AIService aiService;

    public ItineraryService(ItineraryRepository itineraryRepository,
                           ItineraryMapper itineraryMapper,
                           AIService aiService) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryMapper = itineraryMapper;
        this.aiService = aiService;
    }

    /**
     * Create a new trip plan by generating itinerary via AI service
     */
    public ItineraryResponse createTripPlan(CreateItineraryRequest request, String userId) {
        logger.info("Creating trip plan for user: {} with title: {}", userId, request.getTitle());
        
        // Validate business rules
        validateTripPlanRequest(request);
        
        // Generate itinerary using AI service (if plan is not provided)
        if (request.getPlan() == null) {
            TripPlanRequest tripPlanRequest = convertToTripPlanRequest(request);
            Map<String, Object> generatedPlan = aiService.generateItinerary(tripPlanRequest);
            request.setPlan(generatedPlan);
        }
        
        // Convert request to entity
        Itinerary itinerary = itineraryMapper.toEntity(request, UUID.fromString(userId));
        
        // Save to database
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        
        logger.info("Successfully created itinerary with ID: {} for user: {}", 
                   savedItinerary.getId(), userId);
        
        return itineraryMapper.toResponse(savedItinerary);
    }

    /**
     * Get itinerary by ID for specific user
     */
    @Transactional(readOnly = true)
    public Optional<ItineraryResponse> getItinerary(UUID id, String userId) {
        logger.debug("Fetching itinerary {} for user: {}", id, userId);
        
        return itineraryRepository.findByIdAndUserId(id, userId)
                .map(itineraryMapper::toResponse);
    }

    /**
     * Update existing itinerary
     */
    public Optional<ItineraryResponse> updateItinerary(UUID id, ItineraryUpdateRequest request, String userId) {
        logger.info("Updating itinerary {} for user: {}", id, userId);
        
        // Validate update request
        validateItineraryUpdateRequest(request);
        
        Optional<Itinerary> existingItinerary = itineraryRepository.findByIdAndUserId(id, userId);
        
        if (existingItinerary.isEmpty()) {
            logger.warn("Itinerary {} not found for user: {}", id, userId);
            return Optional.empty();
        }
        
        Itinerary itinerary = existingItinerary.get();
        
        // Apply partial updates manually since we removed the updateEntityFromRequest method
        if (request.getTitle() != null) {
            itinerary.setTitle(request.getTitle());
        }
        if (request.getDays() != null) {
            itinerary.setDays(request.getDays());
        }
        if (request.getBudget() != null) {
            itinerary.setBudget(request.getBudget());
        }
        
        // Save updated entity
        Itinerary updatedItinerary = itineraryRepository.save(itinerary);
        
        logger.info("Successfully updated itinerary {} for user: {}", id, userId);
        
        return Optional.of(itineraryMapper.toResponse(updatedItinerary));
    }

    /**
     * Get user's itineraries with pagination
     */
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> getUserItineraries(String userId, Pageable pageable) {
        logger.debug("Fetching itineraries for user: {} with pagination: {}", userId, pageable);
        
        Page<Itinerary> itineraries = itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return itineraries.map(itineraryMapper::toResponse);
    }

    /**
     * Delete itinerary by ID for specific user
     */
    public boolean deleteItinerary(UUID id, String userId) {
        logger.info("Deleting itinerary {} for user: {}", id, userId);
        
        Optional<Itinerary> existingItinerary = itineraryRepository.findByIdAndUserId(id, userId);
        
        if (existingItinerary.isEmpty()) {
            logger.warn("Itinerary {} not found for user: {}", id, userId);
            return false;
        }
        
        itineraryRepository.delete(existingItinerary.get());
        
        logger.info("Successfully deleted itinerary {} for user: {}", id, userId);
        
        return true;
    }

    /**
     * Check if user owns the itinerary
     */
    @Transactional(readOnly = true)
    public boolean isOwner(UUID id, String userId) {
        return itineraryRepository.existsByIdAndUserId(id, userId);
    }

    /**
     * Get user's itinerary count
     */
    @Transactional(readOnly = true)
    public long getUserItineraryCount(String userId) {
        return itineraryRepository.countByUserId(userId);
    }

    /**
     * Search itineraries by criteria
     */
    @Transactional(readOnly = true)
    public Page<ItineraryResponse> searchItineraries(String userId, String title, 
                                                   Integer minBudget, Integer maxBudget,
                                                   Integer minDays, Integer maxDays,
                                                   Pageable pageable) {
        logger.debug("Searching itineraries for user: {} with criteria - title: {}, budget: {}-{}, days: {}-{}", 
                    userId, title, minBudget, maxBudget, minDays, maxDays);
        
        Page<Itinerary> itineraries = itineraryRepository.findByUserIdAndCriteria(
            userId, title, minBudget, maxBudget, minDays, maxDays, pageable);
        
        return itineraries.map(itineraryMapper::toResponse);
    }

    /**
     * Validate trip plan request business rules
     */
    private TripPlanRequest convertToTripPlanRequest(CreateItineraryRequest request) {
        TripPlanRequest tripPlanRequest = new TripPlanRequest();
        tripPlanRequest.setTitle(request.getTitle());
        tripPlanRequest.setDays(request.getDays());
        tripPlanRequest.setBudget(request.getBudget());
        
        // Convert origins list to single origin (take first one)
        if (request.getOrigins() != null && !request.getOrigins().isEmpty()) {
            tripPlanRequest.setOrigin(request.getOrigins().get(0));
        }
        
        tripPlanRequest.setDestinations(request.getDestinations());
        
        return tripPlanRequest;
    }

    private void validateTripPlanRequest(CreateItineraryRequest request) {
        if (request.getBudget() != null && request.getBudget() <= 0) {
            throw new IllegalArgumentException("Budget must be greater than 0");
        }
        
        if (request.getDays() != null && (request.getDays() < 1 || request.getDays() > 30)) {
            throw new IllegalArgumentException("Days must be between 1 and 30");
        }
        
        if (request.getDestinations() == null || request.getDestinations().isEmpty()) {
            throw new IllegalArgumentException("At least one destination is required");
        }
        
        if (request.getDestinations().size() > 10) {
            throw new IllegalArgumentException("Maximum 10 destinations allowed");
        }
    }

    /**
     * Validate itinerary update request business rules
     */
    private void validateItineraryUpdateRequest(ItineraryUpdateRequest request) {
        if (request.getBudget() != null && request.getBudget() <= 0) {
            throw new IllegalArgumentException("Budget must be greater than 0");
        }
        
        if (request.getDays() != null && (request.getDays() < 1 || request.getDays() > 30)) {
            throw new IllegalArgumentException("Days must be between 1 and 30");
        }
        
        if (request.getDestinations() != null) {
            if (request.getDestinations().isEmpty()) {
                throw new IllegalArgumentException("At least one destination is required");
            }
            
            if (request.getDestinations().size() > 10) {
                throw new IllegalArgumentException("Maximum 10 destinations allowed");
            }
        }
    }
}