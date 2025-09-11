package com.hopngo.booking.repository;

import com.hopngo.booking.entity.VendorResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorResponseRepository extends JpaRepository<VendorResponse, UUID> {
    
    Optional<VendorResponse> findByReviewId(UUID reviewId);
    
    List<VendorResponse> findByVendorUserId(String vendorUserId);
    
    @Query("SELECT vr FROM VendorResponse vr WHERE vr.review.listing.id = :listingId")
    List<VendorResponse> findByListingId(@Param("listingId") UUID listingId);
    
    @Query("SELECT vr FROM VendorResponse vr WHERE vr.review.vendor.id = :vendorId")
    List<VendorResponse> findByVendorId(@Param("vendorId") UUID vendorId);
    
    boolean existsByReviewId(UUID reviewId);
}