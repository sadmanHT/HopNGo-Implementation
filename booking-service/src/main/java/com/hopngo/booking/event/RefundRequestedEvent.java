package com.hopngo.booking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a refund is requested.
 * This event is consumed by the market-service to initiate refund processing.
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
) {}