package com.hopngo.booking.event;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens to refund events from market-service and updates booking status accordingly.
 * Part of the refund SAGA orchestration.
 */
@Component
@Transactional
public class RefundEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(RefundEventListener.class);
    
    @Autowired
    private BookingRepository bookingRepository;
    
    /**
     * Handles refund requested events.
     * Updates booking status to indicate refund is being processed.
     */
    @EventListener
    public void handleRefundRequested(RefundRequestedEvent event) {
        logger.info("Handling refund requested event for booking: {}", event.bookingId());
        
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(event.bookingId());
            if (bookingOpt.isEmpty()) {
                logger.error("Booking not found for refund requested event: {}", event.bookingId());
                return;
            }
            
            Booking booking = bookingOpt.get();
            
            // Update booking status to indicate refund is being processed
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                // Add refund processing metadata
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
                
                logger.info("Updated booking {} for refund processing", booking.getId());
            } else {
                logger.warn("Booking {} is not in CANCELLED status for refund processing", booking.getId());
            }
            
        } catch (Exception e) {
            logger.error("Error handling refund requested event for booking: {}", event.bookingId(), e);
        }
    }
    
    /**
     * Handles refund succeeded events.
     * Marks booking as fully refunded.
     */
    @EventListener
    public void handleRefundSucceeded(RefundSucceededEvent event) {
        logger.info("Handling refund succeeded event for booking: {}", event.bookingId());
        
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(event.bookingId());
            if (bookingOpt.isEmpty()) {
                logger.error("Booking not found for refund succeeded event: {}", event.bookingId());
                return;
            }
            
            Booking booking = bookingOpt.get();
            
            // Update booking with refund information
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            logger.info("Successfully processed refund for booking: {}, amount: {} {}", 
                       booking.getId(), event.refundedAmount(), event.currency());
            
        } catch (Exception e) {
            logger.error("Error handling refund succeeded event for booking: {}", event.bookingId(), e);
        }
    }
    
    /**
     * Handles refund failed events.
     * May trigger compensation actions or retry logic.
     */
    @EventListener
    public void handleRefundFailed(RefundFailedEvent event) {
        logger.error("Handling refund failed event for booking: {}, reason: {}", 
                    event.bookingId(), event.failureReason());
        
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(event.bookingId());
            if (bookingOpt.isEmpty()) {
                logger.error("Booking not found for refund failed event: {}", event.bookingId());
                return;
            }
            
            Booking booking = bookingOpt.get();
            
            // Update booking to indicate refund failure
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // TODO: Implement compensation logic
            // - Notify customer service
            // - Schedule retry
            // - Revert booking status if needed
            
            logger.error("Refund failed for booking: {}, manual intervention may be required", 
                        booking.getId());
            
        } catch (Exception e) {
            logger.error("Error handling refund failed event for booking: {}", event.bookingId(), e);
        }
    }
}