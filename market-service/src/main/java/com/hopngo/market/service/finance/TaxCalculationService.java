package com.hopngo.market.service.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalculationService {

    /**
     * Calculate tax amount based on subtotal and location
     */
    public BigDecimal calculateTax(BigDecimal subtotal, String country, String state) {
        log.debug("Calculating tax for subtotal: {}, country: {}, state: {}", subtotal, country, state);
        
        BigDecimal taxRate = getTaxRate(country, state);
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Tax calculated: {} (rate: {})", tax, taxRate);
        return tax;
    }

    /**
     * Get tax rate for given location
     */
    public BigDecimal getTaxRate(String country, String state) {
        // Default tax rates - in production this would come from a tax service
        switch (country.toUpperCase()) {
            case "US":
                return getUSTaxRate(state);
            case "CA":
                return getCanadaTaxRate(state);
            case "BD":
                return new BigDecimal("0.15"); // 15% VAT
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal getUSTaxRate(String state) {
        // Simplified state tax rates
        switch (state.toUpperCase()) {
            case "CA":
                return new BigDecimal("0.0875"); // California
            case "NY":
                return new BigDecimal("0.08"); // New York
            case "TX":
                return new BigDecimal("0.0625"); // Texas
            case "FL":
                return BigDecimal.ZERO; // No state tax
            default:
                return new BigDecimal("0.07"); // Average rate
        }
    }

    private BigDecimal getCanadaTaxRate(String province) {
        // Simplified Canadian tax rates (GST + PST)
        switch (province.toUpperCase()) {
            case "ON":
                return new BigDecimal("0.13"); // HST
            case "BC":
                return new BigDecimal("0.12"); // GST + PST
            case "QC":
                return new BigDecimal("0.14975"); // GST + QST
            default:
                return new BigDecimal("0.05"); // GST only
        }
    }
}