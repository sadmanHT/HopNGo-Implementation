package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Find event by unique event ID
     */
    Optional<Event> findByEventId(String eventId);

    /**
     * Check if event exists by event ID (for deduplication)
     */
    boolean existsByEventId(String eventId);

    /**
     * Find events by user ID within date range
     */
    @Query("SELECT e FROM Event e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    Page<Event> findByUserIdAndDateRange(@Param("userId") String userId, 
                                        @Param("startDate") OffsetDateTime startDate, 
                                        @Param("endDate") OffsetDateTime endDate, 
                                        Pageable pageable);

    /**
     * Find events by event type within date range
     */
    @Query("SELECT e FROM Event e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    Page<Event> findByEventTypeAndDateRange(@Param("eventType") String eventType, 
                                           @Param("startDate") OffsetDateTime startDate, 
                                           @Param("endDate") OffsetDateTime endDate, 
                                           Pageable pageable);

    /**
     * Find events by category within date range
     */
    @Query("SELECT e FROM Event e WHERE e.eventCategory = :category AND e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    Page<Event> findByEventCategoryAndDateRange(@Param("category") String category, 
                                               @Param("startDate") OffsetDateTime startDate, 
                                               @Param("endDate") OffsetDateTime endDate, 
                                               Pageable pageable);

    /**
     * Find events by session ID
     */
    List<Event> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * Count events by user ID within date range
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    Long countByUserIdAndDateRange(@Param("userId") String userId, 
                                  @Param("startDate") OffsetDateTime startDate, 
                                  @Param("endDate") OffsetDateTime endDate);

    /**
     * Count events by type within date range
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :startDate AND :endDate")
    Long countByEventTypeAndDateRange(@Param("eventType") String eventType, 
                                     @Param("startDate") OffsetDateTime startDate, 
                                     @Param("endDate") OffsetDateTime endDate);

    /**
     * Get daily active users count
     */
    @Query("SELECT COUNT(DISTINCT e.userId) FROM Event e WHERE e.userId IS NOT NULL AND e.createdAt BETWEEN :startDate AND :endDate")
    Long countDistinctUsersByDateRange(@Param("startDate") OffsetDateTime startDate, 
                                      @Param("endDate") OffsetDateTime endDate);

    /**
     * Get event counts grouped by type within date range
     */
    @Query("SELECT e.eventType, COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> getEventCountsByTypeAndDateRange(@Param("startDate") OffsetDateTime startDate, 
                                                   @Param("endDate") OffsetDateTime endDate);

    /**
     * Get event counts grouped by category within date range
     */
    @Query("SELECT e.eventCategory, COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :startDate AND :endDate GROUP BY e.eventCategory ORDER BY COUNT(e) DESC")
    List<Object[]> getEventCountsByCategoryAndDateRange(@Param("startDate") OffsetDateTime startDate, 
                                                       @Param("endDate") OffsetDateTime endDate);

    /**
     * Get hourly event counts for a specific date
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count " +
                  "FROM events " +
                  "WHERE DATE(created_at) = DATE(:date) " +
                  "GROUP BY EXTRACT(HOUR FROM created_at) " +
                  "ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyEventCounts(@Param("date") OffsetDateTime date);

    /**
     * Get top pages by event count within date range
     */
    @Query("SELECT e.pageUrl, COUNT(e) FROM Event e WHERE e.pageUrl IS NOT NULL AND e.createdAt BETWEEN :startDate AND :endDate GROUP BY e.pageUrl ORDER BY COUNT(e) DESC")
    List<Object[]> getTopPagesByEventCount(@Param("startDate") OffsetDateTime startDate, 
                                          @Param("endDate") OffsetDateTime endDate, 
                                          Pageable pageable);

    /**
     * Get conversion funnel data
     */
    @Query("SELECT e.eventType, COUNT(DISTINCT e.userId) FROM Event e WHERE e.eventType IN :eventTypes AND e.createdAt BETWEEN :startDate AND :endDate GROUP BY e.eventType")
    List<Object[]> getConversionFunnelData(@Param("eventTypes") List<String> eventTypes, 
                                          @Param("startDate") OffsetDateTime startDate, 
                                          @Param("endDate") OffsetDateTime endDate);

    /**
     * Find unprocessed events for batch processing
     */
    @Query("SELECT e FROM Event e WHERE e.processedAt IS NULL ORDER BY e.createdAt ASC")
    List<Event> findUnprocessedEvents(Pageable pageable);

    /**
     * Delete old events (for data retention)
     */
    @Query("DELETE FROM Event e WHERE e.createdAt < :cutoffDate")
    void deleteEventsOlderThan(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * Get unique users count by date range and event type
     */
    @Query("SELECT COUNT(DISTINCT e.userId) FROM Event e WHERE e.eventType = :eventType AND e.userId IS NOT NULL AND e.createdAt BETWEEN :startDate AND :endDate")
    Long countDistinctUsersByEventTypeAndDateRange(@Param("eventType") String eventType, 
                                                  @Param("startDate") OffsetDateTime startDate, 
                                                  @Param("endDate") OffsetDateTime endDate);

    /**
     * Count events by event type and timestamp range
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :startTime AND :endTime")
    Long countByEventTypeAndTimestampBetween(
            @Param("eventType") String eventType,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Count distinct users by event type and timestamp range
     */
    @Query("SELECT COUNT(DISTINCT e.userId) FROM Event e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :startTime AND :endTime")
    Long countDistinctUsersByEventTypeAndTimestampBetween(
            @Param("eventType") String eventType,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}