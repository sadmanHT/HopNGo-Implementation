package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.ItineraryComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryCommentRepository extends JpaRepository<ItineraryComment, UUID> {

    /**
     * Find all comments for a specific itinerary ordered by creation time
     */
    List<ItineraryComment> findByItineraryIdOrderByCreatedAtAsc(UUID itineraryId);

    /**
     * Find all comments for a specific itinerary with pagination
     */
    Page<ItineraryComment> findByItineraryIdOrderByCreatedAtAsc(UUID itineraryId, Pageable pageable);

    /**
     * Find comments by author
     */
    List<ItineraryComment> findByAuthorUserIdOrderByCreatedAtDesc(String authorUserId);

    /**
     * Find comments by author with pagination
     */
    Page<ItineraryComment> findByAuthorUserIdOrderByCreatedAtDesc(String authorUserId, Pageable pageable);

    /**
     * Count total comments for an itinerary
     */
    long countByItineraryId(UUID itineraryId);

    /**
     * Count total comments by a user
     */
    long countByAuthorUserId(String authorUserId);

    /**
     * Find recent comments for an itinerary (last N comments)
     */
    @Query("SELECT c FROM ItineraryComment c WHERE c.itinerary.id = :itineraryId " +
           "ORDER BY c.createdAt DESC")
    Page<ItineraryComment> findRecentCommentsByItineraryId(@Param("itineraryId") UUID itineraryId, Pageable pageable);

    /**
     * Find comments created after a specific date
     */
    List<ItineraryComment> findByItineraryIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID itineraryId, LocalDateTime after);

    /**
     * Find comments created between dates
     */
    List<ItineraryComment> findByItineraryIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID itineraryId, LocalDateTime start, LocalDateTime end);

    /**
     * Delete all comments for an itinerary
     */
    void deleteByItineraryId(UUID itineraryId);

    /**
     * Delete all comments by a user
     */
    void deleteByAuthorUserId(String authorUserId);

    /**
     * Check if a user has commented on an itinerary
     */
    boolean existsByItineraryIdAndAuthorUserId(UUID itineraryId, String authorUserId);

    /**
     * Find all comments across all itineraries (for admin/monitoring)
     */
    @Query("SELECT c FROM ItineraryComment c ORDER BY c.createdAt DESC")
    Page<ItineraryComment> findAllCommentsOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find comments containing specific text (case insensitive)
     */
    @Query("SELECT c FROM ItineraryComment c WHERE c.itinerary.id = :itineraryId " +
           "AND LOWER(c.message) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY c.createdAt ASC")
    List<ItineraryComment> findByItineraryIdAndMessageContainingIgnoreCase(
            @Param("itineraryId") UUID itineraryId, @Param("searchText") String searchText);
}