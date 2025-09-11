package com.hopngo.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for refund operations from payment providers.
 */
public record RefundResponse(
    String providerRefundId,
    RefundStatus status,
    BigDecimal refundedAmount,
    String currency,
    String message,
    LocalDateTime processedAt
) {
    
    public enum RefundStatus {
        PENDING,
        PROCESSING,
        SUCCEEDED,
        FAILED
    }
    
    public static RefundResponse success(String providerRefundId, BigDecimal amount, String currency) {
        return new RefundResponse(
            providerRefundId,
            RefundStatus.SUCCEEDED,
            amount,
            currency,
            "Refund processed successfully",
            LocalDateTime.now()
        );
    }
    
    public static RefundResponse pending(String providerRefundId, BigDecimal amount, String currency) {
        return new RefundResponse(
            providerRefundId,
            RefundStatus.PENDING,
            amount,
            currency,
            "Refund is being processed",
            LocalDateTime.now()
        );
    }
    
    public static RefundResponse failed(String message) {
        return new RefundResponse(
            null,
            RefundStatus.FAILED,
            BigDecimal.ZERO,
            "USD",
            message,
            LocalDateTime.now()
        );
    }
}