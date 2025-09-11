package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.ProviderBookingsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderBookingsDailyRepository extends JpaRepository<ProviderBookingsDaily, Long> {

    /**
     * Find bookings data for a specific provider and date
     */
    Optional<ProviderBookingsDaily> findByProviderIdAndDate(String providerId, LocalDate date);

    /**
     * Find all bookings data for a provider within a date range
     */
    @Query("SELECT pbd FROM ProviderBookingsDaily pbd WHERE pbd.providerId = :providerId " +
           "AND pbd.date >= :fromDate AND pbd.date <= :toDate ORDER BY pbd.date")
    List<ProviderBookingsDaily> findByProviderIdAndDateBetween(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Find all providers with bookings on a specific date
     */
    List<ProviderBookingsDaily> findByDate(LocalDate date);

    /**
     * Get total bookings for a provider within a date range
     */
    @Query("SELECT COALESCE(SUM(pbd.bookings), 0) FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.providerId = :providerId AND pbd.date >= :fromDate AND pbd.date <= :toDate")
    Long getTotalBookingsByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get total revenue for a provider within a date range
     */
    @Query("SELECT COALESCE(SUM(pbd.revenueMinor), 0) FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.providerId = :providerId AND pbd.date >= :fromDate AND pbd.date <= :toDate")
    Long getTotalRevenueByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get total cancellations for a provider within a date range
     */
    @Query("SELECT COALESCE(SUM(pbd.cancellations), 0) FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.providerId = :providerId AND pbd.date >= :fromDate AND pbd.date <= :toDate")
    Long getTotalCancellationsByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get average daily bookings for a provider within a date range
     */
    @Query("SELECT AVG(pbd.bookings) FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.providerId = :providerId AND pbd.date >= :fromDate AND pbd.date <= :toDate")
    Double getAverageDailyBookingsByProviderAndDateRange(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Get top performing providers by total bookings within a date range
     */
    @Query("SELECT pbd.providerId, SUM(pbd.bookings) as totalBookings " +
           "FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.date >= :fromDate AND pbd.date <= :toDate " +
           "GROUP BY pbd.providerId " +
           "ORDER BY totalBookings DESC")
    List<Object[]> getTopProvidersByBookings(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get top performing providers by total revenue within a date range
     */
    @Query("SELECT pbd.providerId, SUM(pbd.revenueMinor) as totalRevenue " +
           "FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.date >= :fromDate AND pbd.date <= :toDate " +
           "GROUP BY pbd.providerId " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> getTopProvidersByRevenue(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get providers with highest cancellation rates within a date range
     */
    @Query("SELECT pbd.providerId, " +
           "SUM(pbd.bookings) as totalBookings, " +
           "SUM(pbd.cancellations) as totalCancellations, " +
           "CASE WHEN SUM(pbd.bookings) > 0 THEN (SUM(pbd.cancellations) * 100.0 / SUM(pbd.bookings)) ELSE 0 END as cancellationRate " +
           "FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.date >= :fromDate AND pbd.date <= :toDate " +
           "GROUP BY pbd.providerId " +
           "HAVING SUM(pbd.bookings) > 0 " +
           "ORDER BY cancellationRate DESC")
    List<Object[]> getProvidersByCancellationRate(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get monthly summary for a provider
     */
    @Query("SELECT EXTRACT(YEAR FROM pbd.date) as year, " +
           "EXTRACT(MONTH FROM pbd.date) as month, " +
           "SUM(pbd.bookings) as totalBookings, " +
           "SUM(pbd.cancellations) as totalCancellations, " +
           "SUM(pbd.revenueMinor) as totalRevenue " +
           "FROM ProviderBookingsDaily pbd " +
           "WHERE pbd.providerId = :providerId " +
           "AND pbd.date >= :fromDate AND pbd.date <= :toDate " +
           "GROUP BY EXTRACT(YEAR FROM pbd.date), EXTRACT(MONTH FROM pbd.date) " +
           "ORDER BY year, month")
    List<Object[]> getMonthlySummaryByProvider(
            @Param("providerId") String providerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Check if data exists for a provider and date
     */
    boolean existsByProviderIdAndDate(String providerId, LocalDate date);

    /**
     * Delete old data before a specific date
     */
    void deleteByDateBefore(LocalDate cutoffDate);

    /**
     * Find all unique provider IDs
     */
    @Query("SELECT DISTINCT pbd.providerId FROM ProviderBookingsDaily pbd")
    List<String> findAllUniqueProviderIds();
}