package com.hopngo.market.service;

import com.hopngo.market.dto.RefundResponse;
import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.Refund;
import com.hopngo.market.entity.RefundStatus;
import com.hopngo.market.event.RefundFailedEvent;
import com.hopngo.market.event.RefundRequestedEvent;
import com.hopngo.market.event.RefundSucceededEvent;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.repository.RefundRepository;
import com.hopngo.market.service.payment.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefundService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);
    
    @Autowired
    private RefundRepository refundRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentProviderService paymentProviderService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * Processes a refund for a booking cancellation.
     * 
     * @param bookingId the booking ID to refund
     * @param refundAmount the amount to refund
     * @param reason the reason for the refund
     * @return the created refund entity
     */
    public Refund processBookingRefund(UUID bookingId, BigDecimal refundAmount, String reason) {
        logger.info("Processing refund for booking: {}, amount: {}", bookingId, refundAmount);
        
        // Find the payment for this booking
        Optional<Payment> paymentOpt = paymentRepository.findByOrderBookingId(bookingId);
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("No payment found for booking: " + bookingId);
        }
        
        Payment payment = paymentOpt.get();
        
        // Check if refund already exists
        if (refundRepository.existsByBookingId(bookingId)) {
            throw new IllegalStateException("Refund already exists for booking: " + bookingId);
        }
        
        // Validate refund amount
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original payment amount");
        }
        
        // Create refund record
        Refund refund = new Refund(payment, bookingId, refundAmount, payment.getCurrency(), reason);
        refund = refundRepository.save(refund);
        
        // Publish refund requested event
        eventPublisher.publishEvent(RefundRequestedEvent.from(refund));
        
        // Process refund with payment provider
        try {
            PaymentProvider provider = paymentProviderService.getProvider(payment.getProvider().name());
            RefundResponse response = provider.processRefund(
                payment.getProviderPaymentId(),
                refundAmount,
                payment.getCurrency(),
                reason
            );
            
            // Update refund based on provider response
            updateRefundFromResponse(refund, response);
            
            // Publish refund event
            publishRefundEvent(refund, response);
            
        } catch (Exception e) {
            logger.error("Failed to process refund with provider for booking: {}", bookingId, e);
            refund.markAsFailed("Provider refund failed: " + e.getMessage());
            refundRepository.save(refund);
            
            // Publish failed event
            publishRefundFailedEvent(refund, e.getMessage());
        }
        
        return refund;
    }
    
    /**
     * Gets refund status for a booking.
     */
    public Optional<Refund> getRefundByBookingId(UUID bookingId) {
        List<Refund> refunds = refundRepository.findByBookingId(bookingId);
        return refunds.isEmpty() ? Optional.empty() : Optional.of(refunds.get(0));
    }
    
    /**
     * Gets all refunds for a user.
     */
    public List<Refund> getRefundsByUserId(UUID userId) {
        return refundRepository.findByUserIdAndStatus(userId, RefundStatus.SUCCEEDED);
    }
    
    /**
     * Retries failed refunds.
     */
    public void retryFailedRefunds() {
        List<Refund> failedRefunds = refundRepository.findByStatus(RefundStatus.FAILED);
        
        for (Refund refund : failedRefunds) {
            try {
                logger.info("Retrying failed refund: {}", refund.getId());
                
                PaymentProvider provider = paymentProviderService.getProvider(refund.getPayment().getProvider().name());
                RefundResponse response = provider.processRefund(
                    refund.getPayment().getProviderPaymentId(),
                    refund.getAmount(),
                    refund.getCurrency(),
                    refund.getReason()
                );
                
                updateRefundFromResponse(refund, response);
                publishRefundEvent(refund, response);
                
            } catch (Exception e) {
                logger.error("Retry failed for refund: {}", refund.getId(), e);
            }
        }
    }
    
    private void updateRefundFromResponse(Refund refund, RefundResponse response) {
        switch (response.status()) {
            case SUCCEEDED -> refund.markAsSucceeded(response.providerRefundId());
            case FAILED -> refund.markAsFailed(response.message());
            case PROCESSING -> refund.markAsProcessing();
            case PENDING -> {
                // Keep as pending
                refund.setProviderRefundId(response.providerRefundId());
            }
        }
        refundRepository.save(refund);
    }
    
    private void publishRefundEvent(Refund refund, RefundResponse response) {
        switch (response.status()) {
            case SUCCEEDED -> {
                logger.info("Publishing refund succeeded event for booking: {}", refund.getBookingId());
                eventPublisher.publishEvent(RefundSucceededEvent.from(refund));
            }
            case FAILED -> {
                logger.error("Publishing refund failed event for booking: {}", refund.getBookingId());
                eventPublisher.publishEvent(RefundFailedEvent.from(refund));
            }
            case PROCESSING, PENDING -> {
                logger.info("Refund status: {} for booking: {}, no event published yet", 
                           response.status(), refund.getBookingId());
            }
        }
    }
    
    private void publishRefundFailedEvent(Refund refund, String errorMessage) {
        logger.error("Publishing refund failed event for booking: {}, error: {}", 
                    refund.getBookingId(), errorMessage);
        eventPublisher.publishEvent(RefundFailedEvent.from(refund));
    }
}