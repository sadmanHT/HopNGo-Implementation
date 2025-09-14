package com.hopngo.repository;

import com.hopngo.entity.DataExportJob;
import com.hopngo.entity.DataExportJob.ExportStatus;
import com.hopngo.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataExportJobRepository extends JpaRepository<DataExportJob, Long> {
    
    /**
     * Find all export jobs for a specific user
     */
    List<DataExportJob> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find all export jobs for a user by user ID
     */
    List<DataExportJob> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find export jobs by status
     */
    List<DataExportJob> findByStatus(ExportStatus status);
    
    /**
     * Find export jobs by status ordered by creation date
     */
    List<DataExportJob> findByStatusOrderByCreatedAtAsc(ExportStatus status);
    
    /**
     * Find export jobs for a user with specific status
     */
    List<DataExportJob> findByUserAndStatus(User user, ExportStatus status);
    
    /**
     * Find export jobs for a user ID with specific status
     */
    List<DataExportJob> findByUserIdAndStatus(Long userId, ExportStatus status);

    /**
     * Find export jobs for a user ID with multiple statuses
     */
    List<DataExportJob> findByUserIdAndStatusIn(Long userId, List<ExportStatus> statuses);

    /**
     * Find export job by ID and user ID
     */
    Optional<DataExportJob> findByIdAndUserId(Long id, Long userId);

    /**
     * Find export jobs for a user ordered by requested date (descending), limited
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.userId = :userId ORDER BY j.createdAt DESC")
    List<DataExportJob> findByUserIdOrderByRequestedAtDesc(@Param("userId") Long userId, Pageable pageable);

    default List<DataExportJob> findByUserIdOrderByRequestedAtDesc(Long userId, int limit) {
        return findByUserIdOrderByRequestedAtDesc(userId, PageRequest.of(0, limit));
    }

    /**
     * Find expired jobs that need cleanup
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.status = 'COMPLETED' AND j.expiresAt < :expireTime")
    List<DataExportJob> findExpiredJobs(@Param("expireTime") LocalDateTime expireTime);
    
    /**
     * Find the most recent export job for a user
     */
    Optional<DataExportJob> findFirstByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find the most recent export job for a user by user ID
     */
    Optional<DataExportJob> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find expired export jobs
     */
    List<DataExportJob> findByExpiresAtBefore(LocalDateTime dateTime);
    
    /**
     * Find completed export jobs that haven't expired
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.status = 'COMPLETED' AND " +
           "(j.expiresAt IS NULL OR j.expiresAt > :currentTime)")
    List<DataExportJob> findActiveCompletedJobs(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find export jobs created between dates
     */
    List<DataExportJob> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find export jobs that need notification (completed but not notified)
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.status = 'COMPLETED' AND " +
           "j.notificationSent = false")
    List<DataExportJob> findJobsNeedingNotification();
    
    /**
     * Count export jobs by status
     */
    long countByStatus(ExportStatus status);
    
    /**
     * Count export jobs for a user
     */
    long countByUser(User user);
    
    /**
     * Count export jobs for a user by user ID
     */
    long countByUserId(Long userId);
    
    /**
     * Count export jobs created in the last N days
     */
    @Query("SELECT COUNT(j) FROM DataExportJob j WHERE j.createdAt >= :sinceDate")
    long countJobsCreatedSince(@Param("sinceDate") LocalDateTime sinceDate);
    
    /**
     * Check if user has any pending or processing export jobs
     */
    @Query("SELECT COUNT(j) > 0 FROM DataExportJob j WHERE j.user.id = :userId AND " +
           "j.status IN ('PENDING', 'PROCESSING')")
    boolean hasActiveExportJob(@Param("userId") Long userId);
    
    /**
     * Find jobs that have been processing for too long (stuck jobs)
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.status = 'PROCESSING' AND " +
           "j.startedAt < :cutoffTime")
    List<DataExportJob> findStuckJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Update export job status
     */
    @Modifying
    @Query("UPDATE DataExportJob j SET j.status = :status WHERE j.id = :jobId")
    void updateStatus(@Param("jobId") Long jobId, @Param("status") ExportStatus status);
    
    /**
     * Mark job as started
     */
    @Modifying
    @Query("UPDATE DataExportJob j SET j.status = 'PROCESSING', j.startedAt = :startTime " +
           "WHERE j.id = :jobId")
    void markAsStarted(@Param("jobId") Long jobId, @Param("startTime") LocalDateTime startTime);
    
    /**
     * Mark job as completed
     */
    @Modifying
    @Query("UPDATE DataExportJob j SET j.status = 'COMPLETED', j.completedAt = :completedTime, " +
           "j.filePath = :filePath, j.downloadUrl = :downloadUrl, j.fileSizeBytes = :fileSize " +
           "WHERE j.id = :jobId")
    void markAsCompleted(@Param("jobId") Long jobId, 
                        @Param("completedTime") LocalDateTime completedTime,
                        @Param("filePath") String filePath,
                        @Param("downloadUrl") String downloadUrl,
                        @Param("fileSize") Long fileSize);
    
    /**
     * Mark job as failed
     */
    @Modifying
    @Query("UPDATE DataExportJob j SET j.status = 'FAILED', j.completedAt = :failedTime, " +
           "j.errorMessage = :errorMessage WHERE j.id = :jobId")
    void markAsFailed(@Param("jobId") Long jobId, 
                     @Param("failedTime") LocalDateTime failedTime,
                     @Param("errorMessage") String errorMessage);
    
    /**
     * Mark notification as sent
     */
    @Modifying
    @Query("UPDATE DataExportJob j SET j.notificationSent = true WHERE j.id = :jobId")
    void markNotificationSent(@Param("jobId") Long jobId);
    
    /**
     * Delete expired export jobs
     */
    @Modifying
    @Query("DELETE FROM DataExportJob j WHERE j.expiresAt < :currentTime")
    void deleteExpiredJobs(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Delete old failed jobs (cleanup)
     */
    @Modifying
    @Query("DELETE FROM DataExportJob j WHERE j.status = 'FAILED' AND " +
           "j.createdAt < :cutoffDate")
    void deleteOldFailedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find jobs for cleanup (old completed jobs)
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.status = 'COMPLETED' AND " +
           "j.expiresAt < :currentTime")
    List<DataExportJob> findJobsForCleanup(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Get export statistics for a date range
     */
    @Query("SELECT j.status, COUNT(j) FROM DataExportJob j WHERE " +
           "j.createdAt BETWEEN :startDate AND :endDate GROUP BY j.status")
    List<Object[]> getExportStatistics(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get average processing time for completed jobs
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, j.startedAt, j.completedAt)) FROM DataExportJob j " +
           "WHERE j.status = 'COMPLETED' AND j.startedAt IS NOT NULL AND j.completedAt IS NOT NULL")
    Double getAverageProcessingTimeSeconds();
    
    /**
     * Find recent export jobs for monitoring
     */
    @Query("SELECT j FROM DataExportJob j WHERE j.createdAt >= :sinceDate " +
           "ORDER BY j.createdAt DESC")
    List<DataExportJob> findRecentJobs(@Param("sinceDate") LocalDateTime sinceDate);
    
    /**
     * Check rate limiting - count jobs created by user in time window
     */
    @Query("SELECT COUNT(j) FROM DataExportJob j WHERE j.user.id = :userId AND " +
           "j.createdAt >= :sinceTime")
    long countJobsByUserSince(@Param("userId") Long userId, @Param("sinceTime") LocalDateTime sinceTime);
}