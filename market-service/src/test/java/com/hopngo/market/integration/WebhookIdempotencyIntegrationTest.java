package com.hopngo.market.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.market.entity.*;
import com.hopngo.market.repository.*;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
class WebhookIdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private StreamBridge streamBridge;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private Payment testPayment;
    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clear repositories
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // Create test product
        testProduct = new Product();
        testProduct.setSku("TEST-SKU-WEBHOOK");
        testProduct.setName("Test Webhook Product");
        testProduct.setDescription("A test product for webhook testing");
        testProduct.setPrice(new BigDecimal("50.00"));
        testProduct.setCategory("Test");
        testProduct.setBrand("TestBrand");
        testProduct.setStockQuantity(10);
        testProduct.setIsAvailableForPurchase(true);
        testProduct.setActive(true);
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());
        testProduct = productRepository.save(testProduct);

        // Create test order
        testOrder = new Order();
        testOrder.setUserId(UUID.randomUUID());
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setType(OrderType.PURCHASE);
        testOrder.setTotalAmount(new BigDecimal("50.00"));
        testOrder.setShippingAddress("123 Test Street");
        testOrder.setShippingCity("Test City");
        testOrder.setShippingPostalCode("12345");
        testOrder.setShippingCountry("Test Country");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
        testOrder = orderRepository.save(testOrder);

        // Create test order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(testOrder.getId());
        orderItem.setProductId(testProduct.getId());
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(new BigDecimal("50.00"));
        orderItem.setTotalPrice(new BigDecimal("50.00"));
        orderItem.setProductName("Test Webhook Product");
        orderItem.setProductSku("TEST-SKU-WEBHOOK");
        orderItem.setCreatedAt(LocalDateTime.now());
        orderItem.setUpdatedAt(LocalDateTime.now());
        orderItemRepository.save(orderItem);

        // Create test payment
        testPayment = new Payment();
        testPayment.setOrderId(testOrder.getId());
        testPayment.setAmount(new BigDecimal("50.00"));
        testPayment.setCurrency("USD");
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setProvider(PaymentProvider.MOCK);
        testPayment.setTransactionReference("test-txn-ref-123");
        testPayment.setProviderTransactionId("mock-provider-txn-456");
        testPayment.setPaymentIntentId("mock-intent-789");
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());
        testPayment = paymentRepository.save(testPayment);

        // Mock StreamBridge
        when(streamBridge.send(any(String.class), any(Object.class))).thenReturn(true);
    }

    @Test
    void testWebhookIdempotency_SingleSuccessfulWebhook() throws Exception {
        String webhookPayload = objectMapper.writeValueAsString(new WebhookPayload(
            "payment.succeeded",
            testPayment.getTransactionReference(),
            testPayment.getProviderTransactionId(),
            "succeeded",
            "Payment completed successfully",
            LocalDateTime.now()
        ));

        // First webhook call - should succeed
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", "webhook-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        // Verify payment status updated
        Payment updatedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(updatedPayment.getProcessedAt()).isNotNull();

        // Verify order status updated
        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(updatedOrder.getPaidAt()).isNotNull();

        // Verify event was published
        verify(streamBridge, times(1)).send(eq("payment-events-out-0"), any());
        verify(streamBridge, times(1)).send(eq("order-events-out-0"), any());
    }

    @Test
    void testWebhookIdempotency_DuplicateWebhooks() throws Exception {
        String idempotencyKey = "webhook-key-duplicate-123";
        String webhookPayload = objectMapper.writeValueAsString(new WebhookPayload(
            "payment.succeeded",
            testPayment.getTransactionReference(),
            testPayment.getProviderTransactionId(),
            "succeeded",
            "Payment completed successfully",
            LocalDateTime.now()
        ));

        // First webhook call - should succeed
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        // Second webhook call with same idempotency key - should return cached result
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook already processed"));

        // Third webhook call with same idempotency key - should still return cached result
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook already processed"));

        // Verify payment status is still correct
        Payment updatedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        // Verify events were only published once
        verify(streamBridge, times(1)).send(eq("payment-events-out-0"), any());
        verify(streamBridge, times(1)).send(eq("order-events-out-0"), any());
    }

    @Test
    void testWebhookIdempotency_ConcurrentWebhooks() throws Exception {
        String idempotencyKey = "webhook-key-concurrent-123";
        String webhookPayload = objectMapper.writeValueAsString(new WebhookPayload(
            "payment.succeeded",
            testPayment.getTransactionReference(),
            testPayment.getProviderTransactionId(),
            "succeeded",
            "Payment completed successfully",
            LocalDateTime.now()
        ));

        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        try {
            // Submit 10 concurrent webhook requests with the same idempotency key
            CompletableFuture<Void>[] futures = IntStream.range(0, 10)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        mockMvc.perform(post("/api/payments/webhook")
                                .header("X-Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(webhookPayload))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

            // Wait for all requests to complete
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

            // Verify payment status is correct
            Payment updatedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
            assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

            // Verify order status is correct
            Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);

            // Verify events were only published once despite multiple concurrent requests
            verify(streamBridge, times(1)).send(eq("payment-events-out-0"), any());
            verify(streamBridge, times(1)).send(eq("order-events-out-0"), any());

        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testWebhookIdempotency_FailedWebhook() throws Exception {
        String webhookPayload = objectMapper.writeValueAsString(new WebhookPayload(
            "payment.failed",
            testPayment.getTransactionReference(),
            testPayment.getProviderTransactionId(),
            "failed",
            "Payment failed due to insufficient funds",
            LocalDateTime.now()
        ));

        // First webhook call - should succeed
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", "webhook-key-failed-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        // Verify payment status updated to failed
        Payment updatedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updatedPayment.getFailureReason()).isEqualTo("Payment failed due to insufficient funds");
        assertThat(updatedPayment.getProcessedAt()).isNotNull();

        // Verify order status remains pending
        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Verify event was published
        verify(streamBridge, times(1)).send(eq("payment-events-out-0"), any());
    }

    @Test
    void testWebhookIdempotency_DifferentIdempotencyKeys() throws Exception {
        String webhookPayload = objectMapper.writeValueAsString(new WebhookPayload(
            "payment.succeeded",
            testPayment.getTransactionReference(),
            testPayment.getProviderTransactionId(),
            "succeeded",
            "Payment completed successfully",
            LocalDateTime.now()
        ));

        // First webhook call with first idempotency key
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", "webhook-key-first")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        // Second webhook call with different idempotency key - should be treated as duplicate
        mockMvc.perform(post("/api/payments/webhook")
                .header("X-Idempotency-Key", "webhook-key-second")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook already processed"));

        // Verify payment status is correct
        Payment updatedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        // Verify events were only published once
        verify(streamBridge, times(1)).send(eq("payment-events-out-0"), any());
        verify(streamBridge, times(1)).send(eq("order-events-out-0"), any());
    }

    @Test
    void testWebhookIdempotency_MissingIdempotencyKey() throws Exception {
        String webhookPayload = objectMapper.writeValueAsString(new WebhookPayload(
            "payment.succeeded",
            testPayment.getTransactionReference(),
            testPayment.getProviderTransactionId(),
            "succeeded",
            "Payment completed successfully",
            LocalDateTime.now()
        ));

        // Webhook call without idempotency key - should fail
        mockMvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("X-Idempotency-Key header is required"));

        // Verify payment status unchanged
        Payment unchangedPayment = paymentRepository.findById(testPayment.getId()).orElseThrow();
        assertThat(unchangedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        // Verify no events were published
        verify(streamBridge, never()).send(any(String.class), any());
    }

    // Inner class for webhook payload
    private static class WebhookPayload {
        public String eventType;
        public String transactionReference;
        public String providerTransactionId;
        public String status;
        public String message;
        public LocalDateTime timestamp;

        public WebhookPayload(String eventType, String transactionReference, 
                             String providerTransactionId, String status, 
                             String message, LocalDateTime timestamp) {
            this.eventType = eventType;
            this.transactionReference = transactionReference;
            this.providerTransactionId = providerTransactionId;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}