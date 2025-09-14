package com.hopngo.repository;

import com.hopngo.entity.Dispute;
import com.hopngo.entity.Dispute.DisputeStatus;
import com.hopngo.entity.Dispute.DisputeType;
import com.hopngo.entity.Dispute.DisputeReason;
import com.hopngo.entity.Transaction.PaymentProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    Optional<Dispute> findByDisputeId(String disputeId);
    
    Optional<Dispute> findByProviderDisputeId(String providerDisputeId);
    
    List<Dispute> findByTransactionId(Long transactionId);
    
    List<Dispute> findByStatus(DisputeStatus status);
    
    List<Dispute> findByDisputeType(DisputeType disputeType);
    
    List<Dispute> findByReason(DisputeReason reason);
    
    Page<Dispute> findByStatusAndDisputeType(DisputeStatus status, DisputeType disputeType, Pageable pageable);
    
    List<Dispute> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT d FROM Dispute d JOIN d.transaction t WHERE t.paymentProvider = :provider")
    List<Dispute> findByPaymentProvider(@Param("provider") PaymentProvider provider);
    
    @Query("SELECT d FROM Dispute d JOIN d.transaction t WHERE t.paymentProvider = :provider AND d.status = :status")
    List<Dispute> findByPaymentProviderAndStatus(@Param("provider") PaymentProvider provider,
                                                 @Param("status") DisputeStatus status);
    
    @Query("SELECT d FROM Dispute d WHERE d.status IN ('RECEIVED', 'UNDER_REVIEW', 'EVIDENCE_REQUIRED', 'EVIDENCE_SUBMITTED')")
    List<Dispute> findActiveDisputes();
    
    @Query("SELECT d FROM Dispute d WHERE d.status IN ('WON', 'LOST', 'ACCEPTED', 'EXPIRED')")
    List<Dispute> findResolvedDisputes();
    
    @Query("SELECT d FROM Dispute d WHERE d.evidenceDueBy IS NOT NULL AND d.evidenceDueBy <= :dueDate " +
           "AND d.evidenceSubmitted = false")
    List<Dispute> findDisputesWithUpcomingEvidenceDeadline(@Param("dueDate") LocalDateTime dueDate);
    
    @Query("SELECT d FROM Dispute d WHERE d.evidenceDueBy IS NOT NULL AND d.evidenceDueBy < :currentTime " +
           "AND d.evidenceSubmitted = false")
    List<Dispute> findDisputesWithOverdueEvidence(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT d FROM Dispute d WHERE d.fundsFrozen = true AND d.fundsReleasedAt IS NULL")
    List<Dispute> findDisputesWithFrozenFunds();
    
    @Query("SELECT d FROM Dispute d WHERE d.adminNotified = false AND d.createdAt <= :cutoffTime")
    List<Dispute> findDisputesRequiringAdminNotification(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT d FROM Dispute d WHERE d.providerNotified = false AND d.createdAt <= :cutoffTime")
    List<Dispute> findDisputesRequiringProviderNotification(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Statistics queries
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = :status AND d.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") DisputeStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.disputeType = :type AND d.createdAt BETWEEN :startDate AND :endDate")
    Long countByTypeAndDateRange(@Param("type") DisputeType type,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(d.disputedAmount) FROM Dispute d WHERE d.status = :status " +
           "AND d.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDisputedAmountByStatusAndDateRange(@Param("status") DisputeStatus status,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(d.disputeFee) FROM Dispute d WHERE d.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDisputeFeesByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    // Provider-specific statistics
    @Query("SELECT t.paymentProvider, COUNT(d) as disputeCount, SUM(d.disputedAmount) as totalAmount " +
           "FROM Dispute d JOIN d.transaction t WHERE d.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY t.paymentProvider")
    List<Object[]> getDisputeStatsByProvider(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT d.disputeType, COUNT(d) as disputeCount, SUM(d.disputedAmount) as totalAmount " +
           "FROM Dispute d WHERE d.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY d.disputeType ORDER BY disputeCount DESC")
    List<Object[]> getDisputeStatsByType(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT d.reason, COUNT(d) as disputeCount, SUM(d.disputedAmount) as totalAmount " +
           "FROM Dispute d WHERE d.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY d.reason ORDER BY disputeCount DESC")
    List<Object[]> getDisputeStatsByReason(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    // Win/Loss analysis
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = 'WON' AND d.resolvedAt BETWEEN :startDate AND :endDate")
    Long countWonDisputes(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = 'LOST' AND d.resolvedAt BETWEEN :startDate AND :endDate")
    Long countLostDisputes(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = 'ACCEPTED' AND d.resolvedAt BETWEEN :startDate AND :endDate")
    Long countAcceptedDisputes(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT (COUNT(CASE WHEN d.status = 'WON' THEN 1 END) * 100.0 / COUNT(d)) as winRate " +
           "FROM Dispute d WHERE d.status IN ('WON', 'LOST') " +
           "AND d.resolvedAt BETWEEN :startDate AND :endDate")
    Double calculateWinRate(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);
    
    // Response time analysis
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, d.createdAt, d.evidenceSubmittedAt)) FROM Dispute d " +
           "WHERE d.evidenceSubmitted = true AND d.evidenceSubmittedAt BETWEEN :startDate AND :endDate")
    Double getAverageEvidenceResponseTimeInHours(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, d.createdAt, d.resolvedAt)) FROM Dispute d " +
           "WHERE d.resolvedAt IS NOT NULL AND d.resolvedAt BETWEEN :startDate AND :endDate")
    Double getAverageResolutionTimeInHours(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    // High-value disputes
    @Query("SELECT d FROM Dispute d WHERE d.disputedAmount >= :threshold ORDER BY d.disputedAmount DESC")
    List<Dispute> findHighValueDisputes(@Param("threshold") BigDecimal threshold);
    
    // Recent activity
    @Query("SELECT d FROM Dispute d WHERE d.createdAt >= :fromDate ORDER BY d.createdAt DESC")
    List<Dispute> findRecentDisputes(@Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT d FROM Dispute d WHERE d.updatedAt >= :fromDate ORDER BY d.updatedAt DESC")
    List<Dispute> findRecentlyUpdatedDisputes(@Param("fromDate") LocalDateTime fromDate);
    
    // Monthly aggregation
    @Query("SELECT YEAR(d.createdAt) as year, MONTH(d.createdAt) as month, " +
           "COUNT(d) as disputeCount, SUM(d.disputedAmount) as totalAmount, " +
           "COUNT(CASE WHEN d.status = 'WON' THEN 1 END) as wonCount, " +
           "COUNT(CASE WHEN d.status = 'LOST' THEN 1 END) as lostCount " +
           "FROM Dispute d " +
           "GROUP BY YEAR(d.createdAt), MONTH(d.createdAt) " +
           "ORDER BY YEAR(d.createdAt) DESC, MONTH(d.createdAt) DESC")
    List<Object[]> getMonthlyDisputeSummary();
    
    // Currency-specific queries
    @Query("SELECT DISTINCT d.currency FROM Dispute d")
    List<String> findDistinctCurrencies();
    
    @Query("SELECT d FROM Dispute d WHERE d.currency = :currency AND d.createdAt BETWEEN :startDate AND :endDate")
    List<Dispute> findByCurrencyAndDateRange(@Param("currency") String currency,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    // Workflow status queries
    @Query("SELECT d FROM Dispute d WHERE d.status = 'EVIDENCE_REQUIRED' " +
           "AND d.evidenceDueBy BETWEEN :startDate AND :endDate")
    List<Dispute> findDisputesRequiringEvidenceInPeriod(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
}