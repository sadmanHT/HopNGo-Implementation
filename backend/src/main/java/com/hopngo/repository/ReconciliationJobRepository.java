package com.hopngo.repository;

import com.hopngo.entity.ReconciliationJob;
import com.hopngo.entity.ReconciliationJob.ReconciliationStatus;
import com.hopngo.entity.Transaction.PaymentProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, Long> {
    
    Optional<ReconciliationJob> findByJobId(String jobId);
    
    List<ReconciliationJob> findByStatus(ReconciliationStatus status);
    
    List<ReconciliationJob> findByProvider(PaymentProvider provider);
    
    Page<ReconciliationJob> findByProviderAndStatus(PaymentProvider provider, ReconciliationStatus status, Pageable pageable);
    
    List<ReconciliationJob> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<ReconciliationJob> findByReconciliationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.startDate = :periodStart AND rj.endDate = :periodEnd AND rj.provider = :provider")
    Optional<ReconciliationJob> findByPeriodAndProvider(@Param("periodStart") LocalDateTime periodStart,
                                                       @Param("periodEnd") LocalDateTime periodEnd,
                                                       @Param("provider") PaymentProvider provider);
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.status = :status AND rj.createdAt <= :cutoffTime")
    List<ReconciliationJob> findStaleJobs(@Param("status") ReconciliationStatus status,
                                         @Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.status = 'RUNNING' AND rj.startedAt <= :cutoffTime")
    List<ReconciliationJob> findLongRunningJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.discrepanciesFound > 0 ORDER BY rj.createdAt DESC")
    List<ReconciliationJob> findJobsWithDiscrepancies();
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.discrepanciesFound > 0 AND rj.provider = :provider ORDER BY rj.createdAt DESC")
    List<ReconciliationJob> findJobsWithDiscrepanciesByProvider(@Param("provider") PaymentProvider provider);
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.status = 'COMPLETED' ORDER BY rj.completedAt DESC")
    Page<ReconciliationJob> findCompletedJobsOrderByCompletedAt(Pageable pageable);
    
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.status = 'FAILED' ORDER BY rj.createdAt DESC")
    List<ReconciliationJob> findFailedJobs();
    
    // Statistics queries
    @Query("SELECT COUNT(rj) FROM ReconciliationJob rj WHERE rj.status = :status AND rj.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") ReconciliationStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT rj.provider, COUNT(rj) as jobCount, SUM(rj.discrepanciesFound) as totalDiscrepancies " +
           "FROM ReconciliationJob rj WHERE rj.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rj.provider")
    List<Object[]> getReconciliationStatsByProvider(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(rj.totalProviderTransactions) FROM ReconciliationJob rj WHERE rj.status = 'COMPLETED' " +
           "AND rj.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageTransactionsProcessed(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, rj.startedAt, rj.completedAt)) FROM ReconciliationJob rj " +
           "WHERE rj.status = 'COMPLETED' AND rj.startedAt IS NOT NULL AND rj.completedAt IS NOT NULL " +
           "AND rj.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageProcessingTimeInSeconds(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    // Recent activity
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.createdAt >= :fromDate ORDER BY rj.createdAt DESC")
    List<ReconciliationJob> findRecentJobs(@Param("fromDate") LocalDateTime fromDate);
    
    // Monthly aggregation
    @Query("SELECT YEAR(rj.createdAt) as year, MONTH(rj.createdAt) as month, rj.provider, " +
           "COUNT(rj) as jobCount, SUM(rj.totalProviderTransactions) as totalProcessed, " +
           "SUM(rj.discrepanciesFound) as totalDiscrepancies " +
           "FROM ReconciliationJob rj " +
           "GROUP BY YEAR(rj.createdAt), MONTH(rj.createdAt), rj.provider " +
           "ORDER BY YEAR(rj.createdAt) DESC, MONTH(rj.createdAt) DESC")
    List<Object[]> getMonthlyReconciliationSummary();
    
    // Last successful reconciliation per provider
    @Query("SELECT rj FROM ReconciliationJob rj WHERE rj.provider = :provider AND rj.status = 'COMPLETED' " +
           "ORDER BY rj.completedAt DESC")
    List<ReconciliationJob> findLastSuccessfulReconciliationByProvider(@Param("provider") PaymentProvider provider,
                                                                       Pageable pageable);
    
    // Jobs requiring attention
    @Query("SELECT rj FROM ReconciliationJob rj WHERE (rj.status = 'FAILED' OR rj.discrepanciesFound > 0) " +
           "AND rj.createdAt >= :fromDate ORDER BY rj.createdAt DESC")
    List<ReconciliationJob> findJobsRequiringAttention(@Param("fromDate") LocalDateTime fromDate);
}