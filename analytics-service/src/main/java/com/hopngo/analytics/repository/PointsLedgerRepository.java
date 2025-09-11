package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.PointsLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {

    /**
     * Find all transactions for a user ordered by creation date
     */
    List<PointsLedger> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find transactions for a user with pagination
     */
    Page<PointsLedger> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find transactions by user and transaction type
     */
    List<PointsLedger> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(String userId, PointsLedger.TransactionType transactionType);

    /**
     * Find transactions by source
     */
    List<PointsLedger> findBySourceOrderByCreatedAtDesc(String source);

    /**
     * Find transactions by source and source ID
     */
    List<PointsLedger> findBySourceAndSourceIdOrderByCreatedAtDesc(String source, String sourceId);

    /**
     * Find transactions within date range
     */
    List<PointsLedger> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find user transactions within date range
     */
    List<PointsLedger> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(String userId, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find expired points that haven't been processed
     */
    @Query("SELECT pl FROM PointsLedger pl WHERE pl.expiresAt < :now AND pl.transactionType = 'EARNED'")
    List<PointsLedger> findExpiredEarnedPoints(@Param("now") OffsetDateTime now);

    /**
     * Get current points balance for a user
     */
    @Query("SELECT COALESCE(SUM(pl.pointsAmount), 0) FROM PointsLedger pl " +
           "WHERE pl.userId = :userId AND (pl.expiresAt IS NULL OR pl.expiresAt > :now)")
    Integer getCurrentPointsBalance(@Param("userId") String userId, @Param("now") OffsetDateTime now);

    /**
     * Get the latest balance for a user
     */
    @Query("SELECT pl.balanceAfter FROM PointsLedger pl " +
           "WHERE pl.userId = :userId " +
           "ORDER BY pl.createdAt DESC, pl.id DESC " +
           "LIMIT 1")
    Optional<Integer> getLatestBalance(@Param("userId") String userId);

    /**
     * Get points summary for a user
     */
    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN pl.transactionType = 'EARNED' THEN pl.pointsAmount ELSE 0 END), 0) as totalEarned, " +
           "COALESCE(SUM(CASE WHEN pl.transactionType = 'REDEEMED' THEN ABS(pl.pointsAmount) ELSE 0 END), 0) as totalRedeemed, " +
           "COALESCE(SUM(CASE WHEN pl.transactionType = 'EXPIRED' THEN ABS(pl.pointsAmount) ELSE 0 END), 0) as totalExpired, " +
           "COUNT(CASE WHEN pl.transactionType = 'EARNED' THEN 1 END) as earningTransactions, " +
           "COUNT(CASE WHEN pl.transactionType = 'REDEEMED' THEN 1 END) as redemptionTransactions " +
           "FROM PointsLedger pl WHERE pl.userId = :userId")
    Object[] getPointsSummaryByUser(@Param("userId") String userId);

    /**
     * Get points earned by source for a user
     */
    @Query("SELECT pl.source, SUM(pl.pointsAmount) as totalPoints, COUNT(pl) as transactionCount " +
           "FROM PointsLedger pl " +
           "WHERE pl.userId = :userId AND pl.transactionType = 'EARNED' " +
           "GROUP BY pl.source " +
           "ORDER BY totalPoints DESC")
    List<Object[]> getPointsEarnedBySource(@Param("userId") String userId);

    /**
     * Get daily points metrics for date range
     */
    @Query("SELECT " +
           "DATE(pl.createdAt) as date, " +
           "SUM(CASE WHEN pl.transactionType = 'EARNED' THEN pl.pointsAmount ELSE 0 END) as pointsEarned, " +
           "SUM(CASE WHEN pl.transactionType = 'REDEEMED' THEN ABS(pl.pointsAmount) ELSE 0 END) as pointsRedeemed, " +
           "COUNT(DISTINCT pl.userId) as activeUsers " +
           "FROM PointsLedger pl " +
           "WHERE pl.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(pl.createdAt) " +
           "ORDER BY date DESC")
    List<Object[]> getDailyPointsMetrics(@Param("startDate") OffsetDateTime startDate, 
                                        @Param("endDate") OffsetDateTime endDate);

    /**
     * Get top point earners
     */
    @Query("SELECT pl.userId, SUM(pl.pointsAmount) as totalPoints " +
           "FROM PointsLedger pl " +
           "WHERE pl.transactionType = 'EARNED' " +
           "GROUP BY pl.userId " +
           "ORDER BY totalPoints DESC")
    List<Object[]> getTopPointEarners(Pageable pageable);

    /**
     * Get points distribution by source
     */
    @Query("SELECT pl.source, " +
           "SUM(CASE WHEN pl.transactionType = 'EARNED' THEN pl.pointsAmount ELSE 0 END) as totalEarned, " +
           "COUNT(DISTINCT pl.userId) as uniqueUsers, " +
           "COUNT(pl) as totalTransactions " +
           "FROM PointsLedger pl " +
           "WHERE pl.transactionType = 'EARNED' " +
           "GROUP BY pl.source " +
           "ORDER BY totalEarned DESC")
    List<Object[]> getPointsDistributionBySource();

    /**
     * Count total transactions for a user
     */
    long countByUserId(String userId);

    /**
     * Count transactions by user and type
     */
    long countByUserIdAndTransactionType(String userId, PointsLedger.TransactionType transactionType);

    /**
     * Find transactions that will expire soon
     */
    @Query("SELECT pl FROM PointsLedger pl " +
           "WHERE pl.expiresAt BETWEEN :now AND :expiryThreshold " +
           "AND pl.transactionType = 'EARNED' " +
           "ORDER BY pl.expiresAt ASC")
    List<PointsLedger> findPointsExpiringSoon(@Param("now") OffsetDateTime now, 
                                             @Param("expiryThreshold") OffsetDateTime expiryThreshold);

    /**
     * Get user's points that will expire within a timeframe
     */
    @Query("SELECT SUM(pl.pointsAmount) FROM PointsLedger pl " +
           "WHERE pl.userId = :userId " +
           "AND pl.expiresAt BETWEEN :now AND :expiryThreshold " +
           "AND pl.transactionType = 'EARNED'")
    Optional<Integer> getPointsExpiringForUser(@Param("userId") String userId, 
                                              @Param("now") OffsetDateTime now, 
                                              @Param("expiryThreshold") OffsetDateTime expiryThreshold);

    /**
     * Get monthly points summary
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM pl.createdAt) as year, " +
           "EXTRACT(MONTH FROM pl.createdAt) as month, " +
           "SUM(CASE WHEN pl.transactionType = 'EARNED' THEN pl.pointsAmount ELSE 0 END) as earned, " +
           "SUM(CASE WHEN pl.transactionType = 'REDEEMED' THEN ABS(pl.pointsAmount) ELSE 0 END) as redeemed " +
           "FROM PointsLedger pl " +
           "WHERE pl.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY EXTRACT(YEAR FROM pl.createdAt), EXTRACT(MONTH FROM pl.createdAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyPointsSummary(@Param("startDate") OffsetDateTime startDate, 
                                          @Param("endDate") OffsetDateTime endDate);

    /**
     * Find total points by user ID and transaction type
     */
    @Query("SELECT COALESCE(SUM(pl.pointsAmount), 0) " +
           "FROM PointsLedger pl " +
           "WHERE pl.userId = :userId AND pl.transactionType = :transactionType")
    Integer findTotalPointsByUserIdAndType(@Param("userId") String userId, 
                                          @Param("transactionType") PointsLedger.TransactionType transactionType);
}