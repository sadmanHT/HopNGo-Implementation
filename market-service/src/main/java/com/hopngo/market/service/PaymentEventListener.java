package com.hopngo.market.service;

import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.OrderStatus;
import com.hopngo.market.entity.Payment;
import com.hopngo.market.repository.OrderRepository;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.service.EventPublishingService.PaymentSucceededEvent;
import com.hopngo.market.service.EventPublishingService.PaymentFailedEvent;
import com.hopngo.market.service.EventPublishingService.PaymentCanceledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service that listens to payment events and updates order status accordingly.
 * Handles the business logic for order status transitions based on payment events.
 */
@Service
@Transactional
public class PaymentEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private EventPublishingService eventPublishingService;
    
    /**
     * Handles payment succeeded events by updating order status to PAID.
     * This is triggered when webhooks confirm successful payments.
     */
    @EventListener
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        logger.info("Handling payment succeeded event for payment: {}, order: {}", 
            event.getPaymentId(), event.getOrderId());
        
        try {
            // Find the payment to get the order
            Optional<Payment> paymentOpt = paymentRepository.findById(event.getPaymentId());
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for payment succeeded event: {}", event.getPaymentId());
                return;
            }
            
            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();
            
            // Check if order status update is needed
            if (order.getStatus() == OrderStatus.PAID) {
                logger.info("Order already marked as PAID: {}", order.getId());
                return;
            }
            
            // Validate order status transition
            if (!canTransitionToPaid(order.getStatus())) {
                logger.warn("Cannot transition order {} from status {} to PAID", 
                    order.getId(), order.getStatus());
                return;
            }
            
            // Update order status to PAID
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(payment.getProcessedAt());
            
            order = orderRepository.save(order);
            
            // Publish order status changed event
            eventPublishingService.publishOrderStatusChangedEvent(order, previousStatus.toString());
            
            logger.info("Order status updated to PAID for order: {} due to payment: {}", 
                order.getId(), payment.getId());
                
        } catch (Exception e) {
            logger.error("Error handling payment succeeded event for payment: {}", 
                event.getPaymentId(), e);
            // Don't rethrow - we don't want to break the payment processing
        }
    }
    
    /**
     * Handles payment failed events by updating order status if needed.
     */
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        logger.info("Handling payment failed event for payment: {}, order: {}", 
            event.getPaymentId(), event.getOrderId());
        
        try {
            // Find the payment to get the order
            Optional<Payment> paymentOpt = paymentRepository.findById(event.getPaymentId());
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for payment failed event: {}", event.getPaymentId());
                return;
            }
            
            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();
            
            // For failed payments, we might want to keep the order in PENDING status
            // to allow retry, or move to PAYMENT_FAILED status
            if (order.getStatus() == OrderStatus.PENDING) {
                logger.info("Order {} remains in PENDING status after payment failure, allowing retry", 
                    order.getId());
                // Could optionally set a payment failure count or timestamp
            }
            
        } catch (Exception e) {
            logger.error("Error handling payment failed event for payment: {}", 
                event.getPaymentId(), e);
        }
    }
    
    /**
     * Handles payment canceled events.
     */
    @EventListener
    public void handlePaymentCanceled(PaymentCanceledEvent event) {
        logger.info("Handling payment canceled event for payment: {}, order: {}", 
            event.getPaymentId(), event.getOrderId());
        
        try {
            // Find the payment to get the order
            Optional<Payment> paymentOpt = paymentRepository.findById(event.getPaymentId());
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for payment canceled event: {}", event.getPaymentId());
                return;
            }
            
            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();
            
            // For canceled payments, keep order in PENDING to allow new payment attempts
            logger.info("Order {} remains in PENDING status after payment cancellation", order.getId());
            
        } catch (Exception e) {
            logger.error("Error handling payment canceled event for payment: {}", 
                event.getPaymentId(), e);
        }
    }
    
    /**
     * Determines if an order can transition to PAID status.
     */
    private boolean canTransitionToPaid(OrderStatus currentStatus) {
        switch (currentStatus) {
            case CREATED:
            case PENDING:
                return true;
            case PAID:
            case SHIPPED:
            case DELIVERED:
            case CANCELLED:
                return false;
            default:
                logger.warn("Unknown order status for transition check: {}", currentStatus);
                return false;
        }
    }
    
    /**
     * Gets order processing statistics.
     */
    @Transactional(readOnly = true)
    public OrderProcessingStats getOrderProcessingStats() {
        OrderProcessingStats stats = new OrderProcessingStats();
        
        // Count orders by status
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            stats.addStatusCount(status, count);
        }
        
        return stats;
    }
    
    /**
     * Statistics class for order processing.
     */
    public static class OrderProcessingStats {
        private final java.util.Map<OrderStatus, Long> statusCounts = new java.util.HashMap<>();
        
        public void addStatusCount(OrderStatus status, Long count) {
            statusCounts.put(status, count);
        }
        
        public java.util.Map<OrderStatus, Long> getStatusCounts() {
            return statusCounts;
        }
        
        public Long getPaidOrderCount() {
            return statusCounts.getOrDefault(OrderStatus.PAID, 0L);
        }
        
        public Long getPendingOrderCount() {
            return statusCounts.getOrDefault(OrderStatus.PENDING, 0L);
        }
        
        public Long getCancelledOrderCount() {
            return statusCounts.getOrDefault(OrderStatus.CANCELLED, 0L);
        }
        
        public double getPaymentSuccessRate() {
            long totalOrders = statusCounts.values().stream().mapToLong(Long::longValue).sum();
            if (totalOrders == 0) return 0.0;
            
            long paidOrders = getPaidOrderCount();
            return (double) paidOrders / totalOrders * 100.0;
        }
    }
}