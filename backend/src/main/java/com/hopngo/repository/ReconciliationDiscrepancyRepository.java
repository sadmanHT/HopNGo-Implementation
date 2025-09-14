package com.hopngo.repository;

import com.hopngo.entity.ReconciliationDiscrepancy;
import com.hopngo.entity.ReconciliationDiscrepancy.DiscrepancySeverity;
import com.hopngo.entity.ReconciliationDiscrepancy.DiscrepancyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancy, Long> {
    
    List<ReconciliationDiscrepancy> findByReconciliationJobId(Long reconciliationJobId);
    
    List<ReconciliationDiscrepancy> findByTransactionId(Long transactionId);
    
    List<ReconciliationDiscrepancy> findByDiscrepancyType(DiscrepancyType discrepancyType);
    
    List<ReconciliationDiscrepancy> findBySeverity(DiscrepancySeverity severity);
    
    Page<ReconciliationDiscrepancy> findByDiscrepancyTypeAndSeverity(DiscrepancyType discrepancyType,
                                                                     DiscrepancySeverity severity,
                                                                     Pageable pageable);
    
    List<ReconciliationDiscrepancy> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.resolved = false")
    List<ReconciliationDiscrepancy> findUnresolvedDiscrepancies();
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.resolved = false AND rd.severity = :severity")
    List<ReconciliationDiscrepancy> findUnresolvedDiscrepanciesBySeverity(@Param("severity") DiscrepancySeverity severity);
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.resolved = false " +
           "AND rd.createdAt <= :cutoffDate ORDER BY rd.severity DESC, rd.createdAt ASC")
    List<ReconciliationDiscrepancy> findOldUnresolvedDiscrepancies(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.resolved = true " +
           "AND rd.resolvedAt BETWEEN :startDate AND :endDate")
    List<ReconciliationDiscrepancy> findResolvedDiscrepanciesByDateRange(@Param("startDate") LocalDateTime startDate,
                                                                         @Param("endDate") LocalDateTime endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(rd) FROM ReconciliationDiscrepancy rd WHERE rd.discrepancyType = :type " +
           "AND rd.createdAt BETWEEN :startDate AND :endDate")
    Long countByTypeAndDateRange(@Param("type") DiscrepancyType type,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(rd) FROM ReconciliationDiscrepancy rd WHERE rd.severity = :severity " +
           "AND rd.createdAt BETWEEN :startDate AND :endDate")
    Long countBySeverityAndDateRange(@Param("severity") DiscrepancySeverity severity,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(ABS(rd.amountDifference)) FROM ReconciliationDiscrepancy rd " +
           "WHERE rd.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountDifferenceByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT rd.discrepancyType, COUNT(rd) as discrepancyCount, SUM(ABS(rd.amountDifference)) as totalAmount " +
           "FROM ReconciliationDiscrepancy rd WHERE rd.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rd.discrepancyType ORDER BY discrepancyCount DESC")
    List<Object[]> getDiscrepancyStatsByType(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT rd.severity, COUNT(rd) as discrepancyCount, SUM(ABS(rd.amountDifference)) as totalAmount " +
           "FROM ReconciliationDiscrepancy rd WHERE rd.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rd.severity ORDER BY rd.severity DESC")
    List<Object[]> getDiscrepancyStatsBySeverity(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    // Resolution tracking
    @Query("SELECT COUNT(rd) FROM ReconciliationDiscrepancy rd WHERE rd.resolved = true " +
           "AND rd.resolvedAt BETWEEN :startDate AND :endDate")
    Long countResolvedByDateRange(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, rd.createdAt, rd.resolvedAt)) FROM ReconciliationDiscrepancy rd " +
           "WHERE rd.resolved = true AND rd.resolvedAt BETWEEN :startDate AND :endDate")
    Double getAverageResolutionTimeInHours(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    // High-impact discrepancies
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE ABS(rd.amountDifference) >= :threshold " +
           "ORDER BY ABS(rd.amountDifference) DESC")
    List<ReconciliationDiscrepancy> findHighImpactDiscrepancies(@Param("threshold") BigDecimal threshold);
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.severity = 'CRITICAL' AND rd.resolved = false " +
           "ORDER BY rd.createdAt ASC")
    List<ReconciliationDiscrepancy> findCriticalUnresolvedDiscrepancies();
    
    // Monthly aggregation
    @Query("SELECT YEAR(rd.createdAt) as year, MONTH(rd.createdAt) as month, rd.discrepancyType, " +
           "COUNT(rd) as discrepancyCount, SUM(ABS(rd.amountDifference)) as totalAmount " +
           "FROM ReconciliationDiscrepancy rd " +
           "GROUP BY YEAR(rd.createdAt), MONTH(rd.createdAt), rd.discrepancyType " +
           "ORDER BY YEAR(rd.createdAt) DESC, MONTH(rd.createdAt) DESC")
    List<Object[]> getMonthlyDiscrepancySummary();
    
    // Recent activity
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.createdAt >= :fromDate " +
           "ORDER BY rd.severity DESC, rd.createdAt DESC")
    List<ReconciliationDiscrepancy> findRecentDiscrepancies(@Param("fromDate") LocalDateTime fromDate);
    
    // Support ticket correlation
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.supportTicketId IS NOT NULL")
    List<ReconciliationDiscrepancy> findDiscrepanciesWithSupportTickets();
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd WHERE rd.supportTicketId IS NULL " +
           "AND rd.severity IN ('HIGH', 'CRITICAL') AND rd.resolved = false")
    List<ReconciliationDiscrepancy> findHighSeverityDiscrepanciesWithoutTickets();
    
    // Provider-specific queries (through reconciliation job)
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd " +
           "JOIN rd.reconciliationJob rj WHERE rj.provider = :provider")
    List<ReconciliationDiscrepancy> findByProvider(@Param("provider") String provider);
    
    @Query("SELECT rd FROM ReconciliationDiscrepancy rd " +
           "JOIN rd.reconciliationJob rj WHERE rj.provider = :provider AND rd.resolved = false")
    List<ReconciliationDiscrepancy> findUnresolvedByProvider(@Param("provider") String provider);
}