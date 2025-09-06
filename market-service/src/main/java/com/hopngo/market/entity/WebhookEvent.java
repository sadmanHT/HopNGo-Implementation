package com.hopngo.market.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing webhook events for idempotency tracking.
 * Ensures that webhook events are processed only once.
 */
@Entity
@Table(name = "webhook_events")
public class WebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "webhook_id", nullable = false, unique = true)
    private String webhookId;
    
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "request_body", nullable = false, columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(name = "request_headers", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, String> requestHeaders;
    
    @Column(name = "signature", length = 500)
    private String signature;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "payment_id")
    private UUID paymentId;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor
    public WebhookEvent() {}
    
    // Constructor with required fields
    public WebhookEvent(String webhookId, String provider, String eventType, String requestBody) {
        this.webhookId = webhookId;
        this.provider = provider;
        this.eventType = eventType;
        this.requestBody = requestBody;
    }
    
    // Business methods
    public void markAsProcessing() {
        this.status = WebhookEventStatus.PROCESSING;
    }
    
    public void markAsProcessed() {
        this.status = WebhookEventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String reason) {
        this.status = WebhookEventStatus.FAILED;
        this.failureReason = reason;
        this.retryCount++;
    }
    
    public boolean isProcessed() {
        return status == WebhookEventStatus.PROCESSED;
    }
    
    public boolean canRetry() {
        return retryCount < 3 && status == WebhookEventStatus.FAILED;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getWebhookId() {
        return webhookId;
    }
    
    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getRequestBody() {
        return requestBody;
    }
    
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
    
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }
    
    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public WebhookEventStatus getStatus() {
        return status;
    }
    
    public void setStatus(WebhookEventStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}