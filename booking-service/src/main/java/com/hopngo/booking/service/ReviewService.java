package com.hopngo.booking.service;

import com.hopngo.booking.entity.Review;
import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.Listing;
import com.hopngo.booking.entity.Vendor;
import com.hopngo.booking.repository.ReviewRepository;
import com.hopngo.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final OutboxService outboxService;
    
    @Autowired
    public ReviewService(ReviewRepository reviewRepository,
                        BookingRepository bookingRepository,
                        BookingService bookingService,
                        OutboxService outboxService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.outboxService = outboxService;
    }
    
    public Review createReview(UUID bookingId, String userId, Integer rating, 
                              String title, String comment) {
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        // Find booking
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // Validate that user can review this booking
        if (!bookingService.canUserReviewBooking(bookingId, userId)) {
            throw new IllegalArgumentException("User cannot review this booking");
        }
        
        // Check if review already exists
        Optional<Review> existingReview = reviewRepository.findByBookingId(bookingId);
        if (existingReview.isPresent()) {
            throw new IllegalArgumentException("Review already exists for this booking");
        }
        
        // Create review
        Review review = new Review();
        review.setBooking(booking);
        review.setUserId(userId);
        review.setListing(booking.getListing());
        review.setVendor(booking.getVendor());
        review.setRating(rating);
        review.setTitle(title);
        review.setComment(comment);
        
        Review savedReview = reviewRepository.save(review);
        
        // Publish review created event
        outboxService.publishReviewCreatedEvent(savedReview);
        
        return savedReview;
    }
    
    @Transactional(readOnly = true)
    public Optional<Review> findById(UUID reviewId) {
        return reviewRepository.findById(reviewId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Review> findByBookingId(UUID bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findByUserId(String userId) {
        return reviewRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findByListingId(UUID listingId) {
        return reviewRepository.findByListingId(listingId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findByVendorId(UUID vendorId) {
        return reviewRepository.findByVendorId(vendorId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findByListingIdWithComments(UUID listingId) {
        return reviewRepository.findByListingIdWithComments(listingId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findByVendorIdWithComments(UUID vendorId) {
        return reviewRepository.findByVendorIdWithComments(vendorId);
    }
    
    @Transactional(readOnly = true)
    public Double getAverageRatingForListing(UUID listingId) {
        return reviewRepository.getAverageRatingForListing(listingId);
    }
    
    @Transactional(readOnly = true)
    public Double getAverageRatingForVendor(UUID vendorId) {
        return reviewRepository.getAverageRatingForVendor(vendorId);
    }
    
    @Transactional(readOnly = true)
    public long getReviewCountForListing(UUID listingId) {
        return reviewRepository.countByListingId(listingId);
    }
    
    @Transactional(readOnly = true)
    public long getReviewCountForVendor(UUID vendorId) {
        return reviewRepository.countByVendorId(vendorId);
    }
    
    @Transactional(readOnly = true)
    public Map<Integer, Long> getRatingDistributionForListing(UUID listingId) {
        List<Object[]> results = reviewRepository.getRatingDistributionForListing(listingId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (Object[] result : results) {
            Integer rating = (Integer) result[0];
            Long count = (Long) result[1];
            distribution.put(rating, count);
        }
        return distribution;
    }
    
    @Transactional(readOnly = true)
    public Map<Integer, Long> getRatingDistributionForVendor(UUID vendorId) {
        List<Object[]> results = reviewRepository.getRatingDistributionForVendor(vendorId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (Object[] result : results) {
            Integer rating = (Integer) result[0];
            Long count = (Long) result[1];
            distribution.put(rating, count);
        }
        return distribution;
    }
    
    public Review updateReview(UUID reviewId, String userId, Integer rating, 
                              String title, String comment) {
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        
        // Validate ownership
        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this review");
        }
        
        // Validate rating if provided
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        if (rating != null) review.setRating(rating);
        if (title != null) review.setTitle(title);
        if (comment != null) review.setComment(comment);
        
        Review updatedReview = reviewRepository.save(review);
        
        // Publish review updated event
        outboxService.publishReviewUpdatedEvent(updatedReview);
        
        return updatedReview;
    }
    
    public void deleteReview(UUID reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        
        // Validate ownership
        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this review");
        }
        
        reviewRepository.delete(review);
        
        // Publish review deleted event
        outboxService.publishReviewDeletedEvent(review);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findRecentReviewsForListing(UUID listingId, int limit) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId)
            .stream()
            .limit(limit)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<Review> findRecentReviewsForVendor(UUID vendorId, int limit) {
        return reviewRepository.findByVendorIdOrderByCreatedAtDesc(vendorId)
            .stream()
            .limit(limit)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<Review> findHighRatedReviewsForListing(UUID listingId, int minRating) {
        return reviewRepository.findByListingIdAndRatingGreaterThanEqual(listingId, minRating);
    }
    
    @Transactional(readOnly = true)
    public List<Review> findHighRatedReviewsForVendor(UUID vendorId, int minRating) {
        return reviewRepository.findByVendorIdAndRatingGreaterThanEqual(vendorId, minRating);
    }
    
    @Transactional(readOnly = true)
    public boolean hasUserReviewedBooking(UUID bookingId, String userId) {
        return reviewRepository.existsByBookingId(bookingId);
    }
    
    @Transactional(readOnly = true)
    public void validateReviewOwnership(UUID reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        
        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this review");
        }
    }
}