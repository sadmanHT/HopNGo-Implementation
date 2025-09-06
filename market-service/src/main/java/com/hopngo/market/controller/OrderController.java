package com.hopngo.market.controller;

import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.OrderStatus;
import com.hopngo.market.service.OrderService;
import static com.hopngo.market.entity.OrderStatus.*;
import com.hopngo.market.entity.OrderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/market/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    // Get order by ID
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        
        logger.info("Getting order: {}", orderId);
        
        try {
            Order order = orderService.getOrderById(orderId);
            OrderResponse response = new OrderResponse(order);
            logger.info("Order found: {}, status: {}", orderId, order.getStatus());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Order not found: {}", orderId);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get orders by user ID
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user ID", description = "Retrieve all orders for a specific user")
    public ResponseEntity<Page<OrderResponse>> getOrdersByUserId(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Filter by order status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Getting orders for user: {}, status: {}", userId, status);
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Order> orders;
        if (status != null) {
            orders = orderService.getOrdersByUserIdAndStatus(userId, status, pageable);
        } else {
            orders = orderService.getOrdersByUserId(userId, pageable);
        }
        
        Page<OrderResponse> response = orders.map(OrderResponse::new);
        
        logger.info("Retrieved {} orders for user: {}", orders.getTotalElements(), userId);
        return ResponseEntity.ok(response);
    }
    
    // Get order by tracking number
    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Get order by tracking number", description = "Retrieve order details by tracking number")
    public ResponseEntity<OrderResponse> getOrderByTrackingNumber(
            @Parameter(description = "Tracking number") @PathVariable String trackingNumber) {
        
        logger.info("Getting order by tracking number: {}", trackingNumber);
        
        Optional<Order> order = orderService.getOrderByTrackingNumber(trackingNumber);
        
        if (order.isPresent()) {
            OrderResponse response = new OrderResponse(order.get());
            logger.info("Order found by tracking number: {}, orderId: {}", trackingNumber, order.get().getId());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Order not found with tracking number: {}", trackingNumber);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Update order status to shipped
    @PostMapping("/{orderId}/ship")
    @Operation(summary = "Mark order as shipped", description = "Update order status to shipped and set tracking number")
    public ResponseEntity<OrderResponse> shipOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId,
            @Valid @RequestBody ShipOrderRequest request) {
        
        logger.info("Shipping order: {}, tracking: {}", orderId, request.getTrackingNumber());
        
        try {
            Order order = orderService.markOrderAsShipped(orderId, request.getTrackingNumber(), "Default Carrier");
            
            OrderResponse response = new OrderResponse(order);
            logger.info("Order shipped successfully: {}", orderId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to ship order: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot ship order: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error shipping order: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Update order status to delivered
    @PostMapping("/{orderId}/deliver")
    @Operation(summary = "Mark order as delivered", description = "Update order status to delivered")
    public ResponseEntity<OrderResponse> deliverOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        
        logger.info("Delivering order: {}", orderId);
        
        try {
            Order order = orderService.markOrderAsDelivered(orderId);
            
            OrderResponse response = new OrderResponse(order);
            logger.info("Order delivered successfully: {}", orderId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to deliver order: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot deliver order: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error delivering order: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Cancel order
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order and restore product stock")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        
        logger.info("Cancelling order: {}, reason: {}", orderId, request.getReason());
        
        try {
            Order order = orderService.cancelOrder(orderId, request.getReason());
            
            OrderResponse response = new OrderResponse(order);
            logger.info("Order cancelled successfully: {}", orderId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to cancel order: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot cancel order: {}, error: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error cancelling order: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Get all orders (admin endpoint)
    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieve all orders with optional filters (admin only)")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @Parameter(description = "Filter by order status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Getting all orders, status filter: {}", status);
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Order> orders;
        if (status != null) {
            orders = orderService.getOrdersByStatus(status, pageable);
        } else {
            orders = orderService.getAllOrders(pageable);
        }
        
        Page<OrderResponse> response = orders.map(OrderResponse::new);
        
        logger.info("Retrieved {} orders", orders.getTotalElements());
        return ResponseEntity.ok(response);
    }
    
    // Request DTOs
    public static class ShipOrderRequest {
        @NotBlank
        private String trackingNumber;
        
        public ShipOrderRequest() {}
        
        public ShipOrderRequest(String trackingNumber) {
            this.trackingNumber = trackingNumber;
        }
        
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    }
    
    public static class CancelOrderRequest {
        @NotBlank
        private String reason;
        
        public CancelOrderRequest() {}
        
        public CancelOrderRequest(String reason) {
            this.reason = reason;
        }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    // Response DTO
    public static class OrderResponse {
        private UUID id;
        private UUID userId;
        private String status;
        private String type;
        private java.math.BigDecimal totalAmount;
        private String currency;
        private String shippingAddress;
        private String billingAddress;
        private String trackingNumber;
        private java.time.LocalDate rentalStartDate;
        private java.time.LocalDate rentalEndDate;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime paidAt;
        private java.time.LocalDateTime shippedAt;
        private java.time.LocalDateTime deliveredAt;
        private java.time.LocalDateTime cancelledAt;
        private int itemCount;
        
        public OrderResponse(Order order) {
            this.id = order.getId();
            this.userId = order.getUserId();
            this.status = order.getStatus().toString();
            this.type = order.getOrderType().toString();
            this.totalAmount = order.getTotalAmount();
            this.currency = order.getCurrency();
            this.shippingAddress = order.getShippingAddress();
            this.billingAddress = order.getBillingAddress();
            this.trackingNumber = order.getTrackingNumber();
            this.rentalStartDate = order.getRentalStartDate() != null ? order.getRentalStartDate().toLocalDate() : null;
            this.rentalEndDate = order.getRentalEndDate() != null ? order.getRentalEndDate().toLocalDate() : null;
            this.createdAt = order.getCreatedAt();
            this.paidAt = order.getPayment() != null ? order.getPayment().getProcessedAt() : null;
            this.shippedAt = order.getStatus() == SHIPPED || order.getStatus() == DELIVERED ? order.getUpdatedAt() : null;
            this.deliveredAt = order.getActualDeliveryDate();
            this.cancelledAt = order.getStatus() == OrderStatus.CANCELLED ? order.getUpdatedAt() : null;
            this.itemCount = order.getOrderItems() != null ? order.getOrderItems().size() : 0;
        }
        
        // Getters
        public UUID getId() { return id; }
        public UUID getUserId() { return userId; }
        public String getStatus() { return status; }
        public String getType() { return type; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public String getCurrency() { return currency; }
        public String getShippingAddress() { return shippingAddress; }
        public String getBillingAddress() { return billingAddress; }
        public String getTrackingNumber() { return trackingNumber; }
        public java.time.LocalDate getRentalStartDate() { return rentalStartDate; }
        public java.time.LocalDate getRentalEndDate() { return rentalEndDate; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public java.time.LocalDateTime getPaidAt() { return paidAt; }
        public java.time.LocalDateTime getShippedAt() { return shippedAt; }
        public java.time.LocalDateTime getDeliveredAt() { return deliveredAt; }
        public java.time.LocalDateTime getCancelledAt() { return cancelledAt; }
        public int getItemCount() { return itemCount; }
    }
}