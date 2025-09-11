package com.hopngo.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CancellationResponse(
    UUID bookingId,
    String bookingReference,
    String status,
    BigDecimal refundAmount,
    String refundStatus,
    String cancellationReason,
    LocalDateTime cancelledAt
) {}