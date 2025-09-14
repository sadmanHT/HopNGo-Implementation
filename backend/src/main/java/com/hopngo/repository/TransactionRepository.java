package com.hopngo.repository;

import com.hopngo.entity.Transaction;
import com.hopngo.entity.Transaction.PaymentProvider;
import com.hopngo.entity.Transaction.TransactionStatus;
import com.hopngo.entity.Transaction.TransactionType;
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
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionId(String transactionId);
    
    Optional<Transaction> findByProviderTransactionId(String providerTransactionId);
    
    List<Transaction> findByOrderId(Long orderId);
    
    List<Transaction> findByStatus(TransactionStatus status);
    
    List<Transaction> findByPaymentProvider(PaymentProvider provider);
    
    List<Transaction> findByTransactionType(TransactionType type);
    
    Page<Transaction> findByPaymentProviderAndStatus(PaymentProvider provider, TransactionStatus status, Pageable pageable);
    
    List<Transaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findSuccessfulByDateRange(@Param("status") TransactionStatus status,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate AND t.status = :status")
    List<Transaction> findByDateRangeAndStatus(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("status") TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate AND t.paymentProvider = :provider")
    List<Transaction> findByDateRangeAndProvider(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("provider") PaymentProvider provider);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.paymentProvider = :provider AND t.status = :status")
    List<Transaction> findByDateRangeAndProviderAndStatus(@Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate,
                                                          @Param("provider") PaymentProvider provider,
                                                          @Param("status") TransactionStatus status);
    
    // Aggregation queries for financial reporting
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByStatusAndDateRange(@Param("status") TransactionStatus status,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.fee) FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFeeByStatusAndDateRange(@Param("status") TransactionStatus status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.paymentProvider = :provider AND t.status = :status " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByProviderAndStatusAndDateRange(@Param("provider") PaymentProvider provider,
                                                        @Param("status") TransactionStatus status,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.fee) FROM Transaction t WHERE t.paymentProvider = :provider AND t.status = :status " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFeeByProviderAndStatusAndDateRange(@Param("provider") PaymentProvider provider,
                                                     @Param("status") TransactionStatus status,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") TransactionStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.paymentProvider = :provider AND t.status = :status " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countByProviderAndStatusAndDateRange(@Param("provider") PaymentProvider provider,
                                              @Param("status") TransactionStatus status,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    // Refund-specific queries
    @Query("SELECT t FROM Transaction t WHERE t.transactionType = :type AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findRefundsByDateRange(@Param("type") TransactionType type,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = :type AND t.status = :status " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumRefundAmountByDateRange(@Param("type") TransactionType type,
                                         @Param("status") TransactionStatus status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionType = :type AND t.status = :status " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countRefundsByDateRange(@Param("type") TransactionType type,
                                @Param("status") TransactionStatus status,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    // Currency-specific queries
    @Query("SELECT DISTINCT t.currency FROM Transaction t")
    List<String> findDistinctCurrencies();
    
    @Query("SELECT t FROM Transaction t WHERE t.currency = :currency AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByCurrencyAndDateRange(@Param("currency") String currency,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    // Failed transaction analysis
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt >= :fromDate ORDER BY t.createdAt DESC")
    List<Transaction> findRecentFailedTransactions(@Param("status") TransactionStatus status,
                                                  @Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT t.paymentProvider, COUNT(t) as failureCount FROM Transaction t " +
           "WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY t.paymentProvider ORDER BY failureCount DESC")
    List<Object[]> getFailureCountByProvider(@Param("status") TransactionStatus status,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    // Monthly aggregation for reporting
    @Query("SELECT YEAR(t.createdAt) as year, MONTH(t.createdAt) as month, t.paymentProvider, " +
           "COUNT(t) as transactionCount, SUM(t.amount) as totalAmount, SUM(t.fee) as totalFee " +
           "FROM Transaction t WHERE t.status = :status " +
           "GROUP BY YEAR(t.createdAt), MONTH(t.createdAt), t.paymentProvider " +
           "ORDER BY YEAR(t.createdAt) DESC, MONTH(t.createdAt) DESC")
    List<Object[]> getMonthlyTransactionSummaryByProvider(@Param("status") TransactionStatus status);
    
    // Reconciliation queries
    @Query("SELECT t FROM Transaction t WHERE t.reconciledAt IS NULL AND t.status = :status " +
           "AND t.createdAt <= :cutoffDate")
    List<Transaction> findUnreconciledTransactions(@Param("status") TransactionStatus status,
                                                  @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.reconciledAt BETWEEN :startDate AND :endDate")
    List<Transaction> findReconciledTransactionsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);
    
    // High-value transaction monitoring
    @Query("SELECT t FROM Transaction t WHERE t.amount >= :threshold AND t.createdAt >= :fromDate " +
           "ORDER BY t.amount DESC")
    List<Transaction> findHighValueTransactions(@Param("threshold") BigDecimal threshold,
                                               @Param("fromDate") LocalDateTime fromDate);
    
    // Provider-specific reconciliation
    @Query("SELECT t FROM Transaction t WHERE t.paymentProvider = :provider AND t.reconciledAt IS NULL " +
           "AND t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findUnreconciledTransactionsByProvider(@Param("provider") PaymentProvider provider,
                                                             @Param("status") TransactionStatus status,
                                                             @Param("startDate") LocalDateTime startDate,
                                                             @Param("endDate") LocalDateTime endDate);
}