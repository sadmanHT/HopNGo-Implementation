package com.hopngo.market.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a refund is successfully processed.
 * This completes the refund SAGA orchestration.
 */
public record RefundSucceededEvent(
    UUID refundId,
    UUID bookingId,
    UUID paymentId,
    BigDecimal refundedAmount,
    String currency,
    String providerRefundId,
    String paymentProvider,
    LocalDateTime processedAt
) {
    
    public static RefundSucceededEvent from(com.hopngo.market.entity.Refund refund) {
        return new RefundSucceededEvent(
            refund.getId(),
            refund.getBookingId(),
            refund.getPayment().getId(),
            refund.getAmount(),
            refund.getCurrency(),
            refund.getProviderRefundId(),
            refund.getPayment().getProvider().name(),
            refund.getProcessedAt()
        );
    }
}