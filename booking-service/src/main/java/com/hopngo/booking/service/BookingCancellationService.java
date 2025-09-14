package com.hopngo.booking.service;

import com.hopngo.booking.dto.CancellationRequest;
import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.event.RefundRequestedEvent;
import com.hopngo.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling booking cancellations and initiating refund processes.
 * Part of the refund SAGA orchestration.
 */
@Service
public class BookingCancellationService {

    private static final Logger log = LoggerFactory.getLogger(BookingCancellationService.class);

    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Cancels a booking and initiates refund process if payment was made.
     *
     * @param bookingId the ID of the booking to cancel
     * @param userId the ID of the user requesting cancellation
     * @param cancellationRequest the cancellation request details
     * @return the cancellation response with refund details
     */
    @Transactional
    public com.hopngo.booking.dto.CancellationResponse cancelBooking(UUID bookingId, String userId, CancellationRequest cancellationRequest) {
        log.info("Cancelling booking: {} for user: {} with reason: {}", bookingId, userId, cancellationRequest.reason());

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Validate user authorization
        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: User " + userId + " cannot cancel booking " + bookingId);
        }

        // Validate booking can be cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed booking");
        }

        // Check if check-in date has passed
        if (booking.getCheckInDate() != null && booking.getCheckInDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot cancel booking after check-in date");
        }

        // Calculate refund amount
        BigDecimal refundAmount = calculateRefundAmount(booking);

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        
        // Add cancellation reason if booking entity supports it
        if (cancellationRequest.reason() != null) {
            booking.setCancellationReason(cancellationRequest.reason());
        }

        booking = bookingRepository.save(booking);

        // If booking was paid, initiate refund process
        if (booking.getPaymentId() != null && booking.getTotalPrice() != null) {
            initiateRefund(booking, cancellationRequest.reason());
        }

        log.info("Successfully cancelled booking: {}", bookingId);
        
        // Return cancellation response
        return new com.hopngo.booking.dto.CancellationResponse(
            booking.getId(),
            booking.getBookingReference(),
            "SUCCESS",
            refundAmount,
            "PENDING",
            cancellationRequest.reason(),
            LocalDateTime.now()
        );
    }

    /**
     * Initiates refund process by publishing RefundRequestedEvent.
     *
     * @param booking the cancelled booking
     * @param reason the cancellation reason
     */
    private void initiateRefund(Booking booking, String reason) {
        log.info("Initiating refund for booking: {}", booking.getId());

        RefundRequestedEvent refundEvent = new RefundRequestedEvent(
            UUID.randomUUID(), // refundId
            booking.getId(),
            booking.getPaymentId(),
            booking.getTotalPrice(),
            "USD", // Default currency - should come from booking
            reason,
            "stripe", // Default payment provider - should come from booking
            booking.getPaymentId().toString(), // provider payment ID
            LocalDateTime.now()
        );

        eventPublisher.publishEvent(refundEvent);
        log.info("Published refund requested event for booking: {}", booking.getId());
    }

    /**
     * Checks if a booking can be cancelled based on cancellation policies.
     *
     * @param booking the booking to check
     * @return true if booking can be cancelled
     */
    public boolean canCancelBooking(Booking booking) {
        return booking.getStatus() != BookingStatus.CANCELLED && 
               booking.getStatus() != BookingStatus.COMPLETED;
    }

    /**
     * Calculates refund amount based on cancellation policies.
     *
     * @param booking the booking being cancelled
     * @return the refund amount
     */
    public BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }

        // Get cancellation policies
        var policies = booking.getCancellationPoliciesAsMap();
        if (policies.isEmpty()) {
            // Default: full refund
            return booking.getTotalAmount();
        }

        // Calculate hours until check-in
        if (booking.getCheckInDate() == null) {
            return booking.getTotalAmount();
        }

        long hoursUntilCheckIn = java.time.Duration.between(LocalDateTime.now(), booking.getCheckInDate()).toHours();

        // Get policy values with defaults
        int freeUntilHours = getIntValue(policies, "free_until_hours", 48);
        int partialPercent = getIntValue(policies, "partial_pct", 50);
        int cutoffHours = getIntValue(policies, "cutoff_hours", 24);

        if (hoursUntilCheckIn >= freeUntilHours) {
            // Free cancellation window
            return booking.getTotalAmount();
        } else if (hoursUntilCheckIn >= cutoffHours) {
            // Partial refund window
            return booking.getTotalAmount().multiply(BigDecimal.valueOf(partialPercent)).divide(BigDecimal.valueOf(100));
        } else {
            // No refund
            return BigDecimal.ZERO;
        }
    }

    private int getIntValue(java.util.Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}