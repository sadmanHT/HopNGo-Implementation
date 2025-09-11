package com.hopngo.market.repository;

import com.hopngo.market.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Payout entity operations.
 */
@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    
    /**
     * Find payouts by provider ID.
     */
    Page<Payout> findByProviderIdOrderByRequestedAtDesc(UUID providerId, Pageable pageable);
    
    /**
     * Find payouts by provider ID and status.
     */
    List<Payout> findByProviderIdAndStatusOrderByRequestedAtDesc(UUID providerId, Payout.PayoutStatus status);
    
    /**
     * Find payouts by status.
     */
    Page<Payout> findByStatusOrderByRequestedAtAsc(Payout.PayoutStatus status, Pageable pageable);
    
    /**
     * Find payouts by method.
     */
    List<Payout> findByMethodOrderByRequestedAtDesc(Payout.PayoutMethod method);
    
    /**
     * Find payouts by currency.
     */
    List<Payout> findByCurrencyOrderByRequestedAtDesc(String currency);
    
    /**
     * Find payouts by reference number.
     */
    Optional<Payout> findByReferenceNumber(String referenceNumber);
    
    /**
     * Find payouts by external transaction ID.
     */
    Optional<Payout> findByExternalTransactionId(String externalTransactionId);
    
    /**
     * Find payouts by date range.
     */
    @Query("SELECT p FROM Payout p WHERE p.requestedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.requestedAt DESC")
    List<Payout> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find payouts by provider and date range.
     */
    @Query("SELECT p FROM Payout p WHERE p.providerId = :providerId " +
           "AND p.requestedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.requestedAt DESC")
    List<Payout> findByProviderIdAndDateRange(@Param("providerId") UUID providerId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find pending payouts for provider.
     */
    @Query("SELECT p FROM Payout p WHERE p.providerId = :providerId " +
           "AND p.status IN ('PENDING', 'APPROVED', 'PROCESSING') " +
           "ORDER BY p.requestedAt ASC")
    List<Payout> findPendingPayoutsByProvider(@Param("providerId") UUID providerId);
    
    /**
     * Calculate total pending payout amount for provider.
     */
    @Query("SELECT COALESCE(SUM(p.amountMinor), 0) FROM Payout p " +
           "WHERE p.providerId = :providerId AND p.currency = :currency " +
           "AND p.status IN ('PENDING', 'APPROVED', 'PROCESSING')")
    Long calculatePendingPayoutAmount(@Param("providerId") UUID providerId,
                                     @Param("currency") String currency);
    
    /**
     * Calculate total paid amount for provider.
     */
    @Query("SELECT COALESCE(SUM(p.amountMinor), 0) FROM Payout p " +
           "WHERE p.providerId = :providerId AND p.currency = :currency " +
           "AND p.status = 'PAID'")
    Long calculateTotalPaidAmount(@Param("providerId") UUID providerId,
                                 @Param("currency") String currency);
    
    /**
     * Count payouts by status.
     */
    long countByStatus(Payout.PayoutStatus status);
    
    /**
     * Count payouts by provider and status.
     */
    long countByProviderIdAndStatus(UUID providerId, Payout.PayoutStatus status);
    
    /**
     * Count payouts by method.
     */
    long countByMethod(Payout.PayoutMethod method);
    
    /**
     * Find payouts requiring approval.
     */
    @Query("SELECT p FROM Payout p WHERE p.status = 'PENDING' " +
           "ORDER BY p.requestedAt ASC")
    Page<Payout> findPayoutsRequiringApproval(Pageable pageable);
    
    /**
     * Find approved payouts ready for processing.
     */
    @Query("SELECT p FROM Payout p WHERE p.status = 'APPROVED' " +
           "ORDER BY p.approvedAt ASC")
    List<Payout> findApprovedPayoutsForProcessing();
    
    /**
     * Find processing payouts (for monitoring).
     */
    @Query("SELECT p FROM Payout p WHERE p.status = 'PROCESSING' " +
           "ORDER BY p.processedAt ASC")
    List<Payout> findProcessingPayouts();
    
    /**
     * Find failed payouts.
     */
    @Query("SELECT p FROM Payout p WHERE p.status = 'FAILED' " +
           "ORDER BY p.executedAt DESC")
    List<Payout> findFailedPayouts();
    
    /**
     * Find payouts by amount range.
     */
    @Query("SELECT p FROM Payout p WHERE p.amountMinor BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY p.requestedAt DESC")
    List<Payout> findByAmountRange(@Param("minAmount") Long minAmount,
                                  @Param("maxAmount") Long maxAmount);
    
    /**
     * Find recent payouts.
     */
    @Query("SELECT p FROM Payout p ORDER BY p.requestedAt DESC LIMIT :limit")
    List<Payout> findRecentPayouts(@Param("limit") int limit);
    
    /**
     * Get payout statistics by status.
     */
    @Query("SELECT p.status, p.currency, COUNT(p) as count, SUM(p.amountMinor) as total " +
           "FROM Payout p GROUP BY p.status, p.currency " +
           "ORDER BY p.status, p.currency")
    List<Object[]> getPayoutStatisticsByStatus();
    
    /**
     * Get payout statistics by method.
     */
    @Query("SELECT p.method, p.currency, COUNT(p) as count, SUM(p.amountMinor) as total " +
           "FROM Payout p GROUP BY p.method, p.currency " +
           "ORDER BY p.method, p.currency")
    List<Object[]> getPayoutStatisticsByMethod();
    
    /**
     * Get daily payout summary.
     */
    @Query("SELECT DATE(p.requestedAt) as date, p.currency, p.status, " +
           "COUNT(p) as count, SUM(p.amountMinor) as total " +
           "FROM Payout p WHERE p.requestedAt >= :startDate " +
           "GROUP BY DATE(p.requestedAt), p.currency, p.status " +
           "ORDER BY DATE(p.requestedAt) DESC")
    List<Object[]> getDailyPayoutSummary(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get monthly payout summary.
     */
    @Query("SELECT YEAR(p.requestedAt) as year, MONTH(p.requestedAt) as month, " +
           "p.currency, p.status, COUNT(p) as count, SUM(p.amountMinor) as total " +
           "FROM Payout p WHERE p.requestedAt >= :startDate " +
           "GROUP BY YEAR(p.requestedAt), MONTH(p.requestedAt), p.currency, p.status " +
           "ORDER BY YEAR(p.requestedAt) DESC, MONTH(p.requestedAt) DESC")
    List<Object[]> getMonthlyPayoutSummary(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find provider payout history.
     */
    @Query("SELECT p FROM Payout p WHERE p.providerId = :providerId " +
           "AND p.currency = :currency ORDER BY p.requestedAt DESC")
    Page<Payout> findProviderPayoutHistory(@Param("providerId") UUID providerId,
                                          @Param("currency") String currency,
                                          Pageable pageable);
    
    /**
     * Find payouts by multiple providers.
     */
    @Query("SELECT p FROM Payout p WHERE p.providerId IN :providerIds " +
           "ORDER BY p.requestedAt DESC")
    List<Payout> findByProviderIds(@Param("providerIds") List<UUID> providerIds);
    
    /**
     * Find payouts by multiple statuses.
     */
    @Query("SELECT p FROM Payout p WHERE p.status IN :statuses " +
           "ORDER BY p.requestedAt DESC")
    List<Payout> findByStatusIn(@Param("statuses") List<Payout.PayoutStatus> statuses);
    
    /**
     * Find overdue processing payouts.
     */
    @Query("SELECT p FROM Payout p WHERE p.status = 'PROCESSING' " +
           "AND p.processedAt < :cutoffTime ORDER BY p.processedAt ASC")
    List<Payout> findOverdueProcessingPayouts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Check if provider has pending payouts.
     */
    @Query("SELECT COUNT(p) > 0 FROM Payout p WHERE p.providerId = :providerId " +
           "AND p.status IN ('PENDING', 'APPROVED', 'PROCESSING')")
    boolean hasProviderPendingPayouts(@Param("providerId") UUID providerId);
    
    /**
     * Find payouts by approved by user.
     */
    List<Payout> findByApprovedByOrderByApprovedAtDesc(UUID approvedBy);
    
    /**
     * Find payouts by processed by user.
     */
    List<Payout> findByProcessedByOrderByProcessedAtDesc(UUID processedBy);
    
    /**
     * Get provider earnings summary.
     */
    @Query("SELECT p.providerId, p.currency, " +
           "COUNT(CASE WHEN p.status = 'PAID' THEN 1 END) as paidCount, " +
           "SUM(CASE WHEN p.status = 'PAID' THEN p.amountMinor ELSE 0 END) as totalPaid, " +
           "COUNT(CASE WHEN p.status IN ('PENDING', 'APPROVED', 'PROCESSING') THEN 1 END) as pendingCount, " +
           "SUM(CASE WHEN p.status IN ('PENDING', 'APPROVED', 'PROCESSING') THEN p.amountMinor ELSE 0 END) as totalPending " +
           "FROM Payout p WHERE p.providerId IN :providerIds " +
           "GROUP BY p.providerId, p.currency")
    List<Object[]> getProviderEarningsSummary(@Param("providerIds") List<UUID> providerIds);
    
    /**
     * Find payouts needing attention (old pending/processing).
     */
    @Query("SELECT p FROM Payout p WHERE " +
           "(p.status = 'PENDING' AND p.requestedAt < :pendingCutoff) OR " +
           "(p.status = 'PROCESSING' AND p.processedAt < :processingCutoff) " +
           "ORDER BY p.requestedAt ASC")
    List<Payout> findPayoutsNeedingAttention(@Param("pendingCutoff") LocalDateTime pendingCutoff,
                                            @Param("processingCutoff") LocalDateTime processingCutoff);
}