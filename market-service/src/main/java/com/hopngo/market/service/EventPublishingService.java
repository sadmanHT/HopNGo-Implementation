package com.hopngo.market.service;

import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EventPublishingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublishingService.class);
    
    @Autowired
    private StreamBridge streamBridge;
    
    // Publish payment succeeded event
    public void publishPaymentSucceededEvent(Payment payment) {
        logger.info("Publishing payment succeeded event for payment: {}", payment.getId());
        
        Map<String, Object> event = createPaymentEvent(payment, "payment.succeeded");
        
        try {
            streamBridge.send("payment-events-out-0", event);
            logger.info("Payment succeeded event published successfully for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to publish payment succeeded event for payment: {}", payment.getId(), e);
            throw new RuntimeException("Failed to publish payment succeeded event", e);
        }
    }
    
    // Publish payment failed event
    public void publishPaymentFailedEvent(Payment payment) {
        logger.info("Publishing payment failed event for payment: {}", payment.getId());
        
        Map<String, Object> event = createPaymentEvent(payment, "payment.failed");
        
        try {
            streamBridge.send("payment-events-out-0", event);
            logger.info("Payment failed event published successfully for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to publish payment failed event for payment: {}", payment.getId(), e);
            throw new RuntimeException("Failed to publish payment failed event", e);
        }
    }
    
    // Publish payment canceled event
    public void publishPaymentCanceledEvent(Payment payment) {
        logger.info("Publishing payment canceled event for payment: {}", payment.getId());
        
        Map<String, Object> event = createPaymentEvent(payment, "payment.canceled");
        
        try {
            streamBridge.send("payment-events-out-0", event);
            logger.info("Payment canceled event published successfully for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to publish payment canceled event for payment: {}", payment.getId(), e);
            throw new RuntimeException("Failed to publish payment canceled event", e);
        }
    }
    
    // Publish order status changed event
    public void publishOrderStatusChangedEvent(Order order, String previousStatus) {
        logger.info("Publishing order status changed event for order: {}, status: {} -> {}", 
                   order.getId(), previousStatus, order.getStatus());
        
        Map<String, Object> event = createOrderEvent(order, "order.status.changed");
        event.put("previousStatus", previousStatus);
        
        try {
            streamBridge.send("order-events-out-0", event);
            logger.info("Order status changed event published successfully for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish order status changed event for order: {}", order.getId(), e);
            throw new RuntimeException("Failed to publish order status changed event", e);
        }
    }
    
    // Publish order created event
    public void publishOrderCreatedEvent(Order order) {
        logger.info("Publishing order created event for order: {}", order.getId());
        
        Map<String, Object> event = createOrderEvent(order, "order.created");
        
        try {
            streamBridge.send("order-events-out-0", event);
            logger.info("Order created event published successfully for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish order created event for order: {}", order.getId(), e);
            throw new RuntimeException("Failed to publish order created event", e);
        }
    }
    
    // Create payment event structure
    private Map<String, Object> createPaymentEvent(Payment payment, String eventType) {
        Map<String, Object> event = new HashMap<>();
        
        // Event metadata
        event.put("eventType", eventType);
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("source", "market-service");
        event.put("version", "1.0");
        
        // Payment data
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", payment.getId().toString());
        paymentData.put("orderId", payment.getOrder().getId().toString());
        paymentData.put("userId", payment.getOrder().getUserId().toString());
        paymentData.put("status", payment.getStatus().toString());
        paymentData.put("provider", payment.getProvider().toString());
        paymentData.put("amount", payment.getAmount());
        paymentData.put("currency", payment.getCurrency());
        paymentData.put("transactionReference", payment.getTransactionReference());
        
        if (payment.getProviderTransactionId() != null) {
            paymentData.put("providerTransactionId", payment.getProviderTransactionId());
        }
        
        if (payment.getPaymentIntentId() != null) {
            paymentData.put("paymentIntentId", payment.getPaymentIntentId());
        }
        
        if (payment.getFailureReason() != null) {
            paymentData.put("failureReason", payment.getFailureReason());
        }
        
        if (payment.getProcessedAt() != null) {
            paymentData.put("processedAt", payment.getProcessedAt().toString());
        }
        
        event.put("payment", paymentData);
        
        // Order context
        Map<String, Object> orderContext = new HashMap<>();
        orderContext.put("orderId", payment.getOrder().getId().toString());
        orderContext.put("orderStatus", payment.getOrder().getStatus().toString());
        orderContext.put("orderType", payment.getOrder().getType().toString());
        orderContext.put("totalAmount", payment.getOrder().getTotalAmount());
        
        event.put("order", orderContext);
        
        return event;
    }
    
    // Create order event structure
    private Map<String, Object> createOrderEvent(Order order, String eventType) {
        Map<String, Object> event = new HashMap<>();
        
        // Event metadata
        event.put("eventType", eventType);
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("source", "market-service");
        event.put("version", "1.0");
        
        // Order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getId().toString());
        orderData.put("userId", order.getUserId().toString());
        orderData.put("status", order.getStatus().toString());
        orderData.put("type", order.getType().toString());
        orderData.put("totalAmount", order.getTotalAmount());
        orderData.put("currency", order.getCurrency());
        orderData.put("itemCount", order.getOrderItems().size());
        
        if (order.getTrackingNumber() != null) {
            orderData.put("trackingNumber", order.getTrackingNumber());
        }
        
        if (order.getRentalStartDate() != null) {
            orderData.put("rentalStartDate", order.getRentalStartDate().toString());
        }
        
        if (order.getRentalEndDate() != null) {
            orderData.put("rentalEndDate", order.getRentalEndDate().toString());
        }
        
        event.put("order", orderData);
        
        return event;
    }
    
    // Publish generic event (for future extensibility)
    public void publishEvent(String bindingName, String eventType, Map<String, Object> eventData) {
        logger.info("Publishing generic event: {} to binding: {}", eventType, bindingName);
        
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("source", "market-service");
        event.put("version", "1.0");
        event.putAll(eventData);
        
        try {
            streamBridge.send(bindingName, event);
            logger.info("Generic event published successfully: {} to binding: {}", eventType, bindingName);
        } catch (Exception e) {
            logger.error("Failed to publish generic event: {} to binding: {}", eventType, bindingName, e);
            throw new RuntimeException("Failed to publish event: " + eventType, e);
        }
    }

    // Event classes for internal use
    public static class PaymentSucceededEvent {
        private final UUID paymentId;
        private final UUID orderId;

        public PaymentSucceededEvent(UUID paymentId, UUID orderId) {
            this.paymentId = paymentId;
            this.orderId = orderId;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public UUID getOrderId() {
            return orderId;
        }
    }

    public static class PaymentFailedEvent {
        private final UUID paymentId;
        private final UUID orderId;

        public PaymentFailedEvent(UUID paymentId, UUID orderId) {
            this.paymentId = paymentId;
            this.orderId = orderId;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public UUID getOrderId() {
            return orderId;
        }
    }

    public static class PaymentCanceledEvent {
        private final UUID paymentId;
        private final UUID orderId;

        public PaymentCanceledEvent(UUID paymentId, UUID orderId) {
            this.paymentId = paymentId;
            this.orderId = orderId;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public UUID getOrderId() {
            return orderId;
        }
    }
}