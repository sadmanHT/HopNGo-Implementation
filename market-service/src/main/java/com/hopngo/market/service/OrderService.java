package com.hopngo.market.service;

import com.hopngo.market.entity.*;
import com.hopngo.market.repository.OrderRepository;
import com.hopngo.market.repository.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private PaymentService paymentService;
    
    // In-memory cart storage (in production, consider using Redis)
    private final Map<UUID, Cart> userCarts = new ConcurrentHashMap<>();
    
    // Cart management
    public Cart getOrCreateCart(UUID userId) {
        logger.debug("Getting or creating cart for user: {}", userId);
        return userCarts.computeIfAbsent(userId, k -> new Cart(userId));
    }
    
    public Cart addItemToCart(UUID userId, UUID productId, int quantity, boolean isRental, Integer rentalDays) {
        logger.info("Adding item to cart - User: {}, Product: {}, Quantity: {}, Rental: {}", 
                   userId, productId, quantity, isRental);
        
        Cart cart = getOrCreateCart(userId);
        Optional<Product> productOpt = productService.getProductById(productId);
        
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        
        Product product = productOpt.get();
        
        // Validate product availability
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is not active: " + productId);
        }
        
        if (isRental && !product.isAvailableForRental()) {
            throw new IllegalArgumentException("Product is not available for rental: " + productId);
        }
        
        if (!isRental && !product.isAvailableForPurchase()) {
            throw new IllegalArgumentException("Product is not available for purchase: " + productId);
        }
        
        // Check stock availability
        if (isRental && !product.canReduceRentalStock(quantity)) {
            throw new IllegalArgumentException("Insufficient rental stock for product: " + productId);
        }
        
        if (!isRental && !product.canReducePurchaseStock(quantity)) {
            throw new IllegalArgumentException("Insufficient purchase stock for product: " + productId);
        }
        
        // Add item to cart
        CartItem cartItem = new CartItem(productId, product.getName(), product.getSku(), 
                                       quantity, isRental, rentalDays);
        
        if (isRental) {
            cartItem.setUnitPrice(product.getRentalPricePerDay());
        } else {
            cartItem.setUnitPrice(product.getPurchasePrice());
        }
        
        cart.addItem(cartItem);
        return cart;
    }
    
    public Cart removeItemFromCart(UUID userId, UUID productId) {
        logger.info("Removing item from cart - User: {}, Product: {}", userId, productId);
        Cart cart = getOrCreateCart(userId);
        cart.removeItem(productId);
        return cart;
    }
    
    public Cart updateCartItemQuantity(UUID userId, UUID productId, int quantity) {
        logger.info("Updating cart item quantity - User: {}, Product: {}, Quantity: {}", 
                   userId, productId, quantity);
        Cart cart = getOrCreateCart(userId);
        cart.updateItemQuantity(productId, quantity);
        return cart;
    }
    
    public void clearCart(UUID userId) {
        logger.info("Clearing cart for user: {}", userId);
        userCarts.remove(userId);
    }
    
    // Order creation and management
    public Order createOrderFromCart(UUID userId, OrderType orderType, 
                                   String shippingAddress, String billingAddress,
                                   LocalDate rentalStartDate, LocalDate rentalEndDate) {
        logger.info("Creating order from cart - User: {}, Type: {}", userId, orderType);
        
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        
        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setType(orderType);
        order.setStatus(OrderStatus.CREATED);
        order.setCurrency("USD");
        
        // Set addresses (simplified - in real app, parse address properly)
        if (shippingAddress != null) {
            order.setShippingAddressLine1(shippingAddress);
        }
        if (billingAddress != null) {
            order.setBillingAddressLine1(billingAddress);
        }
        
        // Set rental dates if applicable
        if (orderType == OrderType.RENTAL) {
            if (rentalStartDate == null || rentalEndDate == null) {
                throw new IllegalArgumentException("Rental dates are required for rental orders");
            }
            order.setRentalStartDate(rentalStartDate);
            order.setRentalEndDate(rentalEndDate);
            order.setRentalDays((int) java.time.temporal.ChronoUnit.DAYS.between(rentalStartDate, rentalEndDate) + 1);
        }
        
        // Save order first to get ID
        order = orderRepository.save(order);
        
        // Create order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (CartItem cartItem : cart.getItems()) {
            Optional<Product> productOpt = productService.getProductById(cartItem.getProductId());
            if (productOpt.isEmpty()) {
                throw new IllegalArgumentException("Product not found: " + cartItem.getProductId());
            }
            
            Product product = productOpt.get();
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setProductSku(product.getSku());
            orderItem.setProductDescription(product.getDescription());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setCurrency("USD");
            orderItem.setRental(cartItem.isRental());
            
            if (cartItem.isRental()) {
                orderItem.setRentalDays(cartItem.getRentalDays());
                orderItem.setRentalStartDate(rentalStartDate);
                orderItem.setRentalEndDate(rentalEndDate);
            }
            
            orderItem.calculateTotalPrice();
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
            
            orderItems.add(orderItem);
        }
        
        // Save order items
        orderItemRepository.saveAll(orderItems);
        
        // Update order total
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);
        
        // Reserve stock for all items
        for (CartItem cartItem : cart.getItems()) {
            productService.reserveStock(cartItem.getProductId(), cartItem.getQuantity());
        }
        
        // Clear cart after successful order creation
        clearCart(userId);
        
        logger.info("Order created successfully: {}", order.getId());
        return order;
    }
    
    public Order getOrderById(UUID orderId) {
        logger.debug("Fetching order by ID: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
    
    public Page<Order> getOrdersByUser(UUID userId, Pageable pageable) {
        logger.debug("Fetching orders for user: {}", userId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        logger.debug("Fetching orders by status: {}", status);
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    
    public List<OrderItem> getOrderItems(UUID orderId) {
        logger.debug("Fetching order items for order: {}", orderId);
        return orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
    
    // Order status management
    public Order markOrderAsPaid(UUID orderId) {
        logger.info("Marking order as paid: {}", orderId);
        Order order = getOrderById(orderId);
        
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Order cannot be marked as paid. Current status: " + order.getStatus());
        }
        
        order.markAsPaid();
        
        // Reduce actual stock and release reserved stock
        List<OrderItem> orderItems = getOrderItems(orderId);
        for (OrderItem item : orderItems) {
            if (item.isRental()) {
                productService.updateRentalStock(item.getProductId(), item.getQuantity());
            } else {
                productService.updatePurchaseStock(item.getProductId(), item.getQuantity());
            }
            productService.releaseReservedStock(item.getProductId(), item.getQuantity());
        }
        
        return orderRepository.save(order);
    }
    
    public Order markOrderAsShipped(UUID orderId, String trackingNumber, String carrier) {
        logger.info("Marking order as shipped: {}, tracking: {}", orderId, trackingNumber);
        Order order = getOrderById(orderId);
        
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order cannot be marked as shipped. Current status: " + order.getStatus());
        }
        
        order.markAsShipped();
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(carrier);
        
        return orderRepository.save(order);
    }
    
    public Order markOrderAsDelivered(UUID orderId) {
        logger.info("Marking order as delivered: {}", orderId);
        Order order = getOrderById(orderId);
        
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order cannot be marked as delivered. Current status: " + order.getStatus());
        }
        
        order.markAsDelivered();
        return orderRepository.save(order);
    }
    
    public Order cancelOrder(UUID orderId, String reason) {
        logger.info("Cancelling order: {}, reason: {}", orderId, reason);
        Order order = getOrderById(orderId);
        
        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled. Current status: " + order.getStatus());
        }
        
        order.cancel();
        order.setNotes(reason);
        
        // Release reserved stock
        List<OrderItem> orderItems = getOrderItems(orderId);
        for (OrderItem item : orderItems) {
            productService.releaseReservedStock(item.getProductId(), item.getQuantity());
        }
        
        return orderRepository.save(order);
    }
    
    // Checkout process
    public Order checkout(UUID userId, OrderType orderType, String shippingAddress, 
                         String billingAddress, LocalDate rentalStartDate, 
                         LocalDate rentalEndDate, PaymentProvider paymentProvider) {
        logger.info("Starting checkout process for user: {}", userId);
        
        // Create order from cart
        Order order = createOrderFromCart(userId, orderType, shippingAddress, 
                                        billingAddress, rentalStartDate, rentalEndDate);
        
        // Create payment intent
        Payment payment = paymentService.createPaymentIntent(order, paymentProvider);
        
        logger.info("Checkout completed - Order: {}, Payment: {}", order.getId(), payment.getId());
        return order;
    }
    
    // Inner classes for cart management
    public static class Cart {
        private final UUID userId;
        private final Map<UUID, CartItem> items = new HashMap<>();
        private LocalDateTime lastModified = LocalDateTime.now();
        
        public Cart(UUID userId) {
            this.userId = userId;
        }
        
        public void addItem(CartItem item) {
            CartItem existingItem = items.get(item.getProductId());
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            } else {
                items.put(item.getProductId(), item);
            }
            lastModified = LocalDateTime.now();
        }
        
        public void removeItem(UUID productId) {
            items.remove(productId);
            lastModified = LocalDateTime.now();
        }
        
        public void updateItemQuantity(UUID productId, int quantity) {
            CartItem item = items.get(productId);
            if (item != null) {
                if (quantity <= 0) {
                    items.remove(productId);
                } else {
                    item.setQuantity(quantity);
                }
                lastModified = LocalDateTime.now();
            }
        }
        
        public BigDecimal getTotalAmount() {
            return items.values().stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        // Getters
        public UUID getUserId() { return userId; }
        public Collection<CartItem> getItems() { return items.values(); }
        public LocalDateTime getLastModified() { return lastModified; }
        public int getItemCount() { return items.size(); }
        public int getTotalQuantity() {
            return items.values().stream().mapToInt(CartItem::getQuantity).sum();
        }
    }
    
    public static class CartItem {
        private UUID productId;
        private String productName;
        private String productSku;
        private int quantity;
        private BigDecimal unitPrice;
        private boolean isRental;
        private Integer rentalDays;
        
        public CartItem(UUID productId, String productName, String productSku, 
                       int quantity, boolean isRental, Integer rentalDays) {
            this.productId = productId;
            this.productName = productName;
            this.productSku = productSku;
            this.quantity = quantity;
            this.isRental = isRental;
            this.rentalDays = rentalDays;
        }
        
        public BigDecimal getTotalPrice() {
            if (isRental && rentalDays != null) {
                return unitPrice.multiply(BigDecimal.valueOf(quantity))
                               .multiply(BigDecimal.valueOf(rentalDays));
            }
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        // Getters and setters
        public UUID getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getProductSku() { return productSku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public boolean isRental() { return isRental; }
        public Integer getRentalDays() { return rentalDays; }
    }
}