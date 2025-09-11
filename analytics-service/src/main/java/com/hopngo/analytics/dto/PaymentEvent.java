package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event for payment-related activities
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent extends DomainEvent {
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("bookingId")
    private String bookingId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("paymentProvider")
    private String paymentProvider;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("failureReason")
    private String failureReason;
    
    @JsonProperty("processingTime")
    private Long processingTime;
    
    @JsonProperty("fees")
    private BigDecimal fees;
    
    // Default constructor
    public PaymentEvent() {
        super();
    }
    
    @Override
    public EventRequest toAnalyticsEvent() {
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventId(getEventId());
        eventRequest.setEventType("payment_" + determinePaymentAction());
        eventRequest.setEventCategory("payment");
        eventRequest.setUserId(getUserId());
        eventRequest.setSessionId(getSessionId());
        eventRequest.setTimestamp(getTimestamp().toString());
        
        // Build event data
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("paymentId", paymentId);
        eventData.put("bookingId", bookingId);
        eventData.put("amount", amount);
        eventData.put("currency", currency);
        eventData.put("paymentMethod", paymentMethod);
        eventData.put("paymentProvider", paymentProvider);
        eventData.put("status", status);
        eventData.put("transactionId", transactionId);
        eventData.put("failureReason", failureReason);
        eventData.put("processingTime", processingTime);
        eventData.put("fees", fees);
        eventData.put("aggregateId", getAggregateId());
        eventData.put("aggregateType", getAggregateType());
        
        eventRequest.setEventData(eventData);
        eventRequest.setMetadata(getMetadata());
        
        return eventRequest;
    }
    
    private String determinePaymentAction() {
        if (status == null) {
            return "unknown";
        }
        
        return switch (status.toLowerCase()) {
            case "initiated", "pending" -> "initiated";
            case "processing" -> "processing";
            case "completed", "success", "successful" -> "completed";
            case "failed", "failure" -> "failed";
            case "cancelled" -> "cancelled";
            case "refunded" -> "refunded";
            case "partially_refunded" -> "partially_refunded";
            case "disputed" -> "disputed";
            case "chargeback" -> "chargeback";
            default -> "status_changed";
        };
    }
    
    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
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
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentProvider() {
        return paymentProvider;
    }
    
    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public Long getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }
    
    public BigDecimal getFees() {
        return fees;
    }
    
    public void setFees(BigDecimal fees) {
        this.fees = fees;
    }
    
    @Override
    public String toString() {
        return "PaymentEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", bookingId='" + bookingId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", status='" + status + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", processingTime=" + processingTime +
                ", fees=" + fees +
                "} " + super.toString();
    }
}