package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.ProviderListingFunnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderListingFunnelRepository extends JpaRepository<ProviderListingFunnel, Long> {

    /**
     * Find funnel data for a specific provider and date
     */
    Optional<ProviderListingFunnel> findByProviderIdAndDate(String providerId, LocalDate date);

    /**
     * Find the latest funnel data for a specific provider
     */
    Optional<ProviderListingFunnel> findByProviderId(String providerId);

    /**
     * Find all funnel data for a provider within a date range
     */
    @Query("SELECT plf FROM ProviderListingFunnel plf WHERE plf.providerId = :providerId " +
           "AND plf.date >= :fromDate AND plf.date <= :toDate ORDER BY plf.date")
    List<ProviderListingFunnel> findByProviderIdAndDateBetween(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Find all providers with funnel data on a specific date
     */
    List<ProviderListingFunnel> findByDate(LocalDate date);

    /**
     * Get total impressions for a provider within a date range
     */
    @Query("SELECT COALESCE(SUM(plf.impressions), 0) FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId AND plf.date >= :fromDate AND plf.date <= :toDate")
    Long getTotalImpressionsByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get total detail views for a provider within a date range
     */
    @Query("SELECT COALESCE(SUM(plf.detailViews), 0) FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId AND plf.date >= :fromDate AND plf.date <= :toDate")
    Long getTotalDetailViewsByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get total add to cart events for a provider within a date range
     */
    @Query("SELECT COALESCE(SUM(plf.addToCart), 0) FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId AND plf.date >= :fromDate AND plf.date <= :toDate")
    Long getTotalAddToCartByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get funnel conversion rates for a provider within a date range
     */
    @Query("SELECT " +
           "SUM(plf.impressions) as totalImpressions, " +
           "SUM(plf.detailViews) as totalDetailViews, " +
           "SUM(plf.addToCart) as totalAddToCart, " +
           "SUM(plf.bookings) as totalBookings, " +
           "CASE WHEN SUM(plf.impressions) > 0 THEN (SUM(plf.detailViews) * 100.0 / SUM(plf.impressions)) ELSE 0 END as impressionToDetailRate, " +
           "CASE WHEN SUM(plf.detailViews) > 0 THEN (SUM(plf.addToCart) * 100.0 / SUM(plf.detailViews)) ELSE 0 END as detailToCartRate, " +
           "CASE WHEN SUM(plf.addToCart) > 0 THEN (SUM(plf.bookings) * 100.0 / SUM(plf.addToCart)) ELSE 0 END as cartToBookingRate, " +
           "CASE WHEN SUM(plf.impressions) > 0 THEN (SUM(plf.bookings) * 100.0 / SUM(plf.impressions)) ELSE 0 END as overallConversionRate " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId AND plf.date >= :fromDate AND plf.date <= :toDate")
    Object[] getFunnelConversionRatesByProvider(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get top providers by impression to detail view conversion rate
     */
    @Query("SELECT plf.providerId, " +
           "SUM(plf.impressions) as totalImpressions, " +
           "SUM(plf.detailViews) as totalDetailViews, " +
           "CASE WHEN SUM(plf.impressions) > 0 THEN (SUM(plf.detailViews) * 100.0 / SUM(plf.impressions)) ELSE 0 END as conversionRate " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.date >= :fromDate AND plf.date <= :toDate " +
           "GROUP BY plf.providerId " +
           "HAVING SUM(plf.impressions) >= :minImpressions " +
           "ORDER BY conversionRate DESC")
    List<Object[]> getTopProvidersByImpressionToDetailRate(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("minImpressions") Integer minImpressions,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get top providers by overall conversion rate (impressions to bookings)
     */
    @Query("SELECT plf.providerId, " +
           "SUM(plf.impressions) as totalImpressions, " +
           "SUM(plf.bookings) as totalBookings, " +
           "CASE WHEN SUM(plf.impressions) > 0 THEN (SUM(plf.bookings) * 100.0 / SUM(plf.impressions)) ELSE 0 END as conversionRate " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.date >= :fromDate AND plf.date <= :toDate " +
           "GROUP BY plf.providerId " +
           "HAVING SUM(plf.impressions) >= :minImpressions " +
           "ORDER BY conversionRate DESC")
    List<Object[]> getTopProvidersByOverallConversionRate(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("minImpressions") Integer minImpressions,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers with highest impression volumes
     */
    @Query("SELECT plf.providerId, SUM(plf.impressions) as totalImpressions " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.date >= :fromDate AND plf.date <= :toDate " +
           "GROUP BY plf.providerId " +
           "ORDER BY totalImpressions DESC")
    List<Object[]> getTopProvidersByImpressions(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get funnel drop-off analysis for a provider
     */
    @Query("SELECT " +
           "SUM(plf.impressions) as totalImpressions, " +
           "SUM(plf.detailViews) as totalDetailViews, " +
           "SUM(plf.addToCart) as totalAddToCart, " +
           "SUM(plf.bookings) as totalBookings, " +
           "SUM(plf.impressions - plf.detailViews) as impressionToDetailDropOff, " +
           "SUM(plf.detailViews - plf.addToCart) as detailToCartDropOff, " +
           "SUM(plf.addToCart - plf.bookings) as cartToBookingDropOff " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId AND plf.date >= :fromDate AND plf.date <= :toDate")
    Object[] getFunnelDropOffAnalysisByProvider(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get daily funnel performance trends for a provider
     */
    @Query("SELECT plf.date, " +
           "plf.impressions, plf.detailViews, plf.addToCart, plf.bookings, " +
           "CASE WHEN plf.impressions > 0 THEN (plf.detailViews * 100.0 / plf.impressions) ELSE 0 END as dailyConversionRate " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId AND plf.date >= :fromDate AND plf.date <= :toDate " +
           "ORDER BY plf.date")
    List<Object[]> getDailyFunnelTrendsByProvider(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get monthly funnel summary for a provider
     */
    @Query("SELECT EXTRACT(YEAR FROM plf.date) as year, " +
           "EXTRACT(MONTH FROM plf.date) as month, " +
           "SUM(plf.impressions) as totalImpressions, " +
           "SUM(plf.detailViews) as totalDetailViews, " +
           "SUM(plf.addToCart) as totalAddToCart, " +
           "SUM(plf.bookings) as totalBookings " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.providerId = :providerId " +
           "AND plf.date >= :fromDate AND plf.date <= :toDate " +
           "GROUP BY EXTRACT(YEAR FROM plf.date), EXTRACT(MONTH FROM plf.date) " +
           "ORDER BY year, month")
    List<Object[]> getMonthlyFunnelSummaryByProvider(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get global funnel benchmarks
     */
    @Query("SELECT " +
           "AVG(CASE WHEN plf.impressions > 0 THEN (plf.detailViews * 100.0 / plf.impressions) ELSE 0 END) as avgImpressionToDetailRate, " +
           "AVG(CASE WHEN plf.detailViews > 0 THEN (plf.addToCart * 100.0 / plf.detailViews) ELSE 0 END) as avgDetailToCartRate, " +
           "AVG(CASE WHEN plf.addToCart > 0 THEN (plf.bookings * 100.0 / plf.addToCart) ELSE 0 END) as avgCartToBookingRate, " +
           "AVG(CASE WHEN plf.impressions > 0 THEN (plf.bookings * 100.0 / plf.impressions) ELSE 0 END) as avgOverallConversionRate " +
           "FROM ProviderListingFunnel plf " +
           "WHERE plf.date >= :fromDate AND plf.date <= :toDate")
    Object[] getGlobalFunnelBenchmarks(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Check if data exists for a provider and date
     */
    boolean existsByProviderIdAndDate(String providerId, LocalDate date);

    /**
     * Delete old funnel data before a specific date
     */
    void deleteByDateBefore(LocalDate cutoffDate);

    /**
     * Find all unique provider IDs with funnel data
     */
    @Query("SELECT DISTINCT plf.providerId FROM ProviderListingFunnel plf")
    List<String> findAllUniqueProviderIds();
}