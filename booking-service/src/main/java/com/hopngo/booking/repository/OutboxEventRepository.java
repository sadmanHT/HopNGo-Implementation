package com.hopngo.booking.repository;

import com.hopngo.booking.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsOrderByCreatedAt();
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' ORDER BY o.createdAt ASC")
    List<OutboxEvent> findFailedEvents();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND " +
           "e.createdAt <= :maxCreatedAt ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsOlderThan(@Param("maxCreatedAt") LocalDateTime maxCreatedAt);
    
    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
    
    List<OutboxEvent> findByEventType(String eventType);
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.aggregateType = :aggregateType AND " +
           "e.aggregateId = :aggregateId AND e.status = :status " +
           "ORDER BY e.createdAt DESC")
    List<OutboxEvent> findByAggregateAndStatus(
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId,
        @Param("status") OutboxEvent.OutboxStatus status
    );
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PROCESSED', e.processedAt = :processedAt " +
           "WHERE e.id = :id")
    int markAsProcessed(@Param("id") UUID id, @Param("processedAt") LocalDateTime processedAt);
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'FAILED' WHERE e.id = :id")
    int markAsFailed(@Param("id") UUID id);
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PROCESSED', e.processedAt = :processedAt " +
           "WHERE e.id IN :ids")
    int markMultipleAsProcessed(
        @Param("ids") List<UUID> ids, 
        @Param("processedAt") LocalDateTime processedAt
    );
    
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = 'PENDING'")
    long countPendingEvents();
    
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = 'FAILED'")
    long countFailedEvents();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'FAILED' AND " +
           "e.createdAt >= :fromDate ORDER BY e.createdAt DESC")
    List<OutboxEvent> findRecentFailedEvents(@Param("fromDate") LocalDateTime fromDate);
    
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PROCESSED' AND " +
           "e.processedAt < :cutoffDate")
    int deleteProcessedEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PROCESSED' AND o.processedAt < :cutoffDate")
    void deleteOldProcessedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND " +
           "e.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT DISTINCT e.eventType FROM OutboxEvent e WHERE e.status = 'PENDING'")
    List<String> findDistinctPendingEventTypes();
    
    @Query("SELECT DISTINCT e.aggregateType FROM OutboxEvent e WHERE e.status = 'PENDING'")
    List<String> findDistinctPendingAggregateTypes();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND " +
           "e.eventType = :eventType ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsByType(@Param("eventType") String eventType);
}