package com.hopngo.market.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a payment intent.
 */
public class PaymentIntentRequest {
    
    @NotNull
    private UUID orderId;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;
    
    @NotBlank
    @Size(max = 10)
    private String currency = "USD";
    
    @NotBlank
    private String provider;
    
    // Constructors
    public PaymentIntentRequest() {}
    
    public PaymentIntentRequest(UUID orderId, BigDecimal amount, String currency, String provider) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
    }
    
    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
}