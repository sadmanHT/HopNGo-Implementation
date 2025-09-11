package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.Subscriber;
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
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    /**
     * Find subscriber by email
     */
    Optional<Subscriber> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find subscriber by user ID
     */
    Optional<Subscriber> findByUserId(String userId);

    /**
     * Find subscribers by status
     */
    List<Subscriber> findByStatusOrderByCreatedAtDesc(Subscriber.SubscriberStatus status);

    /**
     * Find subscribers by status with pagination
     */
    Page<Subscriber> findByStatusOrderByCreatedAtDesc(Subscriber.SubscriberStatus status, Pageable pageable);

    /**
     * Find subscribers by source
     */
    List<Subscriber> findBySourceOrderByCreatedAtDesc(String source);

    /**
     * Find subscribers by source with pagination
     */
    Page<Subscriber> findBySourceOrderByCreatedAtDesc(String source, Pageable pageable);

    /**
     * Find subscribers created within date range
     */
    List<Subscriber> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find subscribers subscribed within date range
     */
    List<Subscriber> findBySubscribedAtBetweenOrderBySubscribedAtDesc(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find subscribers by UTM campaign
     */
    List<Subscriber> findByUtmCampaignOrderByCreatedAtDesc(String utmCampaign);

    /**
     * Find subscribers by UTM source
     */
    List<Subscriber> findByUtmSourceOrderByCreatedAtDesc(String utmSource);

    /**
     * Find subscribers with specific tag
     */
    @Query("SELECT s FROM Subscriber s WHERE :tag = ANY(s.tags) ORDER BY s.createdAt DESC")
    List<Subscriber> findByTag(@Param("tag") String tag);

    /**
     * Find subscribers with specific tag and pagination
     */
    @Query("SELECT s FROM Subscriber s WHERE :tag = ANY(s.tags) ORDER BY s.createdAt DESC")
    Page<Subscriber> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Count subscribers by status
     */
    long countByStatus(Subscriber.SubscriberStatus status);

    /**
     * Count subscribers by source
     */
    long countBySource(String source);

    /**
     * Get subscriber statistics
     */
    @Query("SELECT " +
           "COUNT(s) as totalSubscribers, " +
           "SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeSubscribers, " +
           "SUM(CASE WHEN s.status = 'UNSUBSCRIBED' THEN 1 ELSE 0 END) as unsubscribedSubscribers, " +
           "SUM(CASE WHEN s.status = 'BOUNCED' THEN 1 ELSE 0 END) as bouncedSubscribers " +
           "FROM Subscriber s")
    Object[] getSubscriberStatistics();

    /**
     * Get daily subscription metrics for date range
     */
    @Query("SELECT " +
           "DATE(s.subscribedAt) as date, " +
           "COUNT(s) as newSubscriptions, " +
           "SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeSubscriptions " +
           "FROM Subscriber s " +
           "WHERE s.subscribedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(s.subscribedAt) " +
           "ORDER BY date DESC")
    List<Object[]> getDailySubscriptionMetrics(@Param("startDate") OffsetDateTime startDate, 
                                              @Param("endDate") OffsetDateTime endDate);

    /**
     * Get subscription sources performance
     */
    @Query("SELECT s.source, " +
           "COUNT(s) as totalSubscriptions, " +
           "SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeSubscriptions, " +
           "ROUND(CAST(SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(s) * 100, 2) as activeRate " +
           "FROM Subscriber s " +
           "WHERE s.source IS NOT NULL " +
           "GROUP BY s.source " +
           "ORDER BY totalSubscriptions DESC")
    List<Object[]> getSubscriptionSourcesPerformance();

    /**
     * Get UTM campaign performance
     */
    @Query("SELECT s.utmCampaign, " +
           "COUNT(s) as totalSubscriptions, " +
           "SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeSubscriptions " +
           "FROM Subscriber s " +
           "WHERE s.utmCampaign IS NOT NULL " +
           "GROUP BY s.utmCampaign " +
           "ORDER BY totalSubscriptions DESC")
    List<Object[]> getUtmCampaignPerformance();

    /**
     * Get tag usage statistics
     */
    @Query(value = "SELECT tag, COUNT(*) as usage_count " +
                  "FROM subscriber s, UNNEST(s.tags) as tag " +
                  "WHERE s.status = 'ACTIVE' " +
                  "GROUP BY tag " +
                  "ORDER BY usage_count DESC", nativeQuery = true)
    List<Object[]> getTagUsageStatistics();

    /**
     * Find subscribers who unsubscribed within date range
     */
    List<Subscriber> findByUnsubscribedAtBetweenOrderByUnsubscribedAtDesc(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Get monthly subscription trends
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM s.subscribedAt) as year, " +
           "EXTRACT(MONTH FROM s.subscribedAt) as month, " +
           "COUNT(s) as subscriptions, " +
           "SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeSubscriptions " +
           "FROM Subscriber s " +
           "WHERE s.subscribedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY EXTRACT(YEAR FROM s.subscribedAt), EXTRACT(MONTH FROM s.subscribedAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlySubscriptionTrends(@Param("startDate") OffsetDateTime startDate, 
                                               @Param("endDate") OffsetDateTime endDate);

    /**
     * Find subscribers with multiple tags
     */
    @Query("SELECT s FROM Subscriber s WHERE s.tags && CAST(:tags AS text[]) ORDER BY s.createdAt DESC")
    List<Subscriber> findByMultipleTags(@Param("tags") String[] tags);

    /**
     * Get subscriber growth rate
     */
    @Query("SELECT " +
           "DATE(s.subscribedAt) as date, " +
           "COUNT(s) as newSubscribers, " +
           "(SELECT COUNT(us) FROM Subscriber us WHERE us.unsubscribedAt = DATE(s.subscribedAt)) as unsubscribers, " +
           "COUNT(s) - (SELECT COUNT(us) FROM Subscriber us WHERE us.unsubscribedAt = DATE(s.subscribedAt)) as netGrowth " +
           "FROM Subscriber s " +
           "WHERE s.subscribedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(s.subscribedAt) " +
           "ORDER BY date DESC")
    List<Object[]> getSubscriberGrowthRate(@Param("startDate") OffsetDateTime startDate, 
                                          @Param("endDate") OffsetDateTime endDate);

    /**
     * Find recent subscribers for welcome campaigns
     */
    @Query("SELECT s FROM Subscriber s " +
           "WHERE s.status = 'ACTIVE' " +
           "AND s.subscribedAt >= :since " +
           "ORDER BY s.subscribedAt DESC")
    List<Subscriber> findRecentActiveSubscribers(@Param("since") OffsetDateTime since);

    /**
     * Find subscribers by email domain
     */
    @Query("SELECT s FROM Subscriber s WHERE s.email LIKE CONCAT('%@', :domain) ORDER BY s.createdAt DESC")
    List<Subscriber> findByEmailDomain(@Param("domain") String domain);

    /**
     * Get email domain statistics
     */
    @Query(value = "SELECT SUBSTRING(email FROM '@(.*)$') as domain, COUNT(*) as count " +
                  "FROM subscribers " +
                  "WHERE status = 'ACTIVE' " +
                  "GROUP BY domain " +
                  "ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getEmailDomainStatistics();

    /**
     * Find subscriber by unsubscribe token
     */
    Optional<Subscriber> findByUnsubscribeToken(String unsubscribeToken);

    /**
     * Find subscribers by status
     */
    List<Subscriber> findByStatus(Subscriber.SubscriberStatus status);

    /**
     * Find subscribers by source and status
     */
    List<Subscriber> findBySourceAndStatus(String source, Subscriber.SubscriberStatus status);

    /**
     * Count subscribers by source and status
     */
    long countBySourceAndStatus(String source, Subscriber.SubscriberStatus status);

    /**
     * Find recent subscribers
     */
    @Query("SELECT s FROM Subscriber s WHERE s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Subscriber> findRecentSubscribers(int limit);

    /**
     * Find subscribers by tags containing specific tag and status
     */
    @Query("SELECT s FROM Subscriber s WHERE :tag = ANY(s.tags) AND s.status = :status ORDER BY s.createdAt DESC")
    List<Subscriber> findByTagsContainingAndStatus(@Param("tag") String tag, @Param("status") Subscriber.SubscriberStatus status);
}