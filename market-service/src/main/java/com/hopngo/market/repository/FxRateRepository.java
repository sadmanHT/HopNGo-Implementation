package com.hopngo.market.repository;

import com.hopngo.market.entity.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for FxRate entity operations.
 */
@Repository
public interface FxRateRepository extends JpaRepository<FxRate, UUID> {
    
    /**
     * Find the most recent exchange rate for a currency on or before a specific date.
     */
    @Query("SELECT f FROM FxRate f WHERE f.currency = :currency AND f.date <= :date " +
           "ORDER BY f.date DESC, f.createdAt DESC LIMIT 1")
    Optional<FxRate> findLatestRateForCurrencyOnOrBefore(
            @Param("currency") String currency, 
            @Param("date") LocalDate date);
    
    /**
     * Find the current (latest) exchange rate for a currency.
     */
    @Query("SELECT f FROM FxRate f WHERE f.currency = :currency " +
           "ORDER BY f.date DESC, f.createdAt DESC LIMIT 1")
    Optional<FxRate> findLatestRateForCurrency(@Param("currency") String currency);
    
    /**
     * Find exchange rate for a specific currency and date.
     */
    Optional<FxRate> findByCurrencyAndDate(String currency, LocalDate date);
    
    /**
     * Find all rates for a specific date.
     */
    List<FxRate> findByDateOrderByCurrency(LocalDate date);
    
    /**
     * Find all rates for a specific currency ordered by date descending.
     */
    List<FxRate> findByCurrencyOrderByDateDescCreatedAtDesc(String currency);
    
    /**
     * Find rates that are older than specified days.
     */
    @Query("SELECT f FROM FxRate f WHERE f.date < :cutoffDate")
    List<FxRate> findStaleRates(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * Get latest rates for all currencies.
     */
    @Query("SELECT f FROM FxRate f WHERE f.id IN (" +
           "SELECT MAX(f2.id) FROM FxRate f2 WHERE f2.currency = f.currency " +
           "AND f2.date = (SELECT MAX(f3.date) FROM FxRate f3 WHERE f3.currency = f2.currency))")
    List<FxRate> findLatestRatesForAllCurrencies();
    
    /**
     * Find rates by source.
     */
    List<FxRate> findBySourceOrderByDateDescCreatedAtDesc(String source);
    
    /**
     * Find rates within a date range for a currency.
     */
    @Query("SELECT f FROM FxRate f WHERE f.currency = :currency " +
           "AND f.date BETWEEN :startDate AND :endDate " +
           "ORDER BY f.date DESC")
    List<FxRate> findByCurrencyAndDateBetween(
            @Param("currency") String currency,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * Check if rate exists for currency and date.
     */
    boolean existsByCurrencyAndDate(String currency, LocalDate date);
    
    /**
     * Count rates by source.
     */
    long countBySource(String source);
    
    /**
     * Find currencies that need rate updates (no rate for today).
     */
    @Query("SELECT DISTINCT f.currency FROM FxRate f " +
           "WHERE f.currency NOT IN (" +
           "SELECT f2.currency FROM FxRate f2 WHERE f2.date = CURRENT_DATE)")
    List<String> findCurrenciesNeedingUpdate();
    
    /**
     * Get the oldest rate date for a currency.
     */
    @Query("SELECT MIN(f.date) FROM FxRate f WHERE f.currency = :currency")
    Optional<LocalDate> findOldestRateDateForCurrency(@Param("currency") String currency);
    
    /**
     * Get the newest rate date for a currency.
     */
    @Query("SELECT MAX(f.date) FROM FxRate f WHERE f.currency = :currency")
    Optional<LocalDate> findNewestRateDateForCurrency(@Param("currency") String currency);
    
    /**
     * Delete old rates (cleanup operation).
     */
    @Query("DELETE FROM FxRate f WHERE f.date < :cutoffDate")
    void deleteRatesOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}