package com.hopngo.market.service;

import com.hopngo.market.entity.Review;
import com.hopngo.market.repository.ReviewRepository;
import com.hopngo.market.dto.FlagContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    
    private final ReviewRepository reviewRepository;
    private final EventPublisher eventPublisher;
    
    public ReviewService(ReviewRepository reviewRepository, EventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public Review createReview(Review review) {
        review.setVisibility(Review.Visibility.PUBLIC);
        Review savedReview = reviewRepository.save(review);
        logger.info("Review created: {} for product {}", savedReview.getId(), savedReview.getProductId());
        return savedReview;
    }
    
    public Optional<Review> getReviewById(String reviewId, String currentUserId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            if (canViewReview(review, currentUserId)) {
                return Optional.of(review);
            }
        }
        return Optional.empty();
    }
    
    public Page<Review> getReviewsByProductId(String productId, int page, int size, String currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        // For product reviews, show only public reviews to all users
        return reviewRepository.findByProductIdAndVisibilityPublic(productId, pageable);
    }
    
    public Page<Review> getReviewsByUserId(String userId, int page, int size, String currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        
        // If viewing own reviews or admin, show all reviews; otherwise only public reviews
        if (userId.equals(currentUserId) || isAdmin(currentUserId)) {
            return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            return reviewRepository.findByUserIdAndVisibilityPublic(userId, pageable);
        }
    }
    
    public Double getAverageRatingForProduct(String productId) {
        return reviewRepository.findAverageRatingByProductId(productId);
    }
    
    public Long getReviewCountForProduct(String productId) {
        return reviewRepository.countByProductIdAndVisibilityPublic(productId);
    }
    
    public void flagReview(String reviewId, String reporterId, FlagContentRequest request) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            
            String reason = request.getReason();
            if (request.getDetails() != null && !request.getDetails().trim().isEmpty()) {
                reason += ": " + request.getDetails();
            }
            
            eventPublisher.publishContentFlaggedEvent(
                "REVIEW", reviewId, reporterId, reason
            );
            
            logger.info("Review flagged: {} by user {} for reason: {}", reviewId, reporterId, request.getReason());
        } else {
            throw new RuntimeException("Review not found: " + reviewId);
        }
    }
    
    public void updateReviewVisibility(String reviewId, Review.Visibility visibility, String reason) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            Review review = reviewOpt.get();
            Review.Visibility oldVisibility = review.getVisibility();
            review.setVisibility(visibility);
            reviewRepository.save(review);
            
            // Publish moderation event
            String action = visibility == Review.Visibility.PUBLIC ? "APPROVED" : 
                           visibility == Review.Visibility.REMOVED ? "REMOVED" : "PENDING";
            
            eventPublisher.publishContentModerationEvent(
                "REVIEW", reviewId, action, reason
            );
            
            logger.info("Review visibility updated: {} from {} to {} - {}", 
                       reviewId, oldVisibility, visibility, reason);
        } else {
            throw new RuntimeException("Review not found: " + reviewId);
        }
    }
    
    private boolean canViewReview(Review review, String currentUserId) {
        // Public reviews can be viewed by anyone
        if (review.getVisibility() == Review.Visibility.PUBLIC) {
            return true;
        }
        
        // Owner can always view their own reviews
        if (review.getUserId().equals(currentUserId)) {
            return true;
        }
        
        // Admin can view all reviews
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