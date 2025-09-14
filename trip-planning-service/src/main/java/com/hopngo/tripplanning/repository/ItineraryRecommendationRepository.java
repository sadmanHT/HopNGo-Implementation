package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.ItineraryRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryRecommendationRepository extends JpaRepository<ItineraryRecommendation, UUID> {

    /**
     * Find active (non-expired) recommendations for a user
     */
    @Query("SELECT ir FROM ItineraryRecommendation ir WHERE ir.userId = :userId AND ir.expiresAt > CURRENT_TIMESTAMP ORDER BY ir.recommendationScore DESC")
    List<ItineraryRecommendation> findActiveRecommendationsForUser(@Param("userId") String userId);

    /**
     * Find active recommendations for a user with pagination
     */
    @Query("SELECT ir FROM ItineraryRecommendation ir WHERE ir.userId = :userId AND ir.expiresAt > CURRENT_TIMESTAMP ORDER BY ir.recommendationScore DESC")
    Page<ItineraryRecommendation> findActiveRecommendationsForUser(@Param("userId") String userId, Pageable pageable);

    /**
     * Find top N active recommendations for a user
     */
    @Query("SELECT ir FROM ItineraryRecommendation ir WHERE ir.userId = :userId AND ir.expiresAt > CURRENT_TIMESTAMP ORDER BY ir.recommendationScore DESC")
    List<ItineraryRecommendation> findTopActiveRecommendationsForUser(@Param("userId") String userId, Pageable pageable);

    /**
     * Find recommendations above a certain score threshold
     */
    @Query("SELECT ir FROM ItineraryRecommendation ir WHERE ir.userId = :userId AND ir.expiresAt > CURRENT_TIMESTAMP AND ir.recommendationScore >= :minScore ORDER BY ir.recommendationScore DESC")
    List<ItineraryRecommendation> findActiveRecommendationsAboveScore(
            @Param("userId") String userId, 
            @Param("minScore") BigDecimal minScore);

    /**
     * Check if a specific itinerary is already recommended to a user
     */
    @Query("SELECT COUNT(ir) > 0 FROM ItineraryRecommendation ir WHERE ir.userId = :userId AND ir.recommendedItinerary.id = :itineraryId AND ir.expiresAt > CURRENT_TIMESTAMP")
    boolean existsActiveRecommendationForUserAndItinerary(
            @Param("userId") String userId, 
            @Param("itineraryId") UUID itineraryId);

    /**
     * Find expired recommendations
     */
    @Query("SELECT ir FROM ItineraryRecommendation ir WHERE ir.expiresAt <= CURRENT_TIMESTAMP")
    List<ItineraryRecommendation> findExpiredRecommendations();

    /**
     * Delete expired recommendations
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ItineraryRecommendation ir WHERE ir.expiresAt <= CURRENT_TIMESTAMP")
    int deleteExpiredRecommendations();

    /**
     * Delete all recommendations for a user
     */
    void deleteByUserId(String userId);

    /**
     * Delete recommendations for a specific itinerary
     */
    void deleteByRecommendedItineraryId(UUID itineraryId);

    /**
     * Count active recommendations for a user
     */
    @Query("SELECT COUNT(ir) FROM ItineraryRecommendation ir WHERE ir.userId = :userId AND ir.expiresAt > CURRENT_TIMESTAMP")
    long countActiveRecommendationsForUser(@Param("userId") String userId);

    /**
     * Find all recommendations for a user (including expired)
     */
    List<ItineraryRecommendation> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find recommendations created within a time range
     */
    List<ItineraryRecommendation> findByCreatedAtBetween(Instant startDate, Instant endDate);

    /**
     * Find recommendations expiring soon (within specified hours)
     */
    @Query("SELECT ir FROM ItineraryRecommendation ir WHERE ir.expiresAt > CURRENT_TIMESTAMP AND ir.expiresAt <= :expiryThreshold")
    List<ItineraryRecommendation> findRecommendationsExpiringSoon(@Param("expiryThreshold") Instant expiryThreshold);

    /**
     * Update expiry time for recommendations
     */
    @Modifying
    @Transactional
    @Query("UPDATE ItineraryRecommendation ir SET ir.expiresAt = :newExpiryTime WHERE ir.userId = :userId")
    int updateExpiryTimeForUser(@Param("userId") String userId, @Param("newExpiryTime") Instant newExpiryTime);

    /**
     * Find most recommended itineraries globally
     */
    @Query("SELECT ir.recommendedItinerary.id, COUNT(ir) as recommendationCount, AVG(ir.recommendationScore) as avgScore " +
           "FROM ItineraryRecommendation ir " +
           "WHERE ir.expiresAt > CURRENT_TIMESTAMP " +
           "GROUP BY ir.recommendedItinerary.id " +
           "ORDER BY recommendationCount DESC")
    List<Object[]> findMostRecommendedItineraries(Pageable pageable);

    /**
     * Find average recommendation score for an itinerary
     */
    @Query("SELECT AVG(ir.recommendationScore) FROM ItineraryRecommendation ir WHERE ir.recommendedItinerary.id = :itineraryId AND ir.expiresAt > CURRENT_TIMESTAMP")
    Optional<BigDecimal> findAverageRecommendationScoreForItinerary(@Param("itineraryId") UUID itineraryId);

    /**
     * Find users who have been recommended a specific itinerary
     */
    @Query("SELECT DISTINCT ir.userId FROM ItineraryRecommendation ir WHERE ir.recommendedItinerary.id = :itineraryId AND ir.expiresAt > CURRENT_TIMESTAMP")
    List<String> findUsersRecommendedItinerary(@Param("itineraryId") UUID itineraryId);

    /**
     * Delete old recommendations (older than specified date)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ItineraryRecommendation ir WHERE ir.createdAt < :cutoffDate")
    int deleteOldRecommendations(@Param("cutoffDate") Instant cutoffDate);
}