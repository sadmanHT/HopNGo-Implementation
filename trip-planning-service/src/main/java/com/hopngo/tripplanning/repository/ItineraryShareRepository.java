package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.ItineraryShare;
import com.hopngo.tripplanning.enums.ShareVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryShareRepository extends JpaRepository<ItineraryShare, UUID> {

    /**
     * Find share configuration by token
     */
    Optional<ItineraryShare> findByToken(String token);

    /**
     * Find share configuration by itinerary ID
     */
    Optional<ItineraryShare> findByItineraryId(UUID itineraryId);

    /**
     * Find share configuration by itinerary ID and check if it exists
     */
    boolean existsByItineraryId(UUID itineraryId);

    /**
     * Find all public itineraries
     */
    @Query("SELECT s FROM ItineraryShare s WHERE s.visibility = :visibility ORDER BY s.createdAt DESC")
    List<ItineraryShare> findByVisibility(@Param("visibility") ShareVisibility visibility);

    /**
     * Find all shares for itineraries owned by a specific user
     */
    @Query("SELECT s FROM ItineraryShare s WHERE s.itinerary.userId = :userId ORDER BY s.createdAt DESC")
    List<ItineraryShare> findByItineraryUserId(@Param("userId") String userId);

    /**
     * Check if an itinerary is publicly accessible (LINK or PUBLIC)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ItineraryShare s " +
           "WHERE s.itinerary.id = :itineraryId AND s.visibility IN ('LINK', 'PUBLIC')")
    boolean isItineraryPubliclyAccessible(@Param("itineraryId") UUID itineraryId);

    /**
     * Check if comments are allowed for a shared itinerary
     */
    @Query("SELECT s.canComment FROM ItineraryShare s WHERE s.token = :token")
    Optional<Boolean> canCommentByToken(@Param("token") String token);

    /**
     * Delete share configuration by itinerary ID
     */
    void deleteByItineraryId(UUID itineraryId);

    /**
     * Count total shared itineraries by visibility
     */
    long countByVisibility(ShareVisibility visibility);
}