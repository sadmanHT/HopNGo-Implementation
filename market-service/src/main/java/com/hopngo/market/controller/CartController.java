package com.hopngo.market.controller;

import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.OrderType;
import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.PaymentProvider;
import com.hopngo.market.service.OrderService;
import com.hopngo.market.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/market/carts")
@Tag(name = "Cart & Checkout", description = "Shopping cart and checkout endpoints")
public class CartController {
    
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService paymentService;
    
    // Add item to cart
    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to cart", description = "Add a product to user's shopping cart")
    public ResponseEntity<CartResponse> addItemToCart(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody AddToCartRequest request) {
        
        logger.info("Adding item to cart - userId: {}, productId: {}, quantity: {}", 
                   userId, request.getProductId(), request.getQuantity());
        
        try {
            orderService.addToCart(userId, request.getProductId(), request.getQuantity(), 
                                 request.getOrderType(), request.getRentalDays());
            
            OrderService.Cart cart = orderService.getCart(userId);
            CartResponse response = new CartResponse(cart);
            
            logger.info("Item added to cart successfully - userId: {}, cart items: {}", 
                       userId, cart.getItems().size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to add item to cart - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Get cart contents
    @GetMapping("/{userId}")
    @Operation(summary = "Get cart contents", description = "Retrieve user's shopping cart contents")
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        logger.info("Getting cart contents for user: {}", userId);
        
        OrderService.Cart cart = orderService.getCart(userId);
        CartResponse response = new CartResponse(cart);
        
        logger.info("Retrieved cart for user: {}, items: {}, total: {}", 
                   userId, cart.getItems().size(), cart.getTotalAmount());
        
        return ResponseEntity.ok(response);
    }
    
    // Update cart item
    @PutMapping("/{userId}/items/{productId}")
    @Operation(summary = "Update cart item", description = "Update quantity of an item in the cart")
    public ResponseEntity<CartResponse> updateCartItem(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        
        logger.info("Updating cart item - userId: {}, productId: {}, quantity: {}", 
                   userId, productId, request.getQuantity());
        
        try {
            orderService.updateCartItem(userId, productId, request.getQuantity());
            
            OrderService.Cart cart = orderService.getCart(userId);
            CartResponse response = new CartResponse(cart);
            
            logger.info("Cart item updated successfully - userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update cart item - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Remove item from cart
    @DeleteMapping("/{userId}/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Remove an item from the cart")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Product ID") @PathVariable UUID productId) {
        
        logger.info("Removing item from cart - userId: {}, productId: {}", userId, productId);
        
        try {
            orderService.removeFromCart(userId, productId);
            
            OrderService.Cart cart = orderService.getCart(userId);
            CartResponse response = new CartResponse(cart);
            
            logger.info("Item removed from cart successfully - userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to remove item from cart - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Clear cart
    @DeleteMapping("/{userId}")
    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        logger.info("Clearing cart for user: {}", userId);
        
        orderService.clearCart(userId);
        
        logger.info("Cart cleared successfully for user: {}", userId);
        return ResponseEntity.noContent().build();
    }
    
    // Checkout - create order and payment intent
    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Checkout", description = "Create order from cart and initiate payment")
    public ResponseEntity<CheckoutResponse> checkout(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody CheckoutRequest request) {
        
        logger.info("Processing checkout for user: {}", userId);
        
        try {
            // Create order from cart
            Order order = orderService.createOrderFromCart(
                userId, 
                request.getShippingAddress(), 
                request.getBillingAddress(),
                request.getRentalStartDate(),
                request.getRentalEndDate()
            );
            
            // Create payment intent
            PaymentProvider provider = request.getPaymentProvider() != null ? 
                request.getPaymentProvider() : PaymentProvider.MOCK;
            
            Payment payment = paymentService.createPaymentIntent(order, provider);
            
            CheckoutResponse response = new CheckoutResponse(
                order.getId(),
                payment.getId(),
                payment.getPaymentIntentId(),
                order.getTotalAmount(),
                order.getCurrency(),
                payment.getStatus().toString()
            );
            
            logger.info("Checkout completed successfully - userId: {}, orderId: {}, paymentId: {}", 
                       userId, order.getId(), payment.getId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Checkout failed for user: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Checkout error for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Request DTOs
    public static class AddToCartRequest {
        @NotNull
        private UUID productId;
        
        @Positive
        private int quantity;
        
        private OrderType orderType = OrderType.PURCHASE;
        private Integer rentalDays;
        
        // Constructors
        public AddToCartRequest() {}
        
        public AddToCartRequest(UUID productId, int quantity, OrderType orderType, Integer rentalDays) {
            this.productId = productId;
            this.quantity = quantity;
            this.orderType = orderType;
            this.rentalDays = rentalDays;
        }
        
        // Getters and setters
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public OrderType getOrderType() { return orderType; }
        public void setOrderType(OrderType orderType) { this.orderType = orderType; }
        
        public Integer getRentalDays() { return rentalDays; }
        public void setRentalDays(Integer rentalDays) { this.rentalDays = rentalDays; }
    }
    
    public static class UpdateCartItemRequest {
        @Positive
        private int quantity;
        
        // Constructors
        public UpdateCartItemRequest() {}
        
        public UpdateCartItemRequest(int quantity) {
            this.quantity = quantity;
        }
        
        // Getters and setters
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    
    public static class CheckoutRequest {
        @NotNull
        private String shippingAddress;
        
        private String billingAddress;
        private PaymentProvider paymentProvider = PaymentProvider.MOCK;
        private LocalDate rentalStartDate;
        private LocalDate rentalEndDate;
        
        // Constructors
        public CheckoutRequest() {}
        
        public CheckoutRequest(String shippingAddress, String billingAddress, PaymentProvider paymentProvider) {
            this.shippingAddress = shippingAddress;
            this.billingAddress = billingAddress;
            this.paymentProvider = paymentProvider;
        }
        
        // Getters and setters
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        
        public String getBillingAddress() { return billingAddress; }
        public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
        
        public PaymentProvider getPaymentProvider() { return paymentProvider; }
        public void setPaymentProvider(PaymentProvider paymentProvider) { this.paymentProvider = paymentProvider; }
        
        public LocalDate getRentalStartDate() { return rentalStartDate; }
        public void setRentalStartDate(LocalDate rentalStartDate) { this.rentalStartDate = rentalStartDate; }
        
        public LocalDate getRentalEndDate() { return rentalEndDate; }
        public void setRentalEndDate(LocalDate rentalEndDate) { this.rentalEndDate = rentalEndDate; }
    }
    
    // Response DTOs
    public static class CartResponse {
        private List<CartItemResponse> items;
        private BigDecimal totalAmount;
        private String currency;
        private int itemCount;
        
        public CartResponse(OrderService.Cart cart) {
            this.items = cart.getItems().stream()
                .map(CartItemResponse::new)
                .collect(java.util.stream.Collectors.toList());
            this.totalAmount = cart.getTotalAmount();
            this.currency = "USD"; // Default currency
            this.itemCount = cart.getItems().size();
        }
        
        // Getters
        public List<CartItemResponse> getItems() { return items; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getCurrency() { return currency; }
        public int getItemCount() { return itemCount; }
    }
    
    public static class CartItemResponse {
        private UUID productId;
        private String productName;
        private String productSku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private OrderType orderType;
        private Integer rentalDays;
        
        public CartItemResponse(OrderService.CartItem item) {
            this.productId = item.getProduct().getId();
            this.productName = item.getProduct().getName();
            this.productSku = item.getProduct().getSku();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.totalPrice = item.getTotalPrice();
            this.orderType = item.getOrderType();
            this.rentalDays = item.getRentalDays();
        }
        
        // Getters
        public UUID getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getProductSku() { return productSku; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public OrderType getOrderType() { return orderType; }
        public Integer getRentalDays() { return rentalDays; }
    }
    
    public static class CheckoutResponse {
        private UUID orderId;
        private UUID paymentId;
        private String paymentIntentId;
        private BigDecimal totalAmount;
        private String currency;
        private String paymentStatus;
        
        public CheckoutResponse(UUID orderId, UUID paymentId, String paymentIntentId, 
                              BigDecimal totalAmount, String currency, String paymentStatus) {
            this.orderId = orderId;
            this.paymentId = paymentId;
            this.paymentIntentId = paymentIntentId;
            this.totalAmount = totalAmount;
            this.currency = currency;
            this.paymentStatus = paymentStatus;
        }
        
        // Getters
        public UUID getOrderId() { return orderId; }
        public UUID getPaymentId() { return paymentId; }
        public String getPaymentIntentId() { return paymentIntentId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getCurrency() { return currency; }
        public String getPaymentStatus() { return paymentStatus; }
    }
}