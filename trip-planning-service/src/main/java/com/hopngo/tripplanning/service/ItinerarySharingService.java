package com.hopngo.tripplanning.service;

import com.hopngo.tripplanning.dto.*;
import com.hopngo.tripplanning.entity.*;
import com.hopngo.tripplanning.enums.ShareVisibility;
import com.hopngo.tripplanning.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Transactional
public class ItinerarySharingService {

    private static final Logger logger = LoggerFactory.getLogger(ItinerarySharingService.class);
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    private final ItineraryRepository itineraryRepository;
    private final ItineraryShareRepository shareRepository;
    private final ItineraryVersionRepository versionRepository;
    private final ItineraryCommentRepository commentRepository;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    public ItinerarySharingService(ItineraryRepository itineraryRepository,
                                 ItineraryShareRepository shareRepository,
                                 ItineraryVersionRepository versionRepository,
                                 ItineraryCommentRepository commentRepository) {
        this.itineraryRepository = itineraryRepository;
        this.shareRepository = shareRepository;
        this.versionRepository = versionRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Create or update sharing configuration for an itinerary
     */
    public ShareItineraryResponse shareItinerary(UUID itineraryId, ShareItineraryRequest request, String userId) {
        logger.info("Sharing itinerary: {} for user: {} with visibility: {}", itineraryId, userId, request.getVisibility());
        
        // Verify itinerary exists and belongs to user
        Itinerary itinerary = itineraryRepository.findByIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found or access denied"));
        
        try {
            // Check if sharing already exists
            Optional<ItineraryShare> existingShare = shareRepository.findByItineraryId(itineraryId);
            
            ItineraryShare share;
            if (existingShare.isPresent()) {
                // Update existing share
                share = existingShare.get();
                share.setVisibility(request.getVisibility());
                share.setCanComment(request.getCanComment());
                logger.info("Updated existing share configuration for itinerary: {}", itineraryId);
            } else {
                // Create new share
                String token = generateSecureToken();
                share = new ItineraryShare(itinerary, token, request.getVisibility(), request.getCanComment());
                logger.info("Created new share configuration for itinerary: {} with token: {}", itineraryId, token);
            }
            
            share = shareRepository.save(share);
            
            // Build share URL
            String shareUrl = frontendUrl + "/trips/share/" + share.getToken();
            
            return new ShareItineraryResponse(
                share.getId(),
                share.getToken(),
                shareUrl,
                share.getVisibility(),
                share.getCanComment(),
                share.getCreatedAt()
            );
            
        } catch (Exception e) {
            logger.error("Failed to share itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to share itinerary: " + e.getMessage(), e);
        }
    }

    /**
     * Get shared itinerary by token (public access)
     */
    @Transactional(readOnly = true)
    public Optional<ItineraryResponse> getSharedItinerary(String token) {
        logger.debug("Fetching shared itinerary with token: {}", token);
        
        return shareRepository.findByToken(token)
                .filter(share -> share.getVisibility() != ShareVisibility.PRIVATE)
                .map(share -> {
                    Itinerary itinerary = share.getItinerary();
                    return new ItineraryResponse(
                        itinerary.getId(),
                        itinerary.getUserId(),
                        itinerary.getTitle(),
                        itinerary.getDays(),
                        itinerary.getBudget(),
                        itinerary.getOrigins(),
                        itinerary.getDestinations(),
                        itinerary.getPlan(),
                        itinerary.getCreatedAt(),
                        itinerary.getUpdatedAt()
                    );
                });
    }

    /**
     * Add comment to a shared itinerary
     */
    public CommentResponse addComment(UUID itineraryId, CommentRequest request, String userId) {
        logger.info("Adding comment to itinerary: {} by user: {}", itineraryId, userId);
        
        // Verify itinerary exists
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));
        
        // Check if comments are allowed
        Optional<ItineraryShare> share = shareRepository.findByItineraryId(itineraryId);
        if (share.isEmpty() || !share.get().getCanComment()) {
            throw new RuntimeException("Comments are not allowed on this itinerary");
        }
        
