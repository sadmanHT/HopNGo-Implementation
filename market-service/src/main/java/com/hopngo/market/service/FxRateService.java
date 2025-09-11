package com.hopngo.market.service;

import com.hopngo.market.entity.FxRate;
import com.hopngo.market.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing foreign exchange rates and currency conversions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FxRateService {
    
    private final FxRateRepository fxRateRepository;
    
    @Value("${hopngo.finance.base-currency:BDT}")
    private String baseCurrency;
    
    @Value("${hopngo.finance.fx-rate-max-age-days:7}")
    private int maxRateAgeDays;
    
    /**
     * Convert amount between currencies using latest available rates.
     */
    public long convertCurrency(long amountMinor, String fromCurrency, String toCurrency) {
        return convertCurrency(amountMinor, fromCurrency, toCurrency, LocalDate.now());
    }
    
    /**
     * Convert amount between currencies using rates for a specific date.
     */
    public long convertCurrency(long amountMinor, String fromCurrency, String toCurrency, LocalDate date) {
        // If same currency, return original amount
        if (fromCurrency.equals(toCurrency)) {
            return amountMinor;
        }
        
        log.debug("Converting {} minor units from {} to {} on date {}", 
                amountMinor, fromCurrency, toCurrency, date);
        
        try {
            // Get exchange rates
            BigDecimal fromRate = getExchangeRate(fromCurrency, date);
            BigDecimal toRate = getExchangeRate(toCurrency, date);
            
            // Convert to base currency (BDT) first
            BigDecimal amountDecimal = new BigDecimal(amountMinor).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            BigDecimal baseCurrencyAmount = amountDecimal.multiply(fromRate);
            
            // Convert from base currency to target currency
            BigDecimal targetAmount = baseCurrencyAmount.divide(toRate, 6, RoundingMode.HALF_UP);
            
            // Convert back to minor units
            long convertedMinor = targetAmount.multiply(new BigDecimal("100")).longValue();
            
            log.debug("Conversion result: {} {} = {} {}", 
                    amountDecimal, fromCurrency, targetAmount, toCurrency);
            
            return convertedMinor;
            
        } catch (Exception e) {
            log.error("Failed to convert {} from {} to {} on date {}", 
                    amountMinor, fromCurrency, toCurrency, date, e);
            throw new RuntimeException("Currency conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get exchange rate for a currency on a specific date.
     */
    public BigDecimal getExchangeRate(String currency, LocalDate date) {
        // Base currency always has rate 1.0
        if (baseCurrency.equals(currency)) {
            return BigDecimal.ONE;
        }
        
        Optional<FxRate> rateOpt = fxRateRepository.findLatestRateForCurrencyOnOrBefore(currency, date);
        
        if (rateOpt.isEmpty()) {
            throw new RuntimeException("No exchange rate found for currency: " + currency + " on or before " + date);
        }
        
        FxRate rate = rateOpt.get();
        
        // Check if rate is too old
        if (rate.isStale(maxRateAgeDays)) {
            log.warn("Using stale exchange rate for {}: {} (from {})", 
                    currency, rate.getRateToBdt(), rate.getDate());
        }
        
        return rate.getRateToBdt();
    }
    
    /**
     * Get current exchange rate for a currency.
     */
    public BigDecimal getCurrentExchangeRate(String currency) {
        return getExchangeRate(currency, LocalDate.now());
    }
    
    /**
     * Save or update exchange rate.
     */
    @Transactional
    public FxRate saveExchangeRate(String currency, BigDecimal rateToBdt, LocalDate date, String source) {
        log.info("Saving exchange rate: {} = {} BDT on {} (source: {})", 
                currency, rateToBdt, date, source);
        
        // Check if rate already exists for this currency and date
        Optional<FxRate> existingRate = fxRateRepository.findByCurrencyAndDate(currency, date);
        
        FxRate fxRate;
        if (existingRate.isPresent()) {
            // Update existing rate
            fxRate = existingRate.get();
            fxRate.setRateToBdt(rateToBdt);
            fxRate.setSource(source);
            log.debug("Updated existing rate for {} on {}", currency, date);
        } else {
            // Create new rate
            fxRate = FxRate.builder()
                    .currency(currency.toUpperCase())
                    .rateToBdt(rateToBdt)
                    .date(date)
                    .source(source)
                    .build();
            log.debug("Created new rate for {} on {}", currency, date);
        }
        
        return fxRateRepository.save(fxRate);
    }
    
    /**
     * Get latest rates for all currencies.
     */
    public List<FxRate> getLatestRatesForAllCurrencies() {
        return fxRateRepository.findLatestRatesForAllCurrencies();
    }
    
    /**
     * Get rate history for a currency.
     */
    public List<FxRate> getRateHistory(String currency, LocalDate startDate, LocalDate endDate) {
        return fxRateRepository.findByCurrencyAndDateBetween(currency, startDate, endDate);
    }
    
    /**
     * Check if currency is supported (has at least one rate).
     */
    public boolean isCurrencySupported(String currency) {
        if (baseCurrency.equals(currency)) {
            return true;
        }
        return fxRateRepository.findLatestRateForCurrency(currency).isPresent();
    }
    
    /**
     * Get currencies that need rate updates.
     */
    public List<String> getCurrenciesNeedingUpdate() {
        return fxRateRepository.findCurrenciesNeedingUpdate();
    }
    
    /**
     * Batch update rates from external source.
     */
    @Transactional
    public void batchUpdateRates(List<CurrencyRate> rates, String source) {
        log.info("Batch updating {} exchange rates from source: {}", rates.size(), source);
        
        LocalDate today = LocalDate.now();
        
        for (CurrencyRate rate : rates) {
            try {
                saveExchangeRate(rate.getCurrency(), rate.getRate(), today, source);
            } catch (Exception e) {
                log.error("Failed to save rate for {}: {}", rate.getCurrency(), rate.getRate(), e);
            }
        }
        
        log.info("Completed batch update of exchange rates");
    }
    
    /**
     * Clean up old exchange rates.
     */
    @Transactional
    public void cleanupOldRates(int keepDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);
        log.info("Cleaning up exchange rates older than {}", cutoffDate);
        
        List<FxRate> staleRates = fxRateRepository.findStaleRates(cutoffDate);
        log.info("Found {} stale rates to delete", staleRates.size());
        
        fxRateRepository.deleteRatesOlderThan(cutoffDate);
        log.info("Deleted exchange rates older than {}", cutoffDate);
    }
    
    /**
     * Get conversion rate between two currencies.
     */
    public BigDecimal getConversionRate(String fromCurrency, String toCurrency, LocalDate date) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        BigDecimal fromRate = getExchangeRate(fromCurrency, date);
        BigDecimal toRate = getExchangeRate(toCurrency, date);
        
        return fromRate.divide(toRate, 6, RoundingMode.HALF_UP);
    }
    
    /**
     * Data class for currency rate updates.
     */
    public static class CurrencyRate {
        private final String currency;
        private final BigDecimal rate;
        
        public CurrencyRate(String currency, BigDecimal rate) {
            this.currency = currency;
            this.rate = rate;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public BigDecimal getRate() {
            return rate;
        }
    }
}