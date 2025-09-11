package com.hopngo.market.repository;

import com.hopngo.market.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for LedgerEntry entity operations.
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    /**
     * Find entries by account ID ordered by creation date descending.
     */
    @Query("SELECT le FROM LedgerEntry le WHERE le.account.id = :accountId " +
           "ORDER BY le.createdAt DESC LIMIT :limit")
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId, 
                                                         @Param("limit") int limit);
    
    /**
     * Find entries by transaction ID.
     */
    List<LedgerEntry> findByTransactionIdOrderByCreatedAt(UUID transactionId);
    
    /**
     * Find entries by reference type and ID.
     */
    List<LedgerEntry> findByReferenceTypeAndReferenceIdOrderByCreatedAt(
            LedgerEntry.ReferenceType referenceType, UUID referenceId);
    
    /**
     * Find entries by account and date range.
     */
    @Query("SELECT le FROM LedgerEntry le WHERE le.account.id = :accountId " +
           "AND le.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find entries by account and entry type.
     */
    @Query("SELECT le FROM LedgerEntry le WHERE le.account.id = :accountId " +
           "AND le.entryType = :entryType ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountIdAndEntryType(@Param("accountId") UUID accountId,
                                                  @Param("entryType") LedgerEntry.EntryType entryType);
    
    /**
     * Calculate total debits for an account.
     */
    @Query("SELECT COALESCE(SUM(le.amountMinor), 0) FROM LedgerEntry le " +
           "WHERE le.account.id = :accountId AND le.entryType = 'DEBIT'")
    Long calculateTotalDebits(@Param("accountId") UUID accountId);
    
    /**
     * Calculate total credits for an account.
     */
    @Query("SELECT COALESCE(SUM(le.amountMinor), 0) FROM LedgerEntry le " +
           "WHERE le.account.id = :accountId AND le.entryType = 'CREDIT'")
    Long calculateTotalCredits(@Param("accountId") UUID accountId);
    
    /**
     * Calculate balance for an account (credits - debits).
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amountMinor " +
           "ELSE -le.amountMinor END), 0) FROM LedgerEntry le " +
           "WHERE le.account.id = :accountId")
    Long calculateAccountBalance(@Param("accountId") UUID accountId);
    
    /**
     * Find entries by currency.
     */
    List<LedgerEntry> findByCurrencyOrderByCreatedAtDesc(String currency);
    
    /**
     * Find entries by account and currency.
     */
    @Query("SELECT le FROM LedgerEntry le WHERE le.account.id = :accountId " +
           "AND le.currency = :currency ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountIdAndCurrency(@Param("accountId") UUID accountId,
                                                 @Param("currency") String currency);
    
    /**
     * Count entries by account.
     */
    long countByAccountId(UUID accountId);
    
    /**
     * Count entries by transaction.
     */
    long countByTransactionId(UUID transactionId);
    
    /**
     * Find recent entries across all accounts.
     */
    @Query("SELECT le FROM LedgerEntry le ORDER BY le.createdAt DESC LIMIT :limit")
    List<LedgerEntry> findRecentEntries(@Param("limit") int limit);
    
    /**
     * Find entries by description containing text.
     */
    @Query("SELECT le FROM LedgerEntry le WHERE LOWER(le.description) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
           "ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByDescriptionContainingIgnoreCase(@Param("searchText") String searchText);
    
    /**
     * Find entries by amount range.
     */
    @Query("SELECT le FROM LedgerEntry le WHERE le.amountMinor BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAmountRange(@Param("minAmount") Long minAmount,
                                       @Param("maxAmount") Long maxAmount);
    
    /**
     * Get daily transaction summary.
     */
    @Query("SELECT DATE(le.createdAt) as date, le.currency, le.entryType, " +
           "COUNT(le) as count, SUM(le.amountMinor) as total " +
           "FROM LedgerEntry le " +
           "WHERE le.createdAt >= :startDate " +
           "GROUP BY DATE(le.createdAt), le.currency, le.entryType " +
           "ORDER BY DATE(le.createdAt) DESC")
    List<Object[]> getDailyTransactionSummary(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Check if entry exists for reference.
     */
    boolean existsByReferenceTypeAndReferenceId(LedgerEntry.ReferenceType referenceType, UUID referenceId);
}