package com.hopngo.market.repository;

import com.hopngo.market.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, String> {
    
    // Find all active public listings
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> findActivePublicListings(Pageable pageable);
    
    // Find listings by category - only active public listings
    @Query("SELECT l FROM Listing l WHERE l.category = :category AND l.status = 'ACTIVE' AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> findByCategoryAndActiveAndVisibilityPublic(@Param("category") String category, Pageable pageable);
    
    // Find listings by user ID - only public listings for non-owners
    @Query("SELECT l FROM Listing l WHERE l.userId = :userId AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> findByUserIdAndVisibilityPublic(@Param("userId") String userId, Pageable pageable);
    
    // Find listings by user ID - all listings for owner/admin
    Page<Listing> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Search listings by title or description - only active public listings
    @Query("SELECT l FROM Listing l WHERE (LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND l.status = 'ACTIVE' AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> searchActivePublicListings(@Param("query") String query, Pageable pageable);
    
    // Find listings by price range - only active public listings
    @Query("SELECT l FROM Listing l WHERE l.price BETWEEN :minPrice AND :maxPrice AND l.status = 'ACTIVE' AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> findByPriceRangeAndActiveAndVisibilityPublic(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);
    
    // Find listings by tags - only active public listings
    @Query("SELECT l FROM Listing l JOIN l.tags t WHERE t IN :tags AND l.status = 'ACTIVE' AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> findByTagsInAndActiveAndVisibilityPublic(@Param("tags") List<String> tags, Pageable pageable);
    
    // Find listings by visibility for admin
    Page<Listing> findByVisibilityOrderByCreatedAtDesc(Listing.Visibility visibility, Pageable pageable);
    
    // Find listings by status
    Page<Listing> findByStatusOrderByCreatedAtDesc(Listing.Status status, Pageable pageable);
    
    // Find listings within geographic bounds - only active public listings
    @Query("SELECT l FROM Listing l WHERE l.location.latitude BETWEEN :minLat AND :maxLat AND l.location.longitude BETWEEN :minLng AND :maxLng AND l.status = 'ACTIVE' AND l.visibility = 'PUBLIC' ORDER BY l.createdAt DESC")
    Page<Listing> findListingsInBoundingBoxAndActiveAndVisibilityPublic(@Param("minLat") double minLat, @Param("maxLat") double maxLat, @Param("minLng") double minLng, @Param("maxLng") double maxLng, Pageable pageable);
}