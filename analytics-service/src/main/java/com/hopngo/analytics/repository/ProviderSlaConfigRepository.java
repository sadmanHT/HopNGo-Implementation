package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.ProviderSlaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderSlaConfigRepository extends JpaRepository<ProviderSlaConfig, Long> {

    /**
     * Find SLA configuration for a specific provider
     */
    Optional<ProviderSlaConfig> findByProviderId(String providerId);

    /**
     * Find providers with specific target response time
     */
    List<ProviderSlaConfig> findByTargetResponseTimeSec(Integer targetResponseTimeSec);

    /**
     * Find providers with target response time within a range
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc WHERE psc.targetResponseTimeSec >= :minSeconds AND psc.targetResponseTimeSec <= :maxSeconds")
    List<ProviderSlaConfig> findByTargetResponseTimeRange(
            @Param("minSeconds") Integer minSeconds,
            @Param("maxSeconds") Integer maxSeconds);

    /**
     * Find providers with target conversion rate above threshold
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc WHERE psc.targetBookingConversionRate >= :minRate")
    List<ProviderSlaConfig> findByTargetConversionRateAbove(@Param("minRate") BigDecimal minRate);

    /**
     * Find providers with target monthly revenue above threshold
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc WHERE psc.targetMonthlyRevenueMinor >= :minRevenue")
    List<ProviderSlaConfig> findByTargetMonthlyRevenueAbove(@Param("minRevenue") Long minRevenue);

    /**
     * Get providers with most aggressive SLA targets (shortest response times)
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc ORDER BY psc.targetResponseTimeSec ASC")
    List<ProviderSlaConfig> findProvidersByMostAggressiveSLA(org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers with most lenient SLA targets (longest response times)
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc ORDER BY psc.targetResponseTimeSec DESC")
    List<ProviderSlaConfig> findProvidersByMostLenientSLA(org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers with highest revenue targets
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc ORDER BY psc.targetMonthlyRevenueMinor DESC")
    List<ProviderSlaConfig> findProvidersByHighestRevenueTargets(org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers with highest conversion rate targets
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc ORDER BY psc.targetBookingConversionRate DESC")
    List<ProviderSlaConfig> findProvidersByHighestConversionTargets(org.springframework.data.domain.Pageable pageable);

    /**
     * Get SLA configuration statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalProviders, " +
           "AVG(psc.targetResponseTimeSec) as avgTargetResponseTime, " +
           "MIN(psc.targetResponseTimeSec) as minTargetResponseTime, " +
           "MAX(psc.targetResponseTimeSec) as maxTargetResponseTime, " +
           "AVG(psc.targetBookingConversionRate) as avgTargetConversionRate, " +
           "AVG(psc.targetMonthlyRevenueMinor) as avgTargetMonthlyRevenue " +
           "FROM ProviderSlaConfig psc")
    Object[] getSLAConfigurationStats();

    /**
     * Get SLA target distribution by response time ranges
     */
    @Query(value = "SELECT " +
                   "CASE " +
                   "WHEN target_response_time_sec <= 300 THEN '0-5 min' " +
                   "WHEN target_response_time_sec <= 900 THEN '5-15 min' " +
                   "WHEN target_response_time_sec <= 1800 THEN '15-30 min' " +
                   "WHEN target_response_time_sec <= 3600 THEN '30-60 min' " +
                   "WHEN target_response_time_sec <= 7200 THEN '1-2 hours' " +
                   "ELSE '2+ hours' " +
                   "END as time_range, " +
                   "COUNT(*) as provider_count " +
                   "FROM provider_sla_config " +
                   "GROUP BY time_range " +
                   "ORDER BY MIN(target_response_time_sec)", nativeQuery = true)
    List<Object[]> getSLATargetDistribution();

    /**
     * Get conversion rate target distribution
     */
    @Query(value = "SELECT " +
                   "CASE " +
                   "WHEN target_booking_conversion_rate <= 0.02 THEN '0-2%' " +
                   "WHEN target_booking_conversion_rate <= 0.05 THEN '2-5%' " +
                   "WHEN target_booking_conversion_rate <= 0.10 THEN '5-10%' " +
                   "WHEN target_booking_conversion_rate <= 0.15 THEN '10-15%' " +
                   "ELSE '15%+' " +
                   "END as conversion_range, " +
                   "COUNT(*) as provider_count " +
                   "FROM provider_sla_config " +
                   "GROUP BY conversion_range " +
                   "ORDER BY MIN(target_booking_conversion_rate)", nativeQuery = true)
    List<Object[]> getConversionTargetDistribution();

    /**
     * Find providers with default SLA settings
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc WHERE " +
           "psc.targetResponseTimeSec = 1800 AND " +
           "psc.targetBookingConversionRate = 0.05 AND " +
           "psc.targetMonthlyRevenueMinor = 0")
    List<ProviderSlaConfig> findProvidersWithDefaultSLA();

    /**
     * Find providers with custom SLA settings
     */
    @Query("SELECT psc FROM ProviderSlaConfig psc WHERE " +
           "psc.targetResponseTimeSec != 1800 OR " +
           "psc.targetBookingConversionRate != 0.05 OR " +
           "psc.targetMonthlyRevenueMinor != 0")
    List<ProviderSlaConfig> findProvidersWithCustomSLA();

    /**
     * Get providers needing SLA review (no configuration)
     */
    @Query(value = "SELECT DISTINCT pbd.provider_id " +
                   "FROM provider_bookings_daily pbd " +
                   "LEFT JOIN provider_sla_config psc ON pbd.provider_id = psc.provider_id " +
                   "WHERE psc.provider_id IS NULL", nativeQuery = true)
    List<String> findProvidersWithoutSLAConfig();

    /**
     * Bulk update target response time for multiple providers
     */
    @Query("UPDATE ProviderSlaConfig psc SET psc.targetResponseTimeSec = :targetSeconds " +
           "WHERE psc.providerId IN :providerIds")
    int bulkUpdateTargetResponseTime(
            @Param("providerIds") List<String> providerIds,
            @Param("targetSeconds") Integer targetSeconds);

    /**
     * Bulk update target conversion rate for multiple providers
     */
    @Query("UPDATE ProviderSlaConfig psc SET psc.targetBookingConversionRate = :targetRate " +
           "WHERE psc.providerId IN :providerIds")
    int bulkUpdateTargetConversionRate(
            @Param("providerIds") List<String> providerIds,
            @Param("targetRate") BigDecimal targetRate);

    /**
     * Check if SLA configuration exists for a provider
     */
    boolean existsByProviderId(String providerId);

    /**
     * Delete SLA configuration for a provider
     */
    void deleteByProviderId(String providerId);

    /**
     * Find all provider IDs with SLA configurations
     */
    @Query("SELECT psc.providerId FROM ProviderSlaConfig psc")
    List<String> findAllProviderIds();
}