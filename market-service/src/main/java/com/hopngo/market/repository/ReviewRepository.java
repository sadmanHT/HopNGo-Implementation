package com.hopngo.market.repository;

import com.hopngo.market.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    
    // Find reviews by product ID - only public reviews
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.visibility = 'PUBLIC' ORDER BY r.createdAt DESC")
    Page<Review> findByProductIdAndVisibilityPublic(@Param("productId") String productId, Pageable pageable);
    
    // Find reviews by product ID - all reviews for admin
    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    
    // Find reviews by user ID - only public reviews for non-owners
    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.visibility = 'PUBLIC' ORDER BY r.createdAt DESC")
    Page<Review> findByUserIdAndVisibilityPublic(@Param("userId") String userId, Pageable pageable);
    
    // Find reviews by user ID - all reviews for owner/admin
    Page<Review> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Calculate average rating for a product - only public reviews
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.visibility = 'PUBLIC'")
    Double findAverageRatingByProductId(@Param("productId") String productId);
    
    // Count reviews for a product - only public reviews
    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.visibility = 'PUBLIC'")
    Long countByProductIdAndVisibilityPublic(@Param("productId") String productId);
    
    // Find reviews by visibility for admin
    Page<Review> findByVisibilityOrderByCreatedAtDesc(Review.Visibility visibility, Pageable pageable);
}