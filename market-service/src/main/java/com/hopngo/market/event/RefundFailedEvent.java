package com.hopngo.market.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a refund fails.
 * This triggers compensation actions in the refund SAGA.
 */
public record RefundFailedEvent(
    UUID refundId,
    UUID bookingId,
    UUID paymentId,
    BigDecimal attemptedAmount,
    String currency,
    String paymentProvider,
    String failureReason,
    LocalDateTime failedAt
) {
    
    public static RefundFailedEvent from(com.hopngo.market.entity.Refund refund) {
        return new RefundFailedEvent(
            refund.getId(),
            refund.getBookingId(),
            refund.getPayment().getId(),
            refund.getAmount(),
            refund.getCurrency(),
            refund.getPayment().getProvider(),
            refund.getFailureReason(),
            refund.getProcessedAt()
        );
    }
}