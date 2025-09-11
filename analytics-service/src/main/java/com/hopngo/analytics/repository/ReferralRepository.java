package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.Referral;
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
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    /**
     * Find referral by referral code
     */
    Optional<Referral> findByReferralCode(String referralCode);

    /**
     * Check if referral code exists
     */
    boolean existsByReferralCode(String referralCode);

    /**
     * Find all referrals by referrer user ID
     */
    List<Referral> findByReferrerUserIdOrderByCreatedAtDesc(String referrerUserId);

    /**
     * Find referrals by referrer user ID with pagination
     */
    Page<Referral> findByReferrerUserIdOrderByCreatedAtDesc(String referrerUserId, Pageable pageable);

    /**
     * Find referral by referred user ID
     */
    Optional<Referral> findByReferredUserId(String referredUserId);

    /**
     * Find referrals by status
     */
    List<Referral> findByStatus(Referral.ReferralStatus status);

    /**
     * Find referrals by referrer and status
     */
    List<Referral> findByReferrerUserIdAndStatus(String referrerUserId, Referral.ReferralStatus status);

    /**
     * Find referrals created within date range
     */
    List<Referral> findByCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find referrals by referrer within date range
     */
    List<Referral> findByReferrerUserIdAndCreatedAtBetween(String referrerUserId, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find expired referrals that haven't been marked as expired
     */
    @Query("SELECT r FROM Referral r WHERE r.expiresAt < :now AND r.status = :status")
    List<Referral> findExpiredReferrals(@Param("now") OffsetDateTime now, @Param("status") Referral.ReferralStatus status);

    /**
     * Count referrals by referrer user ID
     */
    long countByReferrerUserId(String referrerUserId);

    /**
     * Count completed referrals by referrer user ID
     */
    long countByReferrerUserIdAndStatus(String referrerUserId, Referral.ReferralStatus status);

    /**
     * Get referral statistics for a user
     */
    @Query("SELECT " +
           "COUNT(r) as totalReferrals, " +
           "SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedReferrals, " +
           "SUM(CASE WHEN r.status = 'PENDING' THEN 1 ELSE 0 END) as pendingReferrals, " +
           "SUM(r.pointsAwarded) as totalPointsAwarded, " +
           "SUM(r.conversionValueMinor) as totalConversionValue " +
           "FROM Referral r WHERE r.referrerUserId = :referrerUserId")
    Object[] getReferralStatsByReferrer(@Param("referrerUserId") String referrerUserId);

    /**
     * Get top referrers by completed referrals
     */
    @Query("SELECT r.referrerUserId, COUNT(r) as completedCount " +
           "FROM Referral r " +
           "WHERE r.status = 'COMPLETED' " +
           "GROUP BY r.referrerUserId " +
           "ORDER BY completedCount DESC")
    List<Object[]> getTopReferrersByCompletedReferrals(Pageable pageable);

    /**
     * Get referral conversion rates by source
     */
    @Query("SELECT r.source, " +
           "COUNT(r) as totalReferrals, " +
           "SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedReferrals, " +
           "ROUND(CAST(SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(r) * 100, 2) as conversionRate " +
           "FROM Referral r " +
           "WHERE r.source IS NOT NULL " +
           "GROUP BY r.source " +
           "ORDER BY conversionRate DESC")
    List<Object[]> getReferralConversionRatesBySource();

    /**
     * Get daily referral metrics for date range
     */
    @Query("SELECT " +
           "DATE(r.createdAt) as date, " +
           "COUNT(r) as totalReferrals, " +
           "SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedReferrals, " +
           "SUM(r.pointsAwarded) as totalPointsAwarded " +
           "FROM Referral r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(r.createdAt) " +
           "ORDER BY date DESC")
    List<Object[]> getDailyReferralMetrics(@Param("startDate") OffsetDateTime startDate, 
                                          @Param("endDate") OffsetDateTime endDate);

    /**
     * Find referrals by campaign
     */
    List<Referral> findByCampaignOrderByCreatedAtDesc(String campaign);

    /**
     * Find referrals by source
     */
    List<Referral> findBySourceOrderByCreatedAtDesc(String source);

    /**
     * Get referrals that need points processing (completed but points not awarded)
     */
    @Query("SELECT r FROM Referral r WHERE r.status = 'COMPLETED' AND r.pointsAwarded = 0 AND r.pointsPending > 0")
    List<Referral> findReferralsNeedingPointsProcessing();

    /**
     * Find referral by referral code and status
     */
    Optional<Referral> findByReferralCodeAndStatus(String referralCode, Referral.ReferralStatus status);

    /**
     * Find referrals by user ID (alias for referrerUserId)
     */
    List<Referral> findByUserId(String userId);

    /**
     * Find referrals by user ID and status (alias for referrerUserId)
     */
    List<Referral> findByUserIdAndStatus(String userId, Referral.ReferralStatus status);
}