package com.hopngo.market.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching daily exchange rates from external sources.
 * This is a stub implementation that can be extended to use real FX APIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FxRateFetcherService {
    
    private final FxRateService fxRateService;
    private final RestTemplate restTemplate;
    
    @Value("${hopngo.finance.fx-api.enabled:false}")
    private boolean fxApiEnabled;
    
    @Value("${hopngo.finance.fx-api.url:}")
    private String fxApiUrl;
    
    @Value("${hopngo.finance.fx-api.key:}")
    private String fxApiKey;
    
    @Value("${hopngo.finance.base-currency:BDT}")
    private String baseCurrency;
    
    // Currencies to fetch rates for
    private static final List<String> SUPPORTED_CURRENCIES = List.of(
            "USD", "EUR", "GBP", "INR", "CAD", "AUD", "JPY", "CNY", "SGD", "MYR"
    );
    
    /**
     * Scheduled task to fetch daily exchange rates.
     * Runs every day at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void fetchDailyRates() {
        if (!fxApiEnabled) {
            log.debug("FX API is disabled, skipping rate fetch");
            return;
        }
        
        log.info("Starting daily FX rate fetch for {} currencies", SUPPORTED_CURRENCIES.size());
        
        try {
            List<FxRateService.CurrencyRate> rates = fetchRatesFromApi();
            
            if (!rates.isEmpty()) {
                fxRateService.batchUpdateRates(rates, "API_SCHEDULED");
                log.info("Successfully updated {} exchange rates", rates.size());
            } else {
                log.warn("No rates fetched from API");
                // Fallback to stub rates
                updateWithStubRates();
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch daily exchange rates", e);
            // Fallback to stub rates
            updateWithStubRates();
        }
    }
    
    /**
     * Fetch rates from external API.
     * This is a stub implementation - replace with actual API integration.
     */
    private List<FxRateService.CurrencyRate> fetchRatesFromApi() {
        List<FxRateService.CurrencyRate> rates = new ArrayList<>();
        
        if (fxApiUrl == null || fxApiUrl.isEmpty()) {
            log.debug("No FX API URL configured, using stub rates");
            return getStubRates();
        }
        
        try {
            // Example API integration (replace with actual implementation)
            // String url = fxApiUrl + "?access_key=" + fxApiKey + "&base=" + baseCurrency;
            // Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // For now, return stub rates
            log.info("API integration not implemented, using stub rates");
            return getStubRates();
            
        } catch (Exception e) {
            log.error("Failed to fetch rates from API: {}", fxApiUrl, e);
            throw e;
        }
    }
    
    /**
     * Generate stub exchange rates for testing.
     * In production, this should be replaced with real API data.
     */
    private List<FxRateService.CurrencyRate> getStubRates() {
        List<FxRateService.CurrencyRate> rates = new ArrayList<>();
        
        // Approximate rates as of 2024 (BDT as base)
        rates.add(new FxRateService.CurrencyRate("USD", new BigDecimal("110.50")));
        rates.add(new FxRateService.CurrencyRate("EUR", new BigDecimal("120.25")));
        rates.add(new FxRateService.CurrencyRate("GBP", new BigDecimal("140.75")));
        rates.add(new FxRateService.CurrencyRate("INR", new BigDecimal("1.32")));
        rates.add(new FxRateService.CurrencyRate("CAD", new BigDecimal("81.20")));
        rates.add(new FxRateService.CurrencyRate("AUD", new BigDecimal("73.45")));
        rates.add(new FxRateService.CurrencyRate("JPY", new BigDecimal("0.74")));
        rates.add(new FxRateService.CurrencyRate("CNY", new BigDecimal("15.30")));
        rates.add(new FxRateService.CurrencyRate("SGD", new BigDecimal("82.10")));
        rates.add(new FxRateService.CurrencyRate("MYR", new BigDecimal("24.80")));
        
        // Add some random variation (±2%) to simulate real market fluctuations
        rates.forEach(rate -> {
            double variation = (Math.random() - 0.5) * 0.04; // ±2%
            BigDecimal newRate = rate.getRate().multiply(BigDecimal.ONE.add(BigDecimal.valueOf(variation)));
            rates.set(rates.indexOf(rate), new FxRateService.CurrencyRate(rate.getCurrency(), newRate));
        });
        
        log.debug("Generated {} stub exchange rates", rates.size());
        return rates;
    }
    
    /**
     * Update rates with stub data as fallback.
     */
    private void updateWithStubRates() {
        try {
            List<FxRateService.CurrencyRate> stubRates = getStubRates();
            fxRateService.batchUpdateRates(stubRates, "STUB_FALLBACK");
            log.info("Updated {} exchange rates using stub data", stubRates.size());
        } catch (Exception e) {
            log.error("Failed to update with stub rates", e);
        }
    }
    
    /**
     * Manual trigger for rate updates (for admin use).
     */
    public void manualRateUpdate() {
        log.info("Manual FX rate update triggered");
        fetchDailyRates();
    }
    
    /**
     * Fetch rates for specific currencies.
     */
    public void fetchRatesForCurrencies(List<String> currencies) {
        log.info("Fetching rates for specific currencies: {}", currencies);
        
        try {
            // In a real implementation, this would call the API for specific currencies
            List<FxRateService.CurrencyRate> allRates = getStubRates();
            List<FxRateService.CurrencyRate> filteredRates = allRates.stream()
                    .filter(rate -> currencies.contains(rate.getCurrency()))
                    .toList();
            
            fxRateService.batchUpdateRates(filteredRates, "API_MANUAL");
            log.info("Updated {} specific exchange rates", filteredRates.size());
            
        } catch (Exception e) {
            log.error("Failed to fetch rates for specific currencies", e);
            throw new RuntimeException("Failed to fetch exchange rates", e);
        }
    }
    
    /**
     * Get supported currencies.
     */
    public List<String> getSupportedCurrencies() {
        return new ArrayList<>(SUPPORTED_CURRENCIES);
    }
    
    /**
     * Check if API is configured and enabled.
     */
    public boolean isApiConfigured() {
        return fxApiEnabled && fxApiUrl != null && !fxApiUrl.isEmpty();
    }
    
    /**
     * Cleanup old rates (runs weekly).
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void weeklyCleanup() {
        log.info("Starting weekly FX rate cleanup");
        
        try {
            // Keep rates for 90 days
            fxRateService.cleanupOldRates(90);
            log.info("Completed weekly FX rate cleanup");
        } catch (Exception e) {
            log.error("Failed to cleanup old FX rates", e);
        }
    }
}