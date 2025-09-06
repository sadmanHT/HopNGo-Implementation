package com.hopngo.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for payment intent creation.
 * Contains information needed by the client to complete payment.
 */
public class PaymentIntentResponse {
    
    @JsonProperty("client_secret")
    private String clientSecret;
    
    @JsonProperty("payment_intent_id")
    private String paymentIntentId;
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("provider")
    private String provider;
    
    // Default constructor
    public PaymentIntentResponse() {}
    
    // Constructor with all fields
    public PaymentIntentResponse(String clientSecret, String paymentIntentId, Long amount, String currency, String status, String provider) {
        this.clientSecret = clientSecret;
        this.paymentIntentId = paymentIntentId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.provider = provider;
    }
    
    // Getters and setters
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getPaymentIntentId() {
        return paymentIntentId;
    }
    
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }
    
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
}