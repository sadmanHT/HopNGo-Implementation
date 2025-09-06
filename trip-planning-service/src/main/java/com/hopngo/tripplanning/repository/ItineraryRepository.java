package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.Itinerary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    /**
     * Find all itineraries for a specific user with pagination
     */
    Page<Itinerary> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find an itinerary by ID and user ID (for security)
     */
    Optional<Itinerary> findByIdAndUserId(UUID id, String userId);

    /**
     * Check if an itinerary exists for a specific user
     */
    boolean existsByIdAndUserId(UUID id, String userId);

    /**
     * Count total itineraries for a user
     */
    long countByUserId(String userId);

    /**
     * Find recent itineraries for a user (last 10)
     */
    @Query("SELECT i FROM Itinerary i WHERE i.userId = :userId ORDER BY i.createdAt DESC")
    Page<Itinerary> findRecentByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Find itineraries by title containing text (case insensitive)
     */
    @Query("SELECT i FROM Itinerary i WHERE i.userId = :userId AND LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY i.createdAt DESC")
    Page<Itinerary> findByUserIdAndTitleContainingIgnoreCase(@Param("userId") String userId, @Param("title") String title, Pageable pageable);

    /**
     * Find itineraries by budget range
     */
    @Query("SELECT i FROM Itinerary i WHERE i.userId = :userId AND i.budget BETWEEN :minBudget AND :maxBudget ORDER BY i.createdAt DESC")
    Page<Itinerary> findByUserIdAndBudgetBetween(@Param("userId") String userId, @Param("minBudget") Integer minBudget, @Param("maxBudget") Integer maxBudget, Pageable pageable);

    /**
     * Find itineraries by days range
     */
    @Query("SELECT i FROM Itinerary i WHERE i.userId = :userId AND i.days BETWEEN :minDays AND :maxDays ORDER BY i.createdAt DESC")
    Page<Itinerary> findByUserIdAndDaysBetween(@Param("userId") String userId, @Param("minDays") Integer minDays, @Param("maxDays") Integer maxDays, Pageable pageable);

    /**
     * Find itineraries by multiple criteria with dynamic filtering
     */
    @Query("SELECT i FROM Itinerary i WHERE i.userId = :userId " +
           "AND (:title IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:minBudget IS NULL OR i.budget >= :minBudget) " +
           "AND (:maxBudget IS NULL OR i.budget <= :maxBudget) " +
           "AND (:minDays IS NULL OR i.days >= :minDays) " +
           "AND (:maxDays IS NULL OR i.days <= :maxDays)")
    Page<Itinerary> findByUserIdAndCriteria(@Param("userId") String userId,
                                          @Param("title") String title,
                                          @Param("minBudget") Integer minBudget,
                                          @Param("maxBudget") Integer maxBudget,
                                          @Param("minDays") Integer minDays,
                                          @Param("maxDays") Integer maxDays,
                                          Pageable pageable);
}