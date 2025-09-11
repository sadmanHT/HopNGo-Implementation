package com.hopngo.market.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Service for calculating taxes and fees based on configurable rates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxFeeCalculationService {
    
    @Value("${hopngo.finance.platform-fee-percentage:5.0}")
    private BigDecimal platformFeePercentage;
    
    @Value("${hopngo.finance.default-vat-percentage:15.0}")
    private BigDecimal defaultVatPercentage;
    
    // Country-specific VAT rates (can be moved to database/config-service later)
    private static final Map<String, BigDecimal> COUNTRY_VAT_RATES = Map.of(
            "BD", new BigDecimal("15.0"), // Bangladesh VAT
            "US", new BigDecimal("8.25"), // Average US sales tax
            "GB", new BigDecimal("20.0"), // UK VAT
            "IN", new BigDecimal("18.0"), // India GST
            "CA", new BigDecimal("13.0")  // Canada HST average
    );
    
    /**
     * Calculate platform fee based on subtotal.
     */
    public long calculatePlatformFeeMinor(long subtotalMinor) {
        BigDecimal subtotal = new BigDecimal(subtotalMinor);
        BigDecimal fee = subtotal.multiply(platformFeePercentage)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        
        long feeMinor = fee.longValue();
        log.debug("Calculated platform fee: {} minor units ({}%) for subtotal: {} minor units", 
                feeMinor, platformFeePercentage, subtotalMinor);
        
        return feeMinor;
    }
    
    /**
     * Calculate VAT/GST based on country and subtotal.
     */
    public long calculateVatMinor(long subtotalMinor, String countryCode) {
        BigDecimal vatRate = getVatRateForCountry(countryCode);
        BigDecimal subtotal = new BigDecimal(subtotalMinor);
        BigDecimal vat = subtotal.multiply(vatRate)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        
        long vatMinor = vat.longValue();
        log.debug("Calculated VAT: {} minor units ({}%) for country: {} and subtotal: {} minor units", 
                vatMinor, vatRate, countryCode, subtotalMinor);
        
        return vatMinor;
    }
    
    /**
     * Calculate service fee (additional processing fee).
     */
    public long calculateServiceFeeMinor(long subtotalMinor) {
        // Fixed service fee of 2% or minimum 50 minor units (0.50 BDT)
        BigDecimal subtotal = new BigDecimal(subtotalMinor);
        BigDecimal serviceFeeRate = new BigDecimal("2.0");
        BigDecimal calculatedFee = subtotal.multiply(serviceFeeRate)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        
        long minimumFee = 50L; // 0.50 BDT minimum
        long serviceFeeMinor = Math.max(calculatedFee.longValue(), minimumFee);
        
        log.debug("Calculated service fee: {} minor units for subtotal: {} minor units", 
                serviceFeeMinor, subtotalMinor);
        
        return serviceFeeMinor;
    }
    
    /**
     * Calculate total amount including all taxes and fees.
     */
    public TaxFeeBreakdown calculateTotalWithTaxesAndFees(long subtotalMinor, String countryCode) {
        long platformFee = calculatePlatformFeeMinor(subtotalMinor);
        long vat = calculateVatMinor(subtotalMinor, countryCode);
        long serviceFee = calculateServiceFeeMinor(subtotalMinor);
        
        long totalTax = vat;
        long totalFees = platformFee + serviceFee;
        long grandTotal = subtotalMinor + totalTax + totalFees;
        
        TaxFeeBreakdown breakdown = TaxFeeBreakdown.builder()
                .subtotalMinor(subtotalMinor)
                .vatMinor(vat)
                .platformFeeMinor(platformFee)
                .serviceFeeMinor(serviceFee)
                .totalTaxMinor(totalTax)
                .totalFeesMinor(totalFees)
                .grandTotalMinor(grandTotal)
                .countryCode(countryCode)
                .vatRate(getVatRateForCountry(countryCode))
                .platformFeeRate(platformFeePercentage)
                .build();
        
        log.info("Tax/Fee breakdown for country {}: subtotal={}, vat={}, platform_fee={}, service_fee={}, total={}", 
                countryCode, subtotalMinor, vat, platformFee, serviceFee, grandTotal);
        
        return breakdown;
    }
    
    /**
     * Calculate provider payout (subtotal minus platform fee).
     */
    public long calculateProviderPayoutMinor(long subtotalMinor) {
        long platformFee = calculatePlatformFeeMinor(subtotalMinor);
        long providerPayout = subtotalMinor - platformFee;
        
        log.debug("Calculated provider payout: {} minor units (subtotal: {} - platform_fee: {})", 
                providerPayout, subtotalMinor, platformFee);
        
        return Math.max(providerPayout, 0L); // Ensure non-negative
    }
    
    /**
     * Get VAT rate for a specific country.
     */
    private BigDecimal getVatRateForCountry(String countryCode) {
        return COUNTRY_VAT_RATES.getOrDefault(
                countryCode != null ? countryCode.toUpperCase() : "BD", 
                defaultVatPercentage
        );
    }
    
    /**
     * Data class for tax and fee breakdown.
     */
    public static class TaxFeeBreakdown {
        private final long subtotalMinor;
        private final long vatMinor;
        private final long platformFeeMinor;
        private final long serviceFeeMinor;
        private final long totalTaxMinor;
        private final long totalFeesMinor;
        private final long grandTotalMinor;
        private final String countryCode;
        private final BigDecimal vatRate;
        private final BigDecimal platformFeeRate;
        
        private TaxFeeBreakdown(Builder builder) {
            this.subtotalMinor = builder.subtotalMinor;
            this.vatMinor = builder.vatMinor;
            this.platformFeeMinor = builder.platformFeeMinor;
            this.serviceFeeMinor = builder.serviceFeeMinor;
            this.totalTaxMinor = builder.totalTaxMinor;
            this.totalFeesMinor = builder.totalFeesMinor;
            this.grandTotalMinor = builder.grandTotalMinor;
            this.countryCode = builder.countryCode;
            this.vatRate = builder.vatRate;
            this.platformFeeRate = builder.platformFeeRate;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public long getSubtotalMinor() { return subtotalMinor; }
        public long getVatMinor() { return vatMinor; }
        public long getPlatformFeeMinor() { return platformFeeMinor; }
        public long getServiceFeeMinor() { return serviceFeeMinor; }
        public long getTotalTaxMinor() { return totalTaxMinor; }
        public long getTotalFeesMinor() { return totalFeesMinor; }
        public long getGrandTotalMinor() { return grandTotalMinor; }
        public String getCountryCode() { return countryCode; }
        public BigDecimal getVatRate() { return vatRate; }
        public BigDecimal getPlatformFeeRate() { return platformFeeRate; }
        
        public static class Builder {
            private long subtotalMinor;
            private long vatMinor;
            private long platformFeeMinor;
            private long serviceFeeMinor;
            private long totalTaxMinor;
            private long totalFeesMinor;
            private long grandTotalMinor;
            private String countryCode;
            private BigDecimal vatRate;
            private BigDecimal platformFeeRate;
            
            public Builder subtotalMinor(long subtotalMinor) {
                this.subtotalMinor = subtotalMinor;
                return this;
            }
            
            public Builder vatMinor(long vatMinor) {
                this.vatMinor = vatMinor;
                return this;
            }
            
            public Builder platformFeeMinor(long platformFeeMinor) {
                this.platformFeeMinor = platformFeeMinor;
                return this;
            }
            
            public Builder serviceFeeMinor(long serviceFeeMinor) {
                this.serviceFeeMinor = serviceFeeMinor;
                return this;
            }
            
            public Builder totalTaxMinor(long totalTaxMinor) {
                this.totalTaxMinor = totalTaxMinor;
                return this;
            }
            
            public Builder totalFeesMinor(long totalFeesMinor) {
                this.totalFeesMinor = totalFeesMinor;
                return this;
            }
            
            public Builder grandTotalMinor(long grandTotalMinor) {
                this.grandTotalMinor = grandTotalMinor;
                return this;
            }
            
            public Builder countryCode(String countryCode) {
                this.countryCode = countryCode;
                return this;
            }
            
            public Builder vatRate(BigDecimal vatRate) {
                this.vatRate = vatRate;
                return this;
            }
            
            public Builder platformFeeRate(BigDecimal platformFeeRate) {
                this.platformFeeRate = platformFeeRate;
                return this;
            }
            
            public TaxFeeBreakdown build() {
                return new TaxFeeBreakdown(this);
            }
        }
    }
}