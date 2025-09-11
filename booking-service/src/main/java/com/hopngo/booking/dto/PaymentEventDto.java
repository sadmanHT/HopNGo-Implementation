package com.hopngo.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {
    
    @JsonProperty("eventType")
    private String eventType; // payment.succeeded or payment.failed
    
    @JsonProperty("orderId")
    private UUID orderId;
    
    @JsonProperty("bookingId")
    private UUID bookingId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("txnRef")
    private String txnRef;
    
    @JsonProperty("paymentIntentId")
    private String paymentIntentId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("reason")
    private String reason; // For failed payments
    
    @JsonProperty("metadata")
    private java.util.Map<String, Object> metadata;
    
    public boolean isSucceeded() {
        return "payment.succeeded".equals(eventType);
    }
    
    public boolean isFailed() {
        return "payment.failed".equals(eventType);
    }
    
    // Manual getters for Lombok fallback
    public String getEventType() { return eventType; }
    public UUID getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
}