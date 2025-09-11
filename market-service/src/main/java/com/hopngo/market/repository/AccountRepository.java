package com.hopngo.market.repository;

import com.hopngo.market.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Account entity operations.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    /**
     * Find account by owner ID, owner type, and currency.
     */
    Optional<Account> findByOwnerIdAndOwnerTypeAndCurrency(UUID ownerId, 
                                                          Account.OwnerType ownerType, 
                                                          String currency);
    
    /**
     * Find all accounts by owner ID and owner type.
     */
    List<Account> findByOwnerIdAndOwnerTypeOrderByCurrency(UUID ownerId, Account.OwnerType ownerType);
    
    /**
     * Find accounts by owner type.
     */
    List<Account> findByOwnerTypeOrderByCreatedAt(Account.OwnerType ownerType);
    
    /**
     * Find accounts by account type.
     */
    List<Account> findByAccountTypeOrderByCreatedAt(Account.AccountType accountType);
    
    /**
     * Find accounts by currency.
     */
    List<Account> findByCurrencyOrderByCreatedAt(String currency);
    
    /**
     * Find accounts by status.
     */
    List<Account> findByStatusOrderByCreatedAt(Account.AccountStatus status);
    
    /**
     * Find active accounts.
     */
    @Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE' ORDER BY a.createdAt")
    List<Account> findActiveAccounts();
    
    /**
     * Find accounts with positive balance.
     */
    @Query("SELECT a FROM Account a WHERE a.balanceMinor > 0 ORDER BY a.balanceMinor DESC")
    List<Account> findAccountsWithPositiveBalance();
    
    /**
     * Find accounts with negative balance.
     */
    @Query("SELECT a FROM Account a WHERE a.balanceMinor < 0 ORDER BY a.balanceMinor ASC")
    List<Account> findAccountsWithNegativeBalance();
    
    /**
     * Find provider accounts with balance above threshold.
     */
    @Query("SELECT a FROM Account a WHERE a.ownerType = 'PROVIDER' " +
           "AND a.balanceMinor >= :threshold AND a.currency = :currency " +
           "ORDER BY a.balanceMinor DESC")
    List<Account> findProviderAccountsAboveThreshold(@Param("threshold") Long threshold,
                                                    @Param("currency") String currency);
    
    /**
     * Calculate total balance by currency.
     */
    @Query("SELECT COALESCE(SUM(a.balanceMinor), 0) FROM Account a WHERE a.currency = :currency")
    Long calculateTotalBalanceByCurrency(@Param("currency") String currency);
    
    /**
     * Calculate total balance by owner type and currency.
     */
    @Query("SELECT COALESCE(SUM(a.balanceMinor), 0) FROM Account a " +
           "WHERE a.ownerType = :ownerType AND a.currency = :currency")
    Long calculateTotalBalanceByOwnerTypeAndCurrency(@Param("ownerType") Account.OwnerType ownerType,
                                                    @Param("currency") String currency);
    
    /**
     * Count accounts by owner type.
     */
    long countByOwnerType(Account.OwnerType ownerType);
    
    /**
     * Count accounts by status.
     */
    long countByStatus(Account.AccountStatus status);
    
    /**
     * Count accounts by currency.
     */
    long countByCurrency(String currency);
    
    /**
     * Find accounts by balance range.
     */
    @Query("SELECT a FROM Account a WHERE a.balanceMinor BETWEEN :minBalance AND :maxBalance " +
           "ORDER BY a.balanceMinor DESC")
    List<Account> findByBalanceRange(@Param("minBalance") Long minBalance,
                                    @Param("maxBalance") Long maxBalance);
    
    /**
     * Find accounts created in date range.
     */
    @Query("SELECT a FROM Account a WHERE a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    List<Account> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find accounts updated in date range.
     */
    @Query("SELECT a FROM Account a WHERE a.updatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.updatedAt DESC")
    List<Account> findByUpdatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find platform accounts.
     */
    @Query("SELECT a FROM Account a WHERE a.ownerType = 'PLATFORM' ORDER BY a.currency")
    List<Account> findPlatformAccounts();
    
    /**
     * Find user accounts.
     */
    @Query("SELECT a FROM Account a WHERE a.ownerType = 'USER' ORDER BY a.createdAt DESC")
    List<Account> findUserAccounts();
    
    /**
     * Find provider accounts.
     */
    @Query("SELECT a FROM Account a WHERE a.ownerType = 'PROVIDER' ORDER BY a.createdAt DESC")
    List<Account> findProviderAccounts();
    
    /**
     * Get account balance summary by currency.
     */
    @Query("SELECT a.currency, a.ownerType, COUNT(a) as accountCount, " +
           "SUM(a.balanceMinor) as totalBalance, AVG(a.balanceMinor) as avgBalance " +
           "FROM Account a WHERE a.status = 'ACTIVE' " +
           "GROUP BY a.currency, a.ownerType " +
           "ORDER BY a.currency, a.ownerType")
    List<Object[]> getBalanceSummaryByCurrency();
    
    /**
     * Find accounts that haven't been updated recently.
     */
    @Query("SELECT a FROM Account a WHERE a.updatedAt < :cutoffDate " +
           "ORDER BY a.updatedAt ASC")
    List<Account> findStaleAccounts(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Check if account exists for owner.
     */
    boolean existsByOwnerIdAndOwnerTypeAndCurrency(UUID ownerId, 
                                                  Account.OwnerType ownerType, 
                                                  String currency);
    
    /**
     * Find accounts by multiple owner IDs.
     */
    @Query("SELECT a FROM Account a WHERE a.ownerId IN :ownerIds " +
           "AND a.ownerType = :ownerType ORDER BY a.createdAt DESC")
    List<Account> findByOwnerIdsAndOwnerType(@Param("ownerIds") List<UUID> ownerIds,
                                            @Param("ownerType") Account.OwnerType ownerType);
    
    /**
     * Find accounts with reserved balance.
     */
    @Query("SELECT a FROM Account a WHERE a.reservedBalanceMinor > 0 " +
           "ORDER BY a.reservedBalanceMinor DESC")
    List<Account> findAccountsWithReservedBalance();
    
    /**
     * Calculate total reserved balance by currency.
     */
    @Query("SELECT COALESCE(SUM(a.reservedBalanceMinor), 0) FROM Account a " +
           "WHERE a.currency = :currency")
    Long calculateTotalReservedBalanceByCurrency(@Param("currency") String currency);
    
    /**
     * Find accounts by account type and status.
     */
    List<Account> findByAccountTypeAndStatusOrderByCreatedAt(Account.AccountType accountType,
                                                            Account.AccountStatus status);
    
    /**
     * Get daily account creation summary.
     */
    @Query("SELECT DATE(a.createdAt) as date, a.ownerType, a.currency, COUNT(a) as count " +
           "FROM Account a WHERE a.createdAt >= :startDate " +
           "GROUP BY DATE(a.createdAt), a.ownerType, a.currency " +
           "ORDER BY DATE(a.createdAt) DESC")
    List<Object[]> getDailyAccountCreationSummary(@Param("startDate") LocalDateTime startDate);
}