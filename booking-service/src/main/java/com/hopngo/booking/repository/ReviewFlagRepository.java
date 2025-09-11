package com.hopngo.booking.repository;

import com.hopngo.booking.entity.ReviewFlag;
import com.hopngo.booking.entity.ReviewFlagStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewFlagRepository extends JpaRepository<ReviewFlag, UUID> {
    
    List<ReviewFlag> findByReviewId(UUID reviewId);
    
    List<ReviewFlag> findByReporterUserId(String reporterUserId);
    
    List<ReviewFlag> findByStatus(ReviewFlagStatus status);
    
    @Query("SELECT rf FROM ReviewFlag rf WHERE rf.review.listing.id = :listingId")
    List<ReviewFlag> findByListingId(@Param("listingId") UUID listingId);
    
    @Query("SELECT rf FROM ReviewFlag rf WHERE rf.review.vendor.id = :vendorId")
    List<ReviewFlag> findByVendorId(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT rf FROM ReviewFlag rf WHERE rf.review.vendor.userId = :vendorUserId")
    List<ReviewFlag> findByVendorUserId(@Param("vendorUserId") String vendorUserId);
    
    boolean existsByReviewIdAndReporterUserId(UUID reviewId, String reporterUserId);
    
    long countByStatus(ReviewFlagStatus status);
}