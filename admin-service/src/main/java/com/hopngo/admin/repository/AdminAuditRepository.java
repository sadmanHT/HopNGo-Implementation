package com.hopngo.admin.repository;

import com.hopngo.admin.entity.AdminAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AdminAuditRepository extends JpaRepository<AdminAudit, Long> {

    /**
     * Find audit entries by actor user ID with pagination
     */
    Page<AdminAudit> findByActorUserId(Long actorUserId, Pageable pageable);

    /**
     * Find audit entries by target type and ID
     */
    Page<AdminAudit> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);

    /**
     * Find audit entries by action
     */
    Page<AdminAudit> findByAction(String action, Pageable pageable);

    /**
     * Find audit entries within a date range
     */
    Page<AdminAudit> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Custom query to find audit entries with optional filters
     */
    @Query("SELECT a FROM AdminAudit a WHERE " +
           "(:actorUserId IS NULL OR a.actorUserId = :actorUserId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:targetType IS NULL OR a.targetType = :targetType) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<AdminAudit> findWithFilters(
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    /**
     * Get recent audit entries for a specific target
     */
    @Query(value = "SELECT a FROM AdminAudit a WHERE a.targetType = :targetType AND a.targetId = :targetId " +
           "ORDER BY a.createdAt DESC LIMIT :limit")
    List<AdminAudit> findRecentByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("limit") int limit
    );

    /**
     * Count audit entries by actor
     */
    long countByActorUserId(Long actorUserId);

    /**
     * Count audit entries by action
     */
    long countByAction(String action);

    /**
     * Alias for findWithFilters method
     */
    default Page<AdminAudit> findByFilters(
            Long actorUserId,
            String targetType,
            Long targetId,
            String action,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        return findWithFilters(actorUserId, action, targetType, startDate, endDate, pageable);
    }

    /**
     * Count audit entries by actor user ID and date range
     */
    long countByActorUserIdAndCreatedAtBetween(Long actorUserId, Instant startDate, Instant endDate);

    /**
     * Find all audit entries ordered by timestamp descending
     */
    @Query("SELECT a FROM AdminAudit a ORDER BY a.createdAt DESC")
    List<AdminAudit> findAllByOrderByTimestampDesc();

    /**
     * Count audit entries by action and date range
     */
    long countByActionAndCreatedAtBetween(String action, Instant startDate, Instant endDate);

    /**
     * Count audit entries by target type, target ID and date range
     */
    long countByTargetTypeAndTargetIdAndCreatedAtBetween(String targetType, Long targetId, Instant startDate, Instant endDate);

    /**
     * Alias for findAllByOrderByTimestampDesc
     */
    default List<AdminAudit> getAuditEntries() {
        return findAllByOrderByTimestampDesc();
    }
}