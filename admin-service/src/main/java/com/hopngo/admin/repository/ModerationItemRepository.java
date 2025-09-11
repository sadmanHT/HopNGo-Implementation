package com.hopngo.admin.repository;

import com.hopngo.admin.entity.ModerationItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ModerationItemRepository extends JpaRepository<ModerationItem, Long> {

    /**
     * Find moderation items by status with pagination
     */
    Page<ModerationItem> findByStatus(ModerationItem.ModerationStatus status, Pageable pageable);

    /**
     * Find moderation items by type with pagination
     */
    Page<ModerationItem> findByType(ModerationItem.ModerationItemType type, Pageable pageable);

    /**
     * Find moderation items by status and type with pagination
     */
    Page<ModerationItem> findByStatusAndType(
            ModerationItem.ModerationStatus status,
            ModerationItem.ModerationItemType type,
            Pageable pageable
    );

    /**
     * Find moderation items assigned to a specific user
     */
    Page<ModerationItem> findByAssigneeUserId(Long assigneeUserId, Pageable pageable);

    /**
     * Custom query to find moderation items with optional filters
     */
    @Query("SELECT m FROM ModerationItem m WHERE " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:type IS NULL OR m.type = :type) AND " +
           "(:assigneeUserId IS NULL OR m.assigneeUserId = :assigneeUserId)")
    Page<ModerationItem> findWithFilters(
            @Param("status") ModerationItem.ModerationStatus status,
            @Param("type") ModerationItem.ModerationItemType type,
            @Param("assigneeUserId") Long assigneeUserId,
            Pageable pageable
    );

    /**
     * Check if a moderation item exists for a specific reference
     */
    boolean existsByTypeAndRefId(ModerationItem.ModerationItemType type, Long refId);

    /**
     * Find moderation item by type and reference ID
     */
    ModerationItem findByTypeAndRefId(ModerationItem.ModerationItemType type, Long refId);

    /**
     * Check if a moderation item exists by reference ID
     */
    boolean existsByReferenceId(String referenceId);

    /**
     * Find moderation items by status, type and assignee user ID
     */
    Page<ModerationItem> findByStatusAndTypeAndAssigneeUserId(
            ModerationItem.ModerationStatus status,
            ModerationItem.ModerationItemType type,
            Long assigneeUserId,
            Pageable pageable
    );

    /**
     * Count moderation items by actor user ID and created date range
     */
    @Query("SELECT COUNT(m) FROM ModerationItem m WHERE m.reporterUserId = :actorUserId AND m.createdAt BETWEEN :startDate AND :endDate")
    long countByActorUserIdAndCreatedAtBetween(
            @Param("actorUserId") Long actorUserId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );


}