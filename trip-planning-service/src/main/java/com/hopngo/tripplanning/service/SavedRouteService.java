package com.hopngo.tripplanning.service;

import com.hopngo.tripplanning.dto.CreateSavedRouteRequest;
import com.hopngo.tripplanning.dto.SavedRouteResponse;
import com.hopngo.tripplanning.dto.UpdateSavedRouteRequest;
import com.hopngo.tripplanning.entity.SavedRoute;
import com.hopngo.tripplanning.mapper.SavedRouteMapper;
import com.hopngo.tripplanning.repository.SavedRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SavedRouteService {

    private static final Logger logger = LoggerFactory.getLogger(SavedRouteService.class);

    private final SavedRouteRepository savedRouteRepository;
    private final SavedRouteMapper savedRouteMapper;

    @Autowired
    public SavedRouteService(SavedRouteRepository savedRouteRepository, SavedRouteMapper savedRouteMapper) {
        this.savedRouteRepository = savedRouteRepository;
        this.savedRouteMapper = savedRouteMapper;
    }

    /**
     * Create a new saved route
     */
    public SavedRouteResponse createSavedRoute(CreateSavedRouteRequest request, String userId) {
        logger.info("Creating saved route for user: {}, name: {}", userId, request.getName());
        
        try {
            SavedRoute savedRoute = savedRouteMapper.toEntity(request, userId);
            SavedRoute saved = savedRouteRepository.save(savedRoute);
            
            logger.info("Successfully created saved route with ID: {}", saved.getId());
            return savedRouteMapper.toResponse(saved);
        } catch (Exception e) {
            logger.error("Failed to create saved route for user: {}", userId, e);
            throw new RuntimeException("Failed to create saved route: " + e.getMessage(), e);
        }
    }

    /**
     * Get saved route by ID with user validation
     */
    @Transactional(readOnly = true)
    public Optional<SavedRouteResponse> getSavedRoute(UUID id, String userId) {
        logger.debug("Fetching saved route: {} for user: {}", id, userId);
        
        return savedRouteRepository.findByIdAndUserId(id, userId)
                .map(savedRouteMapper::toResponse);
    }

    /**
     * Get all saved routes for a user with pagination
     */
    @Transactional(readOnly = true)
    public Page<SavedRouteResponse> getSavedRoutes(String userId, Pageable pageable) {
        logger.debug("Fetching saved routes for user: {} with pagination: {}", userId, pageable);
        
        return savedRouteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(savedRouteMapper::toResponse);
    }

    /**
     * Search saved routes by name
     */
    @Transactional(readOnly = true)
    public Page<SavedRouteResponse> searchSavedRoutesByName(String userId, String name, Pageable pageable) {
        logger.debug("Searching saved routes for user: {} with name containing: {}", userId, name);
        
        return savedRouteRepository.findByUserIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(userId, name, pageable)
                .map(savedRouteMapper::toResponse);
    }

    /**
     * Filter saved routes by transportation mode
     */
    @Transactional(readOnly = true)
    public Page<SavedRouteResponse> getSavedRoutesByMode(String userId, String mode, Pageable pageable) {
        logger.debug("Fetching saved routes for user: {} with mode: {}", userId, mode);
        
        return savedRouteRepository.findByUserIdAndModeOrderByCreatedAtDesc(userId, mode, pageable)
                .map(savedRouteMapper::toResponse);
    }

    /**
     * Filter saved routes by distance range
     */
    @Transactional(readOnly = true)
    public Page<SavedRouteResponse> getSavedRoutesByDistanceRange(String userId, BigDecimal minDistance, 
                                                                 BigDecimal maxDistance, Pageable pageable) {
        logger.debug("Fetching saved routes for user: {} with distance range: {} - {}", 
                    userId, minDistance, maxDistance);
        
        return savedRouteRepository.findByUserIdAndDistanceKmBetweenOrderByDistanceKmAsc(
                userId, minDistance, maxDistance, pageable)
                .map(savedRouteMapper::toResponse);
    }

    /**
     * Filter saved routes by duration range
     */
    @Transactional(readOnly = true)
    public Page<SavedRouteResponse> getSavedRoutesByDurationRange(String userId, Integer minDuration, 
                                                                 Integer maxDuration, Pageable pageable) {
        logger.debug("Fetching saved routes for user: {} with duration range: {} - {} minutes", 
                    userId, minDuration, maxDuration);
        
        return savedRouteRepository.findByUserIdAndDurationMinBetweenOrderByDurationMinAsc(
                userId, minDuration, maxDuration, pageable)
                .map(savedRouteMapper::toResponse);
    }

    /**
     * Update an existing saved route
     */
    public Optional<SavedRouteResponse> updateSavedRoute(UUID id, UpdateSavedRouteRequest request, String userId) {
        logger.info("Updating saved route: {} for user: {}", id, userId);
        
        return savedRouteRepository.findByIdAndUserId(id, userId)
                .map(existingRoute -> {
                    try {
                        savedRouteMapper.updateEntity(existingRoute, request);
                        SavedRoute updated = savedRouteRepository.save(existingRoute);
                        
                        logger.info("Successfully updated saved route: {}", id);
                        return savedRouteMapper.toResponse(updated);
                    } catch (Exception e) {
                        logger.error("Failed to update saved route: {}", id, e);
                        throw new RuntimeException("Failed to update saved route: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * Delete a saved route
     */
    public boolean deleteSavedRoute(UUID id, String userId) {
        logger.info("Deleting saved route: {} for user: {}", id, userId);
        
        Optional<SavedRoute> savedRoute = savedRouteRepository.findByIdAndUserId(id, userId);
        if (savedRoute.isPresent()) {
            try {
                savedRouteRepository.delete(savedRoute.get());
                logger.info("Successfully deleted saved route: {}", id);
                return true;
            } catch (Exception e) {
                logger.error("Failed to delete saved route: {}", id, e);
                throw new RuntimeException("Failed to delete saved route: " + e.getMessage(), e);
            }
        } else {
            logger.warn("Saved route not found or access denied: {} for user: {}", id, userId);
            return false;
        }
    }

    /**
     * Get count of saved routes for a user
     */
    @Transactional(readOnly = true)
    public long countSavedRoutes(String userId) {
        return savedRouteRepository.countByUserId(userId);
    }

    /**
     * Check if a saved route exists and belongs to the user
     */
    @Transactional(readOnly = true)
    public boolean existsSavedRoute(UUID id, String userId) {
        return savedRouteRepository.existsByIdAndUserId(id, userId);
    }
}