package com.hopngo.booking.service;

import com.hopngo.booking.dto.PaymentEventDto;
import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class PaymentEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final BookingRepository bookingRepository;
    private final OutboxService outboxService;
    private final InventoryService inventoryService;
    private final ProcessedEventService processedEventService;
    
    @Autowired
    public PaymentEventConsumer(BookingRepository bookingRepository,
                               OutboxService outboxService,
                               InventoryService inventoryService,
                               ProcessedEventService processedEventService) {
        this.bookingRepository = bookingRepository;
        this.outboxService = outboxService;
        this.inventoryService = inventoryService;
        this.processedEventService = processedEventService;
    }
    
    @Bean
    public Consumer<Message<PaymentEventDto>> paymentEventHandler() {
        return message -> {
            try {
                PaymentEventDto event = message.getPayload();
                String messageId = (String) message.getHeaders().get("messageId");
                
                log.info("Received payment event: {} for booking: {}, messageId: {}", 
                        event.getEventType(), event.getBookingId(), messageId);
                
                // Check for idempotency
                if (messageId != null && processedEventService.isEventProcessed(messageId)) {
                    log.info("Event {} already processed, skipping", messageId);
                    return;
                }
                
                processPaymentEvent(event, messageId);
                
            } catch (Exception e) {
                log.error("Error processing payment event: {}", e.getMessage(), e);
                throw e; // Re-throw to trigger retry mechanism
            }
        };
    }
    
    @Transactional
    public void processPaymentEvent(PaymentEventDto event, String messageId) {
        try {
            if (event.getBookingId() == null) {
                log.warn("Payment event missing bookingId, skipping: {}", event);
                return;
            }
            
            Optional<Booking> bookingOpt = bookingRepository.findById(event.getBookingId());
            if (bookingOpt.isEmpty()) {
                log.warn("Booking not found for payment event: {}", event.getBookingId());
                return;
            }
            
            Booking booking = bookingOpt.get();
            
            // Validate booking is in PENDING state
            if (booking.getStatus() != BookingStatus.PENDING) {
                log.warn("Booking {} is not in PENDING state, current status: {}", 
                        booking.getId(), booking.getStatus());
                return;
            }
            
            if (event.isSucceeded()) {
                handlePaymentSucceeded(booking, event);
            } else if (event.isFailed()) {
                handlePaymentFailed(booking, event);
            } else {
                log.warn("Unknown payment event type: {}", event.getEventType());
            }
            
            // Mark event as processed for idempotency
            if (messageId != null) {
                processedEventService.markEventAsProcessed(messageId, event.getEventType());
            }
            
        } catch (Exception e) {
            log.error("Error processing payment event for booking {}: {}", 
                    event.getBookingId(), e.getMessage(), e);
            throw e;
        }
    }
    
    private void handlePaymentSucceeded(Booking booking, PaymentEventDto event) {
        log.info("Confirming booking {} after successful payment", booking.getId());
        
        // Validate payment amount matches booking amount
        if (event.getAmount() != null && 
            booking.getTotalAmount().compareTo(event.getAmount()) != 0) {
            log.warn("Payment amount {} does not match booking amount {} for booking {}", 
                    event.getAmount(), booking.getTotalAmount(), booking.getId());
        }
        
        // Confirm the booking
        booking.confirm();
        bookingRepository.save(booking);
        
        // Publish booking confirmed event via outbox
        outboxService.publishBookingConfirmedEvent(booking);
        
        log.info("Booking {} confirmed successfully", booking.getId());
    }
    
    private void handlePaymentFailed(Booking booking, PaymentEventDto event) {
        log.info("Cancelling booking {} after failed payment: {}", 
                booking.getId(), event.getReason());
        
        // Cancel the booking
        booking.cancel();
        bookingRepository.save(booking);
        
        // Release inventory
        try {
            inventoryService.releaseInventoryForBooking(booking);
            log.info("Inventory released for cancelled booking {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to release inventory for booking {}: {}", 
                    booking.getId(), e.getMessage(), e);
            // Don't fail the entire transaction for inventory release issues
        }
        
        // Publish booking cancelled event via outbox
        outboxService.publishBookingCancelledEvent(booking);
        
        log.info("Booking {} cancelled successfully due to payment failure", booking.getId());
    }
}