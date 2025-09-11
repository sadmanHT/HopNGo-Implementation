package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.ProviderResponseTimes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderResponseTimesRepository extends JpaRepository<ProviderResponseTimes, Long> {

    /**
     * Find response times for a specific provider
     */
    Optional<ProviderResponseTimes> findByProviderId(String providerId);

    /**
     * Find providers with response times better than target
     */
    @Query("SELECT prt FROM ProviderResponseTimes prt WHERE prt.avgFirstReplySec <= :targetSeconds")
    List<ProviderResponseTimes> findProvidersWithinSLA(@Param("targetSeconds") Integer targetSeconds);

    /**
     * Find providers with response times worse than target
     */
    @Query("SELECT prt FROM ProviderResponseTimes prt WHERE prt.avgFirstReplySec > :targetSeconds")
    List<ProviderResponseTimes> findProvidersExceedingSLA(@Param("targetSeconds") Integer targetSeconds);

    /**
     * Get providers ordered by best response times
     */
    @Query("SELECT prt FROM ProviderResponseTimes prt ORDER BY prt.avgFirstReplySec ASC")
    List<ProviderResponseTimes> findProvidersByBestResponseTimes(org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers ordered by worst response times
     */
    @Query("SELECT prt FROM ProviderResponseTimes prt ORDER BY prt.avgFirstReplySec DESC")
    List<ProviderResponseTimes> findProvidersByWorstResponseTimes(org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers with most conversations
     */
    @Query("SELECT prt FROM ProviderResponseTimes prt ORDER BY prt.totalConversations DESC")
    List<ProviderResponseTimes> findProvidersByMostConversations(org.springframework.data.domain.Pageable pageable);

    /**
     * Update response time for a provider by adding new conversation data
     */
    @Modifying
    @Query("UPDATE ProviderResponseTimes prt SET " +
           "prt.totalConversations = prt.totalConversations + 1, " +
           "prt.totalResponseTimeSec = prt.totalResponseTimeSec + :responseTimeSec, " +
           "prt.avgFirstReplySec = (prt.totalResponseTimeSec + :responseTimeSec) / (prt.totalConversations + 1), " +
           "prt.lastCalculatedAt = :calculatedAt " +
           "WHERE prt.providerId = :providerId")
    int addConversationResponseTime(
            @Param("providerId") String providerId,
            @Param("responseTimeSec") Integer responseTimeSec,
            @Param("calculatedAt") OffsetDateTime calculatedAt);

    /**
     * Recalculate average response time for a provider
     */
    @Modifying
    @Query("UPDATE ProviderResponseTimes prt SET " +
           "prt.avgFirstReplySec = CASE WHEN prt.totalConversations > 0 THEN prt.totalResponseTimeSec / prt.totalConversations ELSE 0 END, " +
           "prt.lastCalculatedAt = :calculatedAt " +
           "WHERE prt.providerId = :providerId")
    int recalculateAverageResponseTime(
            @Param("providerId") String providerId,
            @Param("calculatedAt") OffsetDateTime calculatedAt);

    /**
     * Get average response time across all providers
     */
    @Query("SELECT AVG(prt.avgFirstReplySec) FROM ProviderResponseTimes prt WHERE prt.totalConversations > 0")
    Double getGlobalAverageResponseTime();

    /**
     * Get median response time across all providers
     */
    @Query(value = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY avg_first_reply_sec) " +
                   "FROM provider_response_times WHERE total_conversations > 0", nativeQuery = true)
    Double getGlobalMedianResponseTime();

    /**
     * Get response time percentiles
     */
    @Query(value = "SELECT " +
                   "PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY avg_first_reply_sec) as p25, " +
                   "PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY avg_first_reply_sec) as p50, " +
                   "PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY avg_first_reply_sec) as p75, " +
                   "PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY avg_first_reply_sec) as p90, " +
                   "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY avg_first_reply_sec) as p95 " +
                   "FROM provider_response_times WHERE total_conversations > 0", nativeQuery = true)
    Object[] getResponseTimePercentiles();

    /**
     * Find providers that need response time recalculation
     */
    @Query("SELECT prt FROM ProviderResponseTimes prt WHERE prt.lastCalculatedAt < :cutoffTime OR prt.lastCalculatedAt IS NULL")
    List<ProviderResponseTimes> findProvidersNeedingRecalculation(@Param("cutoffTime") OffsetDateTime cutoffTime);

    /**
     * Get SLA compliance statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalProviders, " +
           "SUM(CASE WHEN prt.avgFirstReplySec <= :targetSeconds THEN 1 ELSE 0 END) as compliantProviders, " +
           "SUM(CASE WHEN prt.avgFirstReplySec > :targetSeconds THEN 1 ELSE 0 END) as nonCompliantProviders " +
           "FROM ProviderResponseTimes prt WHERE prt.totalConversations > 0")
    Object[] getSLAComplianceStats(@Param("targetSeconds") Integer targetSeconds);

    /**
     * Get response time distribution by ranges
     */
    @Query(value = "SELECT " +
                   "CASE " +
                   "WHEN avg_first_reply_sec <= 300 THEN '0-5 min' " +
                   "WHEN avg_first_reply_sec <= 900 THEN '5-15 min' " +
                   "WHEN avg_first_reply_sec <= 1800 THEN '15-30 min' " +
                   "WHEN avg_first_reply_sec <= 3600 THEN '30-60 min' " +
                   "WHEN avg_first_reply_sec <= 7200 THEN '1-2 hours' " +
                   "ELSE '2+ hours' " +
                   "END as time_range, " +
                   "COUNT(*) as provider_count " +
                   "FROM provider_response_times " +
                   "WHERE total_conversations > 0 " +
                   "GROUP BY time_range " +
                   "ORDER BY MIN(avg_first_reply_sec)", nativeQuery = true)
    List<Object[]> getResponseTimeDistribution();

    /**
     * Check if provider exists
     */
    boolean existsByProviderId(String providerId);

    /**
     * Delete response time data for a provider
     */
    void deleteByProviderId(String providerId);

    /**
     * Find all provider IDs with response time data
     */
    @Query("SELECT prt.providerId FROM ProviderResponseTimes prt")
    List<String> findAllProviderIds();
}