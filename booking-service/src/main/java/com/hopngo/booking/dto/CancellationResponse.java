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
) {
    // Convenience methods for backward compatibility with tests
    public boolean isSuccess() {
        return "SUCCESS".equals(status) || "CONFIRMED".equals(status);
    }
    
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
    
    public String getRefundStatus() {
        return refundStatus;
    }
    
    public String getBookingReference() {
        return bookingReference;
    }
}