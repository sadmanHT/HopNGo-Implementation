package com.hopngo.market.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a refund is requested.
 * This triggers the refund SAGA orchestration.
 */
public record RefundRequestedEvent(
    UUID refundId,
    UUID bookingId,
    UUID paymentId,
    BigDecimal refundAmount,
    String currency,
    String reason,
    String paymentProvider,
    String providerPaymentId,
    LocalDateTime requestedAt
) {
    
    public static RefundRequestedEvent from(com.hopngo.market.entity.Refund refund) {
        return new RefundRequestedEvent(
            refund.getId(),
            refund.getBookingId(),
            refund.getPayment().getId(),
            refund.getAmount(),
            refund.getCurrency(),
            refund.getReason(),
            refund.getPayment().getProvider(),
            refund.getPayment().getProviderPaymentId(),
            refund.getCreatedAt()
        );
    }
}