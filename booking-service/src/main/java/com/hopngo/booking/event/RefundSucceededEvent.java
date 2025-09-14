package com.hopngo.booking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a refund succeeds.
 * This event is consumed by the booking-service to update booking status.
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
) {}