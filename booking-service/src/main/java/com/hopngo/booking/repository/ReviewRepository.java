package com.hopngo.booking.repository;

import com.hopngo.booking.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    Optional<Review> findByBookingId(UUID bookingId);
    
    List<Review> findByUserId(String userId);
    
    List<Review> findByListingId(UUID listingId);
    
    List<Review> findByVendorId(UUID vendorId);
    
    @Query("SELECT r FROM Review r WHERE r.vendor.id = :vendorId AND r.rating >= :minRating")
    List<Review> findByVendorIdAndRatingGreaterThanEqual(@Param("vendorId") UUID vendorId, @Param("minRating") int minRating);
    
    @Query("SELECT r FROM Review r WHERE r.listing.id = :listingId AND r.rating >= :minRating")
    List<Review> findByListingIdAndRatingGreaterThanEqual(@Param("listingId") UUID listingId, @Param("minRating") int minRating);
    
    List<Review> findByListingIdOrderByCreatedAtDesc(UUID listingId);
    
    @Query("SELECT r FROM Review r WHERE r.listing.id = :listingId AND r.comment IS NOT NULL AND r.comment != ''")
    List<Review> findByListingIdWithComments(@Param("listingId") UUID listingId);
    
    @Query("SELECT r FROM Review r WHERE r.vendor.id = :vendorId AND r.comment IS NOT NULL AND r.comment != ''")
    List<Review> findByVendorIdWithComments(@Param("vendorId") UUID vendorId);
    
    List<Review> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.listing.id = :listingId")
    Double getAverageRatingForListing(@Param("listingId") UUID listingId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.vendor.id = :vendorId")
    Double getAverageRatingForVendor(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.listing.id = :listingId")
    long countByListingId(@Param("listingId") UUID listingId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.vendor.id = :vendorId")
    long countByVendorId(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.listing.id = :listingId " +
           "GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionForListing(@Param("listingId") UUID listingId);
    
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.vendor.id = :vendorId " +
           "GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionForVendor(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT r FROM Review r WHERE r.listing.id = :listingId AND r.rating >= :minRating " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByListingIdAndMinRating(
        @Param("listingId") UUID listingId,
        @Param("minRating") Integer minRating
    );
    
    @Query("SELECT r FROM Review r WHERE r.vendor.id = :vendorId AND r.rating >= :minRating " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByVendorIdAndMinRating(
        @Param("vendorId") UUID vendorId,
        @Param("minRating") Integer minRating
    );
    
    @Query("SELECT r FROM Review r WHERE r.userId = :userId " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT r FROM Review r WHERE r.listing.id IN :listingIds " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByListingIds(@Param("listingIds") List<UUID> listingIds);
    
    @Query("SELECT r FROM Review r WHERE r.comment IS NOT NULL AND r.comment != '' AND " +
           "r.listing.id = :listingId ORDER BY r.createdAt DESC")
    List<Review> findReviewsWithCommentsForListing(@Param("listingId") UUID listingId);
    
    @Query("SELECT r FROM Review r WHERE r.comment IS NOT NULL AND r.comment != '' AND " +
           "r.vendor.id = :vendorId ORDER BY r.createdAt DESC")
    List<Review> findReviewsWithCommentsForVendor(@Param("vendorId") UUID vendorId);
    
    boolean existsByBookingId(UUID bookingId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") String userId);
    
    @Query("SELECT r FROM Review r WHERE r.createdAt >= :fromDate " +
           "ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(@Param("fromDate") java.time.LocalDateTime fromDate);
}