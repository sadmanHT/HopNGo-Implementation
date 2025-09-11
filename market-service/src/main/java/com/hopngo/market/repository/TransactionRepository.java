package com.hopngo.market.repository;

import com.hopngo.market.entity.LedgerEntry;
import com.hopngo.market.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Transaction entity operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    /**
     * Find transactions by type.
     */
    List<Transaction> findByTransactionTypeOrderByCreatedAtDesc(Transaction.TransactionType transactionType);
    
    /**
     * Find transactions by status.
     */
    List<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.TransactionStatus status);
    
    /**
     * Find transactions by reference type and ID.
     */
    List<Transaction> findByReferenceTypeAndReferenceIdOrderByCreatedAt(
            LedgerEntry.ReferenceType referenceType, UUID referenceId);
    
    /**
     * Find transaction by reference type and ID (expecting single result).
     */
    Optional<Transaction> findByReferenceTypeAndReferenceId(
            LedgerEntry.ReferenceType referenceType, UUID referenceId);
    
    /**
     * Find transactions by currency.
     */
    List<Transaction> findByCurrencyOrderByCreatedAtDesc(String currency);
    
    /**
     * Find transactions by date range.
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find transactions by created by user.
     */
    List<Transaction> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
    
    /**
     * Find pending transactions.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' " +
           "ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions();
    
    /**
     * Find completed transactions.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'COMPLETED' " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findCompletedTransactions();
    
    /**
     * Find failed transactions.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'FAILED' " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findFailedTransactions();
    
    /**
     * Count transactions by status.
     */
    long countByStatus(Transaction.TransactionStatus status);
    
    /**
     * Count transactions by type.
     */
    long countByTransactionType(Transaction.TransactionType transactionType);
    
    /**
     * Calculate total amount by status and currency.
     */
    @Query("SELECT COALESCE(SUM(t.totalAmountMinor), 0) FROM Transaction t " +
           "WHERE t.status = :status AND t.currency = :currency")
    Long calculateTotalAmountByStatusAndCurrency(@Param("status") Transaction.TransactionStatus status,
                                                @Param("currency") String currency);
    
    /**
     * Calculate total amount by type and currency.
     */
    @Query("SELECT COALESCE(SUM(t.totalAmountMinor), 0) FROM Transaction t " +
           "WHERE t.transactionType = :type AND t.currency = :currency")
    Long calculateTotalAmountByTypeAndCurrency(@Param("type") Transaction.TransactionType type,
                                              @Param("currency") String currency);
    
    /**
     * Find recent transactions.
     */
    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC LIMIT :limit")
    List<Transaction> findRecentTransactions(@Param("limit") int limit);
    
    /**
     * Find transactions by description containing text.
     */
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.description) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByDescriptionContainingIgnoreCase(@Param("searchText") String searchText);
    
    /**
     * Find transactions by amount range.
     */
    @Query("SELECT t FROM Transaction t WHERE t.totalAmountMinor BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByAmountRange(@Param("minAmount") Long minAmount,
                                       @Param("maxAmount") Long maxAmount);
    
    /**
     * Get daily transaction summary.
     */
    @Query("SELECT DATE(t.createdAt) as date, t.currency, t.transactionType, t.status, " +
           "COUNT(t) as count, SUM(t.totalAmountMinor) as total " +
           "FROM Transaction t " +
           "WHERE t.createdAt >= :startDate " +
           "GROUP BY DATE(t.createdAt), t.currency, t.transactionType, t.status " +
           "ORDER BY DATE(t.createdAt) DESC")
    List<Object[]> getDailyTransactionSummary(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get monthly transaction summary.
     */
    @Query("SELECT YEAR(t.createdAt) as year, MONTH(t.createdAt) as month, " +
           "t.currency, t.transactionType, t.status, " +
           "COUNT(t) as count, SUM(t.totalAmountMinor) as total " +
           "FROM Transaction t " +
           "WHERE t.createdAt >= :startDate " +
           "GROUP BY YEAR(t.createdAt), MONTH(t.createdAt), t.currency, t.transactionType, t.status " +
           "ORDER BY YEAR(t.createdAt) DESC, MONTH(t.createdAt) DESC")
    List<Object[]> getMonthlyTransactionSummary(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find transactions that need cleanup (old pending transactions).
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' " +
           "AND t.createdAt < :cutoffDate ORDER BY t.createdAt ASC")
    List<Transaction> findStaleTransactions(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Check if transaction exists for reference.
     */
    boolean existsByReferenceTypeAndReferenceId(LedgerEntry.ReferenceType referenceType, UUID referenceId);
    
    /**
     * Find transactions by multiple statuses.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN :statuses " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByStatusIn(@Param("statuses") List<Transaction.TransactionStatus> statuses);
    
    /**
     * Find transactions by type and status.
     */
    List<Transaction> findByTransactionTypeAndStatusOrderByCreatedAtDesc(
            Transaction.TransactionType transactionType, Transaction.TransactionStatus status);
    
    /**
     * Count transactions created today.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE DATE(t.createdAt) = CURRENT_DATE")
    long countTransactionsToday();
    
    /**
     * Get revenue summary for a date range.
     */
    @Query("SELECT t.currency, SUM(t.totalAmountMinor) as total " +
           "FROM Transaction t " +
           "WHERE t.transactionType = 'BOOKING_PAYMENT' AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY t.currency")
    List<Object[]> getRevenueSummary(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
}