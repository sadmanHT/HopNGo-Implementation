package com.hopngo.repository;

import com.hopngo.entity.LedgerEntry;
import com.hopngo.entity.LedgerEntry.AccountType;
import com.hopngo.entity.LedgerEntry.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    List<LedgerEntry> findByTransaction_Id(Long transactionId);
    
    List<LedgerEntry> findByOrder_Id(Long orderId);
    
    List<LedgerEntry> findByAccountType(AccountType accountType);
    
    List<LedgerEntry> findByEntryType(EntryType entryType);
    
    List<LedgerEntry> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.accountType = :accountType AND le.createdAt BETWEEN :startDate AND :endDate")
    List<LedgerEntry> findByAccountTypeAndDateRange(@Param("accountType") AccountType accountType,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.entryType = :entryType AND le.createdAt BETWEEN :startDate AND :endDate")
    List<LedgerEntry> findByEntryTypeAndDateRange(@Param("entryType") EntryType entryType,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    // Balance calculation queries
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.accountType = :accountType")
    BigDecimal calculateAccountBalance(@Param("accountType") AccountType accountType);
    
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.accountType = :accountType AND le.createdAt <= :asOfDate")
    BigDecimal calculateAccountBalanceAsOf(@Param("accountType") AccountType accountType,
                                          @Param("asOfDate") LocalDateTime asOfDate);
    
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.accountType = :accountType " +
           "AND le.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateAccountBalanceForPeriod(@Param("accountType") AccountType accountType,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    
    // Double-entry validation queries
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.transaction.id = :transactionId")
    BigDecimal validateTransactionBalance(@Param("transactionId") Long transactionId);
    
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.order.id = :orderId")
    BigDecimal validateOrderBalance(@Param("orderId") Long orderId);
    
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal validatePeriodBalance(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
    
    // Account activity queries
    @Query("SELECT le.accountType, SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END) as debits, " +
           "SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END) as credits " +
           "FROM LedgerEntry le WHERE le.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY le.accountType")
    List<Object[]> getAccountActivity(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
    
    // Monthly aggregation for reporting
    @Query("SELECT YEAR(le.createdAt) as year, MONTH(le.createdAt) as month, le.accountType, " +
           "SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END) as totalDebits, " +
           "SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END) as totalCredits " +
           "FROM LedgerEntry le " +
           "GROUP BY YEAR(le.createdAt), MONTH(le.createdAt), le.accountType " +
           "ORDER BY YEAR(le.createdAt) DESC, MONTH(le.createdAt) DESC")
    List<Object[]> getMonthlyLedgerSummary();
    
    // Integrity check queries
    @Query("SELECT le.transaction.id, SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) as balance " +
           "FROM LedgerEntry le WHERE le.transaction IS NOT NULL " +
           "GROUP BY le.transaction.id HAVING ABS(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END)) > 0.01")
    List<Object[]> findUnbalancedTransactions();
    
    @Query("SELECT le.order.id, SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) as balance " +
           "FROM LedgerEntry le WHERE le.order IS NOT NULL " +
           "GROUP BY le.order.id HAVING ABS(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END)) > 0.01")
    List<Object[]> findUnbalancedOrders();
    
    // Recent entries for monitoring
    @Query("SELECT le FROM LedgerEntry le WHERE le.createdAt >= :fromDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findRecentEntries(@Param("fromDate") LocalDateTime fromDate);
    
    // Entries by reference
    @Query("SELECT le FROM LedgerEntry le WHERE le.referenceId = :reference")
    List<LedgerEntry> findByReference(@Param("reference") String reference);
    
    // Daily balance snapshots
    @Query("SELECT DATE(le.createdAt) as entryDate, le.accountType, " +
           "SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) as dailyBalance " +
           "FROM LedgerEntry le WHERE le.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(le.createdAt), le.accountType " +
           "ORDER BY DATE(le.createdAt) DESC")
    List<Object[]> getDailyBalanceSnapshots(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    // Currency-specific queries
    @Query("SELECT DISTINCT le.currency FROM LedgerEntry le")
    List<String> findDistinctCurrencies();
    
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) " +
           "FROM LedgerEntry le WHERE le.accountType = :accountType AND le.currency = :currency")
    BigDecimal calculateAccountBalanceByCurrency(@Param("accountType") AccountType accountType,
                                                @Param("currency") String currency);
    
    // Missing methods for LedgerVerificationService
    @Query("SELECT le FROM LedgerEntry le WHERE le.transaction IS NULL AND le.order IS NULL")
    List<LedgerEntry> findOrphanedEntries();
    
    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountType = :accountType " +
           "AND le.entryType = :entryType AND le.createdAt < :beforeDate")
    BigDecimal sumByAccountTypeAndEntryTypeBeforeDate(@Param("accountType") AccountType accountType,
                                                     @Param("entryType") EntryType entryType,
                                                     @Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.entryType = :entryType")
    BigDecimal sumByEntryType(@Param("entryType") EntryType entryType);

    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountType = :accountType AND le.entryType = :entryType")
    BigDecimal sumByAccountTypeAndEntryType(
        @Param("accountType") AccountType accountType,
        @Param("entryType") EntryType entryType
    );
}