        try {
            ItineraryComment comment = new ItineraryComment(itinerary, userId, request.getMessage());
            comment = commentRepository.save(comment);
            
            logger.info("Successfully added comment: {} to itinerary: {}", comment.getId(), itineraryId);
            
            return new CommentResponse(
                comment.getId(),
                comment.getItinerary().getId(),
                comment.getAuthorUserId(),
                comment.getMessage(),
                comment.getCreatedAt()
            );
            
        } catch (Exception e) {
            logger.error("Failed to add comment to itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to add comment: " + e.getMessage(), e);
        }
    }

    /**
     * Get comments for an itinerary
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID itineraryId) {
        logger.debug("Fetching comments for itinerary: {}", itineraryId);
        
        return commentRepository.findByItineraryIdOrderByCreatedAtAsc(itineraryId)
                .stream()
                .map(comment -> new CommentResponse(
                    comment.getId(),
                    comment.getItinerary().getId(),
                    comment.getAuthorUserId(),
                    comment.getMessage(),
                    comment.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Get version history for an itinerary
     */
    @Transactional(readOnly = true)
    public List<VersionResponse> getVersionHistory(UUID itineraryId, String userId) {
        logger.debug("Fetching version history for itinerary: {} by user: {}", itineraryId, userId);
        
        // Verify user has access to itinerary
        if (!itineraryRepository.existsByIdAndUserId(itineraryId, userId)) {
            throw new RuntimeException("Itinerary not found or access denied");
        }
        
        return versionRepository.findByItineraryIdOrderByVersionDesc(itineraryId)
                .stream()
                .map(version -> new VersionResponse(
                    version.getId(),
                    version.getItinerary().getId(),
                    version.getVersion(),
                    version.getPlan(),
                    version.getAuthorUserId(),
                    version.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Create a version snapshot when itinerary is updated
     */
    public void createVersion(UUID itineraryId, String userId) {
        logger.info("Creating version snapshot for itinerary: {} by user: {}", itineraryId, userId);
        
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));
        
        try {
            // Get next version number
            Integer nextVersion = versionRepository.findMaxVersionByItineraryId(itineraryId)
                    .map(max -> max + 1)
                    .orElse(1);
            
            ItineraryVersion version = new ItineraryVersion(
                itinerary, nextVersion, itinerary.getPlan(), userId
            );
            
            versionRepository.save(version);
            logger.info("Created version {} for itinerary: {}", nextVersion, itineraryId);
            
        } catch (Exception e) {
            logger.error("Failed to create version for itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to create version: " + e.getMessage(), e);
        }
    }

    /**
     * Revert itinerary to a specific version
     */
    public ItineraryResponse revertToVersion(UUID itineraryId, Integer version, String userId) {
        logger.info("Reverting itinerary: {} to version: {} by user: {}", itineraryId, version, userId);
        
        // Verify itinerary belongs to user
        Itinerary itinerary = itineraryRepository.findByIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found or access denied"));
        
        // Find the version to revert to
        ItineraryVersion targetVersion = versionRepository.findByItineraryIdAndVersion(itineraryId, version)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        
        try {
            // Create current version before reverting
            createVersion(itineraryId, userId);
            
            // Update itinerary with version data
            itinerary.setPlan(targetVersion.getPlan());
            itinerary = itineraryRepository.save(itinerary);
            
            logger.info("Successfully reverted itinerary: {} to version: {}", itineraryId, version);
            
            return new ItineraryResponse(
                itinerary.getId(),
                itinerary.getUserId(),
                itinerary.getTitle(),
                itinerary.getDays(),
                itinerary.getBudget(),
                itinerary.getOrigins(),
                itinerary.getDestinations(),
                itinerary.getPlan(),
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt()
            );
            
        } catch (Exception e) {
            logger.error("Failed to revert itinerary: {} to version: {}", itineraryId, version, e);
            throw new RuntimeException("Failed to revert to version: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a secure random token for sharing
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Remove sharing configuration
     */
    public boolean removeSharing(UUID itineraryId, String userId) {
        logger.info("Removing sharing for itinerary: {} by user: {}", itineraryId, userId);
        
        // Verify itinerary belongs to user
        if (!itineraryRepository.existsByIdAndUserId(itineraryId, userId)) {
            throw new RuntimeException("Itinerary not found or access denied");
        }
        
        try {
            shareRepository.deleteByItineraryId(itineraryId);
            logger.info("Successfully removed sharing for itinerary: {}", itineraryId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove sharing for itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to remove sharing: " + e.getMessage(), e);
        }
    }
}