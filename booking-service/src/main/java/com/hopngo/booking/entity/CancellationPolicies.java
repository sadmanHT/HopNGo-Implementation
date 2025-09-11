package com.hopngo.booking.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Cancellation policies for listings stored as JSON.
 * Defines refund rules based on cancellation timing.
 */
public class CancellationPolicies implements Serializable {
    
    /**
     * Hours before start date when cancellation is free (100% refund)
     */
    @JsonProperty("free_until_hours")
    private Integer freeUntilHours;
    
    /**
     * Percentage of refund for partial cancellations (0-100)
     */
    @JsonProperty("partial_pct")
    private BigDecimal partialPercentage;
    
    /**
     * Hours before start date when no refund is allowed
     */
    @JsonProperty("cutoff_hours")
    private Integer cutoffHours;
    
    // Constructors
    public CancellationPolicies() {}
    
    public CancellationPolicies(Integer freeUntilHours, BigDecimal partialPercentage, Integer cutoffHours) {
        this.freeUntilHours = freeUntilHours;
        this.partialPercentage = partialPercentage;
        this.cutoffHours = cutoffHours;
    }
    
    // Getters and Setters
    public Integer getFreeUntilHours() {
        return freeUntilHours;
    }
    
    public void setFreeUntilHours(Integer freeUntilHours) {
        this.freeUntilHours = freeUntilHours;
    }
    
    public BigDecimal getPartialPercentage() {
        return partialPercentage;
    }
    
    public void setPartialPercentage(BigDecimal partialPercentage) {
        this.partialPercentage = partialPercentage;
    }
    
    public Integer getCutoffHours() {
        return cutoffHours;
    }
    
    public void setCutoffHours(Integer cutoffHours) {
        this.cutoffHours = cutoffHours;
    }
    
    @Override
    public String toString() {
        return "CancellationPolicies{" +
                "freeUntilHours=" + freeUntilHours +
                ", partialPercentage=" + partialPercentage +
                ", cutoffHours=" + cutoffHours +
                '}';
    }
}