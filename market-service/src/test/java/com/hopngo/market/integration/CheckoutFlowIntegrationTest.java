package com.hopngo.market.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.market.entity.*;
import com.hopngo.market.repository.*;
import com.hopngo.market.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.cloud.stream.bindings.payment-events-out-0.destination=test.payment.events",
    "spring.cloud.stream.bindings.order-events-out-0.destination=test.order.events"
})
@Transactional
class CheckoutFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private StreamBridge streamBridge;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private Product testProduct;
    private String userId;

    @BeforeEach
    void setUp() {
        // Clear repositories
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // Create test product
        testProduct = new Product();
        testProduct.setSku("TEST-SKU-001");
        testProduct.setName("Test Product");
        testProduct.setDescription("A test product for integration testing");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setRentalPricePerDay(new BigDecimal("9.99"));
        testProduct.setCategory("Electronics");
        testProduct.setBrand("TestBrand");
        testProduct.setStockQuantity(10);
        testProduct.setIsAvailableForPurchase(true);
        testProduct.setIsAvailableForRental(true);
        testProduct.setActive(true);
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());
        testProduct = productRepository.save(testProduct);

        userId = "test-user-123";

        // Mock StreamBridge
        when(streamBridge.send(any(String.class), any(Object.class))).thenReturn(true);
    }

    @Test
    void testCompleteCheckoutFlow_Purchase() throws Exception {
        // Step 1: Add item to cart
        String addToCartRequest = objectMapper.writeValueAsString(new AddToCartRequest(
            testProduct.getId(),
            2,
            "PURCHASE",
            null,
            null
        ));

        mockMvc.perform(post("/api/cart/add")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addToCartRequest))
                .andExpect(status().isOk());

        // Step 2: Get cart to verify item was added
        mockMvc.perform(get("/api/cart")
                .header("X-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].type").value("PURCHASE"));

        // Step 3: Checkout
        String checkoutRequest = objectMapper.writeValueAsString(new CheckoutRequest(
            "MOCK",
            "123 Test Street",
            "Test City",
            "12345",
            "Test Country"
        ));

        String checkoutResponse = mockMvc.perform(post("/api/cart/checkout")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.paymentId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CheckoutResponse response = objectMapper.readValue(checkoutResponse, CheckoutResponse.class);
        UUID orderId = response.getOrderId();
        UUID paymentId = response.getPaymentId();

        // Verify order was created
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        assertThat(orderOpt).isPresent();
        Order order = orderOpt.get();
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getType()).isEqualTo(OrderType.PURCHASE);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("199.98")); // 2 * 99.99

        // Verify order items were created
        List<OrderItem> orderItems = orderItemRepository.findByOrder_IdOrderByCreatedAtAsc(orderId);
        assertThat(orderItems).hasSize(1);
        OrderItem orderItem = orderItems.get(0);
        assertThat(orderItem.getProductId()).isEqualTo(testProduct.getId());
        assertThat(orderItem.getQuantity()).isEqualTo(2);
        assertThat(orderItem.getUnitPrice()).isEqualTo(new BigDecimal("99.99"));

        // Verify payment was created
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        assertThat(paymentOpt).isPresent();
        Payment payment = paymentOpt.get();
        assertThat(payment.getOrder().getId()).isEqualTo(orderId);
        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("199.98"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getProvider()).isEqualTo(PaymentProvider.MOCK);

        // Step 4: Process payment (simulate webhook)
        mockMvc.perform(post("/api/payments/mock/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProcessMockPaymentRequest(paymentId))))
                .andExpect(status().isOk());

        // Verify payment status updated
        payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        // Verify order status updated
        order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // Verify product stock was reduced
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(8); // 10 - 2

        // Verify events were published
        verify(streamBridge, atLeastOnce()).send(eq("payment-events-out-0"), any());
        verify(streamBridge, atLeastOnce()).send(eq("order-events-out-0"), any());
    }

    @Test
    void testCompleteCheckoutFlow_Rental() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(3);

        // Step 1: Add rental item to cart
        String addToCartRequest = objectMapper.writeValueAsString(new AddToCartRequest(
            testProduct.getId(),
            1,
            "RENTAL",
            startDate,
            endDate
        ));

        mockMvc.perform(post("/api/cart/add")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addToCartRequest))
                .andExpect(status().isOk());

        // Step 2: Checkout
        String checkoutRequest = objectMapper.writeValueAsString(new CheckoutRequest(
            "MOCK",
            "123 Test Street",
            "Test City",
            "12345",
            "Test Country"
        ));

        String checkoutResponse = mockMvc.perform(post("/api/cart/checkout")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CheckoutResponse response = objectMapper.readValue(checkoutResponse, CheckoutResponse.class);
        UUID orderId = response.getOrderId();

        // Verify rental order was created correctly
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getType()).isEqualTo(OrderType.RENTAL);
        assertThat(order.getRentalStartDate()).isEqualTo(startDate);
        assertThat(order.getRentalEndDate()).isEqualTo(endDate);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("19.98")); // 2 days * 9.99

        // Verify rental order item
        List<OrderItem> orderItems = orderItemRepository.findByOrder_IdOrderByCreatedAtAsc(orderId);
        assertThat(orderItems).hasSize(1);
        OrderItem orderItem = orderItems.get(0);
        assertThat(orderItem.getRentalStartDate()).isEqualTo(startDate);
        assertThat(orderItem.getRentalEndDate()).isEqualTo(endDate);
        assertThat(orderItem.getUnitPrice()).isEqualTo(new BigDecimal("9.99"));
    }

    @Test
    void testCheckoutFlow_InsufficientStock() throws Exception {
        // Try to add more items than available stock
        String addToCartRequest = objectMapper.writeValueAsString(new AddToCartRequest(
            testProduct.getId(),
            15, // More than available stock (10)
            "PURCHASE",
            null,
            null
        ));

        mockMvc.perform(post("/api/cart/add")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addToCartRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock for product: Test Product"));
    }

    @Test
    void testCheckoutFlow_EmptyCart() throws Exception {
        // Try to checkout with empty cart
        String checkoutRequest = objectMapper.writeValueAsString(new CheckoutRequest(
            "MOCK",
            "123 Test Street",
            "Test City",
            "12345",
            "Test Country"
        ));

        mockMvc.perform(post("/api/cart/checkout")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }

    // Inner classes for request/response DTOs
    private static class AddToCartRequest {
        public UUID productId;
        public Integer quantity;
        public String type;
        public LocalDateTime rentalStartDate;
        public LocalDateTime rentalEndDate;

        public AddToCartRequest(UUID productId, Integer quantity, String type, 
                               LocalDateTime rentalStartDate, LocalDateTime rentalEndDate) {
            this.productId = productId;
            this.quantity = quantity;
            this.type = type;
            this.rentalStartDate = rentalStartDate;
            this.rentalEndDate = rentalEndDate;
        }
    }

    private static class CheckoutRequest {
        public String paymentProvider;
        public String shippingAddress;
        public String shippingCity;
        public String shippingPostalCode;
        public String shippingCountry;

        public CheckoutRequest(String paymentProvider, String shippingAddress, 
                              String shippingCity, String shippingPostalCode, String shippingCountry) {
            this.paymentProvider = paymentProvider;
            this.shippingAddress = shippingAddress;
            this.shippingCity = shippingCity;
            this.shippingPostalCode = shippingPostalCode;
            this.shippingCountry = shippingCountry;
        }
    }

    private static class CheckoutResponse {
        public UUID orderId;
        public UUID paymentId;
        public String message;

        public UUID getOrderId() { return orderId; }
        public UUID getPaymentId() { return paymentId; }
    }

    private static class ProcessMockPaymentRequest {
        public UUID paymentId;

        public ProcessMockPaymentRequest(UUID paymentId) {
            this.paymentId = paymentId;
        }
    }
}