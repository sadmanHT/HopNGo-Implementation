package com.hopngo.market.service;

import com.hopngo.market.entity.Listing;
import com.hopngo.market.repository.ListingRepository;
import com.hopngo.market.dto.FlagContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ListingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ListingService.class);
    
    private final ListingRepository listingRepository;
    private final EventPublisher eventPublisher;
    private final AuthIntegrationService authIntegrationService;
    
    public ListingService(ListingRepository listingRepository, EventPublisher eventPublisher, 
                         AuthIntegrationService authIntegrationService) {
        this.listingRepository = listingRepository;
        this.eventPublisher = eventPublisher;
        this.authIntegrationService = authIntegrationService;
    }
    
    public Listing createListing(Listing listing) {
        // Verify that the user is a verified provider
        String userId = listing.getUserId();
        if (!authIntegrationService.isVerifiedProvider(userId)) {
            logger.warn("User {} attempted to create listing but is not a verified provider", userId);
            throw new IllegalStateException("Only verified providers can create listings. Please complete the verification process first.");
        }
        
        listing.setVisibility(Listing.Visibility.PUBLIC);
        listing.setStatus(Listing.Status.ACTIVE);
        Listing savedListing = listingRepository.save(listing);
        logger.info("Listing created: {} by verified provider {}", savedListing.getId(), savedListing.getUserId());
        return savedListing;
    }
    
    public Optional<Listing> getListingById(String listingId, String currentUserId) {
        Optional<Listing> listingOpt = listingRepository.findById(listingId);
        if (listingOpt.isPresent()) {
            Listing listing = listingOpt.get();
            if (canViewListing(listing, currentUserId)) {
                return Optional.of(listing);
            }
        }
        return Optional.empty();
    }
    
    public Page<Listing> getActivePublicListings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository.findActivePublicListings(pageable);
    }
    
    public Page<Listing> getListingsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository.findByCategoryAndActiveAndVisibilityPublic(category, pageable);
    }
    
    public Page<Listing> getListingsByUserId(String userId, int page, int size, String currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        
        // If viewing own listings or admin, show all listings; otherwise only public listings
        if (userId.equals(currentUserId) || isAdmin(currentUserId)) {
            return listingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            return listingRepository.findByUserIdAndVisibilityPublic(userId, pageable);
        }
    }
    
    public Page<Listing> searchListings(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository.searchActivePublicListings(query, pageable);
    }
    
    public Page<Listing> getListingsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository.findByPriceRangeAndActiveAndVisibilityPublic(minPrice, maxPrice, pageable);
    }
    
    public Page<Listing> getListingsByTags(List<String> tags, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository.findByTagsInAndActiveAndVisibilityPublic(tags, pageable);
    }
    
    public Page<Listing> getListingsInBoundingBox(double minLat, double maxLat, double minLng, double maxLng, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return listingRepository.findListingsInBoundingBoxAndActiveAndVisibilityPublic(minLat, maxLat, minLng, maxLng, pageable);
    }
    
    public void flagListing(String listingId, String reporterId, FlagContentRequest request) {
        Optional<Listing> listingOpt = listingRepository.findById(listingId);
        if (listingOpt.isPresent()) {
            Listing listing = listingOpt.get();
            
            String reason = request.getReason();
            if (request.getDetails() != null && !request.getDetails().trim().isEmpty()) {
                reason += ": " + request.getDetails();
            }
            
            eventPublisher.publishContentFlaggedEvent(
                "LISTING", listingId, reporterId, reason
            );
            
            logger.info("Listing flagged: {} by user {} for reason: {}", listingId, reporterId, request.getReason());
        } else {
            throw new RuntimeException("Listing not found: " + listingId);
        }
    }
    
    public void updateListingVisibility(String listingId, Listing.Visibility visibility, String reason) {
        Optional<Listing> listingOpt = listingRepository.findById(listingId);
        if (listingOpt.isPresent()) {
            Listing listing = listingOpt.get();
            Listing.Visibility oldVisibility = listing.getVisibility();
            listing.setVisibility(visibility);
            listingRepository.save(listing);
            
            // Publish moderation event
            String action = visibility == Listing.Visibility.PUBLIC ? "APPROVED" : 
                           visibility == Listing.Visibility.REMOVED ? "REMOVED" : "PENDING";
            
            eventPublisher.publishContentModerationEvent(
                "LISTING", listingId, action, reason
            );
            
            logger.info("Listing visibility updated: {} from {} to {} - {}", 
                       listingId, oldVisibility, visibility, reason);
        } else {
            throw new RuntimeException("Listing not found: " + listingId);
        }
    }
    
    public void updateListingStatus(String listingId, Listing.Status status, String userId) {
        Optional<Listing> listingOpt = listingRepository.findById(listingId);
        if (listingOpt.isPresent()) {
            Listing listing = listingOpt.get();
            
            // Only owner or admin can update status
            if (!listing.getUserId().equals(userId) && !isAdmin(userId)) {
                throw new RuntimeException("Not authorized to update listing status");
            }
            
            Listing.Status oldStatus = listing.getStatus();
            listing.setStatus(status);
            listingRepository.save(listing);
            
            logger.info("Listing status updated: {} from {} to {} by user {}", 
                       listingId, oldStatus, status, userId);
        } else {
            throw new RuntimeException("Listing not found: " + listingId);
        }
    }
    
    private boolean canViewListing(Listing listing, String currentUserId) {
        // Public listings can be viewed by anyone
        if (listing.getVisibility() == Listing.Visibility.PUBLIC && listing.getStatus() == Listing.Status.ACTIVE) {
            return true;
        }
        
        // Owner can always view their own listings
        if (listing.getUserId().equals(currentUserId)) {
            return true;
        }
        
        // Admin can view all listings
        if (isAdmin(currentUserId)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isAdmin(String userId) {
        // TODO: Implement admin check - for now return false
        // This should check user roles from auth service or JWT claims
        return false;
    }
}