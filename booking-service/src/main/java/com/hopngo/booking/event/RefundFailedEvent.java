package com.hopngo.booking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a refund fails.
 * This event is consumed by the booking-service to update booking status.
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
) {}