package com.hopngo.market.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing foreign exchange rates with BDT as base currency.
 */
@Entity
@Table(name = "fx_rates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"date", "currency"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "rate_to_bdt", nullable = false, precision = 15, scale = 6)
    private BigDecimal rateToBdt;
    
    @Column(length = 50)
    @Builder.Default
    private String source = "MANUAL";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Convert amount from this currency to BDT in minor units.
     */
    public long convertToBdtMinor(long amountMinor) {
        BigDecimal amount = new BigDecimal(amountMinor).divide(new BigDecimal("100"));
        BigDecimal bdtAmount = amount.multiply(rateToBdt);
        return bdtAmount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Convert amount from BDT to this currency in minor units.
     */
    public long convertFromBdtMinor(long bdtAmountMinor) {
        BigDecimal bdtAmount = new BigDecimal(bdtAmountMinor).divide(new BigDecimal("100"));
        BigDecimal amount = bdtAmount.divide(rateToBdt, 2, BigDecimal.ROUND_HALF_UP);
        return amount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Get rate as decimal for display.
     */
    public BigDecimal getRateDecimal() {
        return rateToBdt;
    }
    
    /**
     * Check if this is the base currency (BDT).
     */
    public boolean isBaseCurrency() {
        return "BDT".equals(currency);
    }
    
    /**
     * Check if the rate is current (today's date).
     */
    public boolean isCurrent() {
        return LocalDate.now().equals(date);
    }
    
    /**
     * Check if the rate is stale (older than specified days).
     */
    public boolean isStale(int maxDaysOld) {
        return date.isBefore(LocalDate.now().minusDays(maxDaysOld));
    }
    
    @PrePersist
    @PreUpdate
    private void validateCurrency() {
        if (currency != null) {
            currency = currency.toUpperCase();
            if (currency.length() != 3) {
                throw new IllegalArgumentException("Currency code must be exactly 3 characters");
            }
        }
        
        if (rateToBdt != null && rateToBdt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Exchange rate date cannot be in the future");
        }
    }
}