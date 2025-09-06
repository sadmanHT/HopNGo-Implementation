package com.hopngo.market.controller;

import com.hopngo.market.dto.FlagContentRequest;
import com.hopngo.market.service.ReviewService;
import com.hopngo.market.service.ListingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/market")
public class MarketController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);
    
    private final ReviewService reviewService;
    private final ListingService listingService;
    
    public MarketController(ReviewService reviewService, ListingService listingService) {
        this.reviewService = reviewService;
        this.listingService = listingService;
    }
    
    @PostMapping("/reviews/{reviewId}/flag")
    public ResponseEntity<?> flagReview(
            @PathVariable String reviewId,
            @Valid @RequestBody FlagContentRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        try {
            reviewService.flagReview(reviewId, userId, request);
            
            logger.info("Review flagged successfully: {} by user {}", reviewId, userId);
            return ResponseEntity.ok().body("Review flagged successfully");
            
        } catch (RuntimeException e) {
            logger.error("Error flagging review {}: {}", reviewId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error flagging review {}", reviewId, e);
            return ResponseEntity.internalServerError().body("An error occurred while flagging the review");
        }
    }
    
    @PostMapping("/listings/{listingId}/flag")
    public ResponseEntity<?> flagListing(
            @PathVariable String listingId,
            @Valid @RequestBody FlagContentRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        try {
            listingService.flagListing(listingId, userId, request);
            
            logger.info("Listing flagged successfully: {} by user {}", listingId, userId);
            return ResponseEntity.ok().body("Listing flagged successfully");
            
        } catch (RuntimeException e) {
            logger.error("Error flagging listing {}: {}", listingId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error flagging listing {}", listingId, e);
            return ResponseEntity.internalServerError().body("An error occurred while flagging the listing");
        }
    }
    
    // Admin endpoints for moderation
    @PutMapping("/reviews/{reviewId}/visibility")
    public ResponseEntity<?> updateReviewVisibility(
            @PathVariable String reviewId,
            @RequestParam String visibility,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-User-ID") String userId) {
        
        try {
            // TODO: Add admin authorization check
            reviewService.updateReviewVisibility(reviewId, 
                com.hopngo.market.entity.Review.Visibility.valueOf(visibility.toUpperCase()), 
                reason != null ? reason : "Admin action");
            
            logger.info("Review visibility updated: {} to {} by admin {}", reviewId, visibility, userId);
            return ResponseEntity.ok().body("Review visibility updated successfully");
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid visibility value: {}", visibility);
            return ResponseEntity.badRequest().body("Invalid visibility value");
        } catch (RuntimeException e) {
            logger.error("Error updating review visibility {}: {}", reviewId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating review visibility {}", reviewId, e);
            return ResponseEntity.internalServerError().body("An error occurred while updating review visibility");
        }
    }
    
    @PutMapping("/listings/{listingId}/visibility")
    public ResponseEntity<?> updateListingVisibility(
            @PathVariable String listingId,
            @RequestParam String visibility,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-User-ID") String userId) {
        
        try {
            // TODO: Add admin authorization check
            listingService.updateListingVisibility(listingId, 
                com.hopngo.market.entity.Listing.Visibility.valueOf(visibility.toUpperCase()), 
                reason != null ? reason : "Admin action");
            
            logger.info("Listing visibility updated: {} to {} by admin {}", listingId, visibility, userId);
            return ResponseEntity.ok().body("Listing visibility updated successfully");
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid visibility value: {}", visibility);
            return ResponseEntity.badRequest().body("Invalid visibility value");
        } catch (RuntimeException e) {
            logger.error("Error updating listing visibility {}: {}", listingId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating listing visibility {}", listingId, e);
            return ResponseEntity.internalServerError().body("An error occurred while updating listing visibility");
        }
    }
}