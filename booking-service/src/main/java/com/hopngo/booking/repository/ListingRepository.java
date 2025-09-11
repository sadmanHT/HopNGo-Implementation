package com.hopngo.booking.repository;

import com.hopngo.booking.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    
    @Query("SELECT l FROM Listing l JOIN FETCH l.vendor WHERE l.vendor.id = :vendorId")
    Page<Listing> findByVendorId(@Param("vendorId") UUID vendorId, Pageable pageable);
    
    @Query("SELECT l FROM Listing l JOIN FETCH l.vendor WHERE l.vendor.userId = :vendorUserId")
    Page<Listing> findByVendorUserId(@Param("vendorUserId") String vendorUserId, Pageable pageable);
    
    List<Listing> findByStatus(Listing.ListingStatus status);
    
    List<Listing> findByCategory(String category);
    
    List<Listing> findByVendorIdAndStatus(UUID vendorId, Listing.ListingStatus status);
    
    @Query("SELECT l FROM Listing l JOIN FETCH l.vendor WHERE l.status = 'ACTIVE' AND " +
           "(:category IS NULL OR l.category = :category) AND " +
           "(:minPrice IS NULL OR l.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.basePrice <= :maxPrice) AND " +
           "(:maxGuests IS NULL OR l.maxGuests >= :maxGuests)")
    List<Listing> findActiveListingsWithFilters(
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("maxGuests") Integer maxGuests
    );
    
    @Query("SELECT l FROM Listing l JOIN FETCH l.vendor WHERE l.status = 'ACTIVE' AND " +
           "l.latitude IS NOT NULL AND l.longitude IS NOT NULL AND " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) * " +
           "cos(radians(l.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
           "sin(radians(l.latitude)))) <= :radiusKm AND " +
           "(:category IS NULL OR l.category = :category) AND " +
           "(:minPrice IS NULL OR l.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.basePrice <= :maxPrice) AND " +
           "(:maxGuests IS NULL OR l.maxGuests >= :maxGuests)")
    List<Listing> findActiveListingsWithinRadiusAndFilters(
        @Param("lat") Double latitude,
        @Param("lng") Double longitude,
        @Param("radiusKm") Double radiusKm,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("maxGuests") Integer maxGuests
    );
    
    @Query(value = "SELECT * FROM listings l WHERE l.status = 'ACTIVE' AND " +
           "(:amenities IS NULL OR array_length(CAST(:amenities AS text[]), 1) IS NULL OR " +
           "l.amenities && CAST(:amenities AS text[]))", nativeQuery = true)
    List<Listing> findActiveListingsWithAmenities(@Param("amenities") List<String> amenities);
    
    @Query(value = "SELECT * FROM listings l WHERE l.status = 'ACTIVE' AND " +
           "l.latitude IS NOT NULL AND l.longitude IS NOT NULL AND " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) * " +
           "cos(radians(l.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
           "sin(radians(l.latitude)))) <= :radiusKm AND " +
           "(:category IS NULL OR l.category = :category) AND " +
           "(:minPrice IS NULL OR l.base_price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.base_price <= :maxPrice) AND " +
           "(:maxGuests IS NULL OR l.max_guests >= :maxGuests) AND " +
           "(:amenities IS NULL OR array_length(CAST(:amenities AS text[]), 1) IS NULL OR " +
           "l.amenities && CAST(:amenities AS text[]))", nativeQuery = true)
    Page<Listing> searchListings(
        @Param("lat") Double latitude,
        @Param("lng") Double longitude,
        @Param("radiusKm") Double radiusKm,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("maxGuests") Integer maxGuests,
        @Param("amenities") List<String> amenities,
        Pageable pageable
    );
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.listing.id = :listingId")
    Double getAverageRating(@Param("listingId") UUID listingId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.listing.id = :listingId")
    long getReviewCount(@Param("listingId") UUID listingId);
}