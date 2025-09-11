package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.SavedRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedRouteRepository extends JpaRepository<SavedRoute, UUID> {

    /**
     * Find all saved routes for a specific user with pagination
     */
    Page<SavedRoute> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find a saved route by ID and user ID (for security)
     */
    Optional<SavedRoute> findByIdAndUserId(UUID id, String userId);

    /**
     * Check if a saved route exists for a specific user
     */
    boolean existsByIdAndUserId(UUID id, String userId);

    /**
     * Count total saved routes for a user
     */
    long countByUserId(String userId);

    /**
     * Find recent saved routes for a user (last 10)
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId ORDER BY sr.createdAt DESC")
    Page<SavedRoute> findRecentByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Find saved routes by name containing text (case insensitive)
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId AND LOWER(sr.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY sr.createdAt DESC")
    Page<SavedRoute> findByUserIdAndNameContainingIgnoreCase(@Param("userId") String userId, @Param("name") String name, Pageable pageable);

    /**
     * Find routes by name (case-insensitive search) - alternative method name
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId AND LOWER(sr.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY sr.createdAt DESC")
    Page<SavedRoute> findByUserIdAndNameContaining(@Param("userId") String userId, 
                                                   @Param("name") String name, 
                                                   Pageable pageable);

    /**
     * Find routes by name containing text (case insensitive) - ordered by created date
     */
    default Page<SavedRoute> findByUserIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String userId, 
                                                                                          String name, 
                                                                                          Pageable pageable) {
        return findByUserIdAndNameContaining(userId, name, pageable);
    }

    /**
     * Find saved routes by transportation mode
     */
    Page<SavedRoute> findByUserIdAndModeOrderByCreatedAtDesc(String userId, String mode, Pageable pageable);

    /**
     * Find saved routes by distance range
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId AND sr.distanceKm BETWEEN :minDistance AND :maxDistance ORDER BY sr.createdAt DESC")
    Page<SavedRoute> findByUserIdAndDistanceBetween(@Param("userId") String userId, @Param("minDistance") BigDecimal minDistance, @Param("maxDistance") BigDecimal maxDistance, Pageable pageable);

    /**
     * Find routes by distance range (ordered by distance)
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId AND sr.distanceKm BETWEEN :minDistance AND :maxDistance ORDER BY sr.distanceKm ASC")
    Page<SavedRoute> findByUserIdAndDistanceKmBetweenOrderByDistanceKmAsc(String userId, BigDecimal minDistance, BigDecimal maxDistance, Pageable pageable);

    /**
     * Find saved routes by duration range
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId AND sr.durationMin BETWEEN :minDuration AND :maxDuration ORDER BY sr.createdAt DESC")
    Page<SavedRoute> findByUserIdAndDurationBetween(@Param("userId") String userId, @Param("minDuration") Integer minDuration, @Param("maxDuration") Integer maxDuration, Pageable pageable);

    /**
     * Find routes by duration range (ordered by duration)
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId AND sr.durationMin BETWEEN :minDuration AND :maxDuration ORDER BY sr.durationMin ASC")
    Page<SavedRoute> findByUserIdAndDurationMinBetweenOrderByDurationMinAsc(String userId, Integer minDuration, Integer maxDuration, Pageable pageable);

    /**
     * Find saved routes by multiple criteria with dynamic filtering
     */
    @Query("SELECT sr FROM SavedRoute sr WHERE sr.userId = :userId " +
           "AND (:name IS NULL OR LOWER(sr.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:mode IS NULL OR sr.mode = :mode) " +
           "AND (:minDistance IS NULL OR sr.distanceKm >= :minDistance) " +
           "AND (:maxDistance IS NULL OR sr.distanceKm <= :maxDistance) " +
           "AND (:minDuration IS NULL OR sr.durationMin >= :minDuration) " +
           "AND (:maxDuration IS NULL OR sr.durationMin <= :maxDuration)")
    Page<SavedRoute> findByUserIdAndCriteria(@Param("userId") String userId,
                                           @Param("name") String name,
                                           @Param("mode") String mode,
                                           @Param("minDistance") BigDecimal minDistance,
                                           @Param("maxDistance") BigDecimal maxDistance,
                                           @Param("minDuration") Integer minDuration,
                                           @Param("maxDuration") Integer maxDuration,
                                           Pageable pageable);

    /**
     * Check if user has reached the maximum number of saved routes
     */
    @Query("SELECT COUNT(sr) FROM SavedRoute sr WHERE sr.userId = :userId")
    long countSavedRoutesByUserId(@Param("userId") String userId);

    /**
     * Find saved routes by name (exact match) for duplicate checking
     */
    Optional<SavedRoute> findByUserIdAndName(String userId, String name);
}