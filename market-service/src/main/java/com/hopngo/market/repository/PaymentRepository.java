package com.hopngo.market.repository;

import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.PaymentProvider;
import com.hopngo.market.entity.PaymentStatus;
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

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    // Find payment by order
    Optional<Payment> findByOrder_Id(UUID orderId);
    
    // Find payment by transaction reference
    Optional<Payment> findByTransactionReference(String transactionReference);
    
    // Find payment by provider transaction ID
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
    
    // Find payment by payment intent ID
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    
    // Find payments by status
    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);
    
    // Find payments by provider
    Page<Payment> findByProviderOrderByCreatedAtDesc(PaymentProvider provider, Pageable pageable);
    
    // Find payments by provider and status
    Page<Payment> findByProviderAndStatusOrderByCreatedAtDesc(
        PaymentProvider provider, 
        PaymentStatus status, 
        Pageable pageable
    );
    
    // Find pending payments older than specified time
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime ORDER BY p.createdAt ASC")
    List<Payment> findPendingPaymentsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find processing payments older than specified time
    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' AND p.createdAt < :cutoffTime ORDER BY p.createdAt ASC")
    List<Payment> findProcessingPaymentsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find payments within date range
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    Page<Payment> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Find successful payments within date range
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.processedAt BETWEEN :startDate AND :endDate ORDER BY p.processedAt DESC")
    Page<Payment> findSuccessfulPaymentsByProcessedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Find failed payments within date range
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.processedAt BETWEEN :startDate AND :endDate ORDER BY p.processedAt DESC")
    Page<Payment> findFailedPaymentsByProcessedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Count payments by status
    @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
    List<Object[]> countPaymentsByStatus();
    
    // Count payments by provider
    @Query("SELECT p.provider, COUNT(p) FROM Payment p GROUP BY p.provider")
    List<Object[]> countPaymentsByProvider();
    
    // Calculate total successful payment amount
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCEEDED'")
    java.math.BigDecimal calculateTotalSuccessfulPaymentAmount();
    
    // Calculate successful payment amount by date range
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.processedAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal calculateSuccessfulPaymentAmountByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Calculate successful payment amount by provider
    @Query("SELECT p.provider, SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCEEDED' GROUP BY p.provider")
    List<Object[]> calculateSuccessfulPaymentAmountByProvider();
    
    // Find payments by multiple statuses
    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses ORDER BY p.createdAt DESC")
    Page<Payment> findByStatusIn(@Param("statuses") List<PaymentStatus> statuses, Pageable pageable);
    
    // Find payments that need webhook retry
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.webhookReceivedAt IS NULL AND p.processedAt < :cutoffTime ORDER BY p.processedAt ASC")
    List<Payment> findPaymentsNeedingWebhookRetry(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Calculate payment success rate by provider
    @Query("SELECT p.provider, " +
           "COUNT(CASE WHEN p.status = 'SUCCEEDED' THEN 1 END) * 100.0 / COUNT(*) as successRate " +
           "FROM Payment p GROUP BY p.provider")
    List<Object[]> calculateSuccessRateByProvider();
    
    // Calculate average processing time for successful payments
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, p.createdAt, p.processedAt)) FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.processedAt IS NOT NULL")
    Double calculateAverageProcessingTimeInSeconds();
    
    // Find payments with specific failure reasons
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.failureReason LIKE %:reason% ORDER BY p.processedAt DESC")
    Page<Payment> findFailedPaymentsByReason(@Param("reason") String reason, Pageable pageable);
    
    // Find recent payments for monitoring
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPayments(@Param("since") LocalDateTime since);
    
    // Find payments by currency
    Page<Payment> findByCurrencyOrderByCreatedAtDesc(String currency, Pageable pageable);
    
    // Find payments by amount range
    @Query("SELECT p FROM Payment p WHERE p.amount BETWEEN :minAmount AND :maxAmount ORDER BY p.createdAt DESC")
    Page<Payment> findByAmountBetween(
        @Param("minAmount") java.math.BigDecimal minAmount,
        @Param("maxAmount") java.math.BigDecimal maxAmount,
        Pageable pageable
    );
}