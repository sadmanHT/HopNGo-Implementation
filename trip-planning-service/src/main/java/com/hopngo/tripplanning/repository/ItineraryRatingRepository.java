package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.ItineraryRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryRatingRepository extends JpaRepository<ItineraryRating, UUID> {

    /**
     * Find all ratings by a specific user
     */
    List<ItineraryRating> findByUserId(String userId);

    /**
     * Find all ratings for a specific itinerary
     */
    List<ItineraryRating> findByItineraryId(UUID itineraryId);

    /**
     * Find rating by user and itinerary
     */
    Optional<ItineraryRating> findByUserIdAndItineraryId(String userId, UUID itineraryId);

    /**
     * Find ratings by user and interaction type
     */
    List<ItineraryRating> findByUserIdAndInteractionType(String userId, String interactionType);

    /**
     * Find ratings by interaction type
     */
    List<ItineraryRating> findByInteractionType(String interactionType);

    /**
     * Find ratings with pagination
     */
    Page<ItineraryRating> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find recent ratings for a user
     */
    List<ItineraryRating> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            String userId, Instant since);

    /**
     * Calculate average rating for an itinerary
     */
    @Query("SELECT AVG(ir.rating) FROM ItineraryRating ir WHERE ir.itinerary.id = :itineraryId AND ir.interactionType = 'rated'")
    Optional<Double> findAverageRatingForItinerary(@Param("itineraryId") UUID itineraryId);

    /**
     * Count ratings for an itinerary by interaction type
     */
    long countByItineraryIdAndInteractionType(UUID itineraryId, String interactionType);

    /**
     * Count total ratings by a user
     */
    long countByUserId(String userId);

    /**
     * Find users who rated an itinerary highly (4-5 stars)
     */
    @Query("SELECT DISTINCT ir.userId FROM ItineraryRating ir WHERE ir.itinerary.id = :itineraryId AND ir.rating >= 4 AND ir.interactionType = 'rated'")
    List<String> findUsersWhoRatedHighly(@Param("itineraryId") UUID itineraryId);

    /**
     * Find itineraries highly rated by a user
     */
    @Query("SELECT ir.itinerary.id FROM ItineraryRating ir WHERE ir.userId = :userId AND ir.rating >= 4 AND ir.interactionType = 'rated'")
    List<UUID> findHighlyRatedItinerariesByUser(@Param("userId") String userId);

    /**
     * Find users with similar rating patterns to a given user
     */
    @Query("SELECT DISTINCT ir2.userId FROM ItineraryRating ir1 " +
           "JOIN ItineraryRating ir2 ON ir1.itinerary.id = ir2.itinerary.id " +
           "WHERE ir1.userId = :userId AND ir2.userId != :userId " +
           "AND ir1.interactionType = 'rated' AND ir2.interactionType = 'rated' " +
           "AND ABS(ir1.rating - ir2.rating) <= 1")
    List<String> findUsersWithSimilarRatingPatterns(@Param("userId") String userId);

    /**
     * Find most popular itineraries (by interaction count)
     */
    @Query("SELECT ir.itinerary.id, COUNT(ir) as interactionCount " +
           "FROM ItineraryRating ir " +
           "GROUP BY ir.itinerary.id " +
           "ORDER BY interactionCount DESC")
    List<Object[]> findMostPopularItineraries(Pageable pageable);

    /**
     * Find top rated itineraries
     */
    @Query("SELECT ir.itinerary.id, AVG(ir.rating) as avgRating, COUNT(ir) as ratingCount " +
           "FROM ItineraryRating ir " +
           "WHERE ir.interactionType = 'rated' " +
           "GROUP BY ir.itinerary.id " +
           "HAVING COUNT(ir) >= :minRatings " +
           "ORDER BY avgRating DESC")
    List<Object[]> findTopRatedItineraries(@Param("minRatings") long minRatings, Pageable pageable);

    /**
     * Find user's interaction history with itineraries
     */
    @Query("SELECT ir FROM ItineraryRating ir WHERE ir.userId = :userId ORDER BY ir.createdAt DESC")
    List<ItineraryRating> findUserInteractionHistory(@Param("userId") String userId, Pageable pageable);

    /**
     * Check if user has interacted with an itinerary
     */
    boolean existsByUserIdAndItineraryId(String userId, UUID itineraryId);

    /**
     * Delete all ratings for a specific itinerary
     */
    void deleteByItineraryId(UUID itineraryId);

    /**
     * Delete all ratings by a specific user
     */
    void deleteByUserId(String userId);

    /**
     * Find ratings created within a time range
     */
    List<ItineraryRating> findByCreatedAtBetween(Instant startDate, Instant endDate);
}