package com.hopngo.booking.service;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.CancellationPolicies;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class RefundCalculationService {
    
    public RefundCalculationResult calculateRefund(Booking booking) {
        CancellationPolicies policies = booking.getListing().getCancellationPolicies();
        
        // If no cancellation policies are set, default to no refund
        if (policies == null) {
            return new RefundCalculationResult(
                BigDecimal.ZERO,
                "NO_REFUND",
                "No cancellation policy defined"
            );
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInDateTime = booking.getStartDate().atStartOfDay();
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(now, checkInDateTime);
        
        BigDecimal totalAmount = booking.getTotalAmount();
        
        // Free cancellation period
        if (policies.getFreeUntilHours() != null && hoursUntilCheckIn >= policies.getFreeUntilHours()) {
            return new RefundCalculationResult(
                totalAmount,
                "FULL_REFUND",
                "Cancelled within free cancellation period"
            );
        }
        
        // Partial refund period
        if (policies.getCutoffHours() != null && hoursUntilCheckIn >= policies.getCutoffHours()) {
            if (policies.getPartialPct() != null && policies.getPartialPct() > 0) {
                BigDecimal refundAmount = totalAmount
                    .multiply(BigDecimal.valueOf(policies.getPartialPct()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
                return new RefundCalculationResult(
                    refundAmount,
                    "PARTIAL_REFUND",
                    String.format("Partial refund of %d%% applied", policies.getPartialPct())
                );
            }
        }
        
        // No refund
        return new RefundCalculationResult(
            BigDecimal.ZERO,
            "NO_REFUND",
            "Cancellation is outside refund policy window"
        );
    }
    
    public static class RefundCalculationResult {
        private final BigDecimal refundAmount;
        private final String refundType;
        private final String reason;
        
        public RefundCalculationResult(BigDecimal refundAmount, String refundType, String reason) {
            this.refundAmount = refundAmount;
            this.refundType = refundType;
            this.reason = reason;
        }
        
        public BigDecimal getRefundAmount() {
            return refundAmount;
        }
        
        public String getRefundType() {
            return refundType;
        }
        
        public String getReason() {
            return reason;
        }
    }
}