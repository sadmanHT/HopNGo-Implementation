package com.hopngo.booking.repository;

import com.hopngo.booking.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    
    boolean existsByMessageId(String messageId);
    
    List<ProcessedEvent> findByProcessedAtBefore(LocalDateTime cutoffDate);
    
    List<ProcessedEvent> findByEventType(String eventType);
    
    @Query("SELECT COUNT(pe) FROM ProcessedEvent pe WHERE pe.eventType = :eventType")
    long countByEventType(@Param("eventType") String eventType);
    
    @Query("SELECT pe FROM ProcessedEvent pe WHERE pe.processedAt BETWEEN :startDate AND :endDate")
    List<ProcessedEvent> findByProcessedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}