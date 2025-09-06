package com.hopngo.market.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.market.dto.PaymentIntentRequest;
import com.hopngo.market.dto.PaymentIntentResponse;
import com.hopngo.market.entity.*;
import com.hopngo.market.repository.OrderRepository;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the complete payment workflow:
 * 1. Create payment intent
 * 2. Process webhook
 * 3. Verify order status update
 * 4. Verify event publishing
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class PaymentWorkflowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Create test order
        testOrder = new Order();
        testOrder.setTotalAmount(new BigDecimal("150.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        // Customer info would be handled separately in a real implementation
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    void testCompletePaymentWorkflow_MockProvider_Success() throws Exception {
        // Step 1: Create payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("BDT");
        request.setProvider("MOCK");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").exists())
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.status").value("requires_payment_method"))
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Verify payment was created in database
        Optional<Payment> createdPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(createdPayment.isPresent());
        assertEquals(PaymentStatus.PENDING, createdPayment.get().getStatus());
        assertEquals("MOCK", createdPayment.get().getProvider());

        // Step 2: Simulate webhook for successful payment
        String webhookPayload = String.format(
            "{\"id\":\"mock_evt_%d\",\"type\":\"payment.succeeded\",\"payment_intent_id\":\"%s\"}",
            System.currentTimeMillis(),
            paymentResponse.getPaymentIntentId()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "MOCK")
                .header("X-Mock-Signature", "valid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Verify payment status updated
        Optional<Payment> updatedPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(updatedPayment.isPresent());
        assertEquals(PaymentStatus.SUCCEEDED, updatedPayment.get().getStatus());

        // Step 4: Verify order status updated
        Optional<Order> updatedOrder = orderRepository.findById(testOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.PAID, updatedOrder.get().getStatus());

        // Step 5: Verify webhook event was recorded
        List<WebhookEvent> webhookEvents = webhookEventRepository.findByProvider("MOCK");
        assertFalse(webhookEvents.isEmpty());
        WebhookEvent webhookEvent = webhookEvents.get(0);
        assertEquals(WebhookEventStatus.PROCESSED, webhookEvent.getStatus());
        assertEquals("MOCK", webhookEvent.getProvider());
        assertNotNull(webhookEvent.getPaymentId());
        assertEquals(updatedPayment.get().getId(), webhookEvent.getPaymentId());
    }

    @Test
    void testCompletePaymentWorkflow_PaymentFailed() throws Exception {
        // Step 1: Create payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("BDT");
        request.setProvider("MOCK");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Step 2: Simulate webhook for failed payment
        String webhookPayload = String.format(
            "{\"id\":\"mock_evt_failed_%d\",\"type\":\"payment.failed\",\"payment_intent_id\":\"%s\"}",
            System.currentTimeMillis(),
            paymentResponse.getPaymentIntentId()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "MOCK")
                .header("X-Mock-Signature", "valid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Verify payment status updated to failed
        Optional<Payment> updatedPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(updatedPayment.isPresent());
        assertEquals(PaymentStatus.FAILED, updatedPayment.get().getStatus());

        // Step 4: Verify order status updated to failed
        Optional<Order> updatedOrder = orderRepository.findById(testOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.CANCELLED, updatedOrder.get().getStatus());
    }

    @Test
    void testWebhookIdempotency() throws Exception {
        // Step 1: Create payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("BDT");
        request.setProvider("MOCK");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        String webhookId = "mock_evt_idempotent_" + System.currentTimeMillis();
        String webhookPayload = String.format(
            "{\"id\":\"%s\",\"type\":\"payment.succeeded\",\"payment_intent_id\":\"%s\"}",
            webhookId,
            paymentResponse.getPaymentIntentId()
        );

        // Step 2: Process webhook first time
        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "MOCK")
                .header("X-Mock-Signature", "valid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Process same webhook again (should be idempotent)
        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "MOCK")
                .header("X-Mock-Signature", "valid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook already processed (idempotent)"));

        // Step 4: Verify only one webhook event was created
        Optional<WebhookEvent> webhookEventOpt = webhookEventRepository.findByWebhookIdAndProvider(webhookId, "MOCK");
        assertTrue(webhookEventOpt.isPresent());
        assertEquals(WebhookEventStatus.PROCESSED, webhookEventOpt.get().getStatus());

        // Step 5: Verify payment status is still correct
        Optional<Payment> payment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(payment.isPresent());
        assertEquals(PaymentStatus.SUCCEEDED, payment.get().getStatus());
    }

    @Test
    void testWebhookSignatureVerification_InvalidSignature() throws Exception {
        // Step 1: Create payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("BDT");
        request.setProvider("MOCK");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Step 2: Try to process webhook with invalid signature
        String webhookPayload = String.format(
            "{\"id\":\"mock_evt_invalid_%d\",\"type\":\"payment.succeeded\",\"payment_intent_id\":\"%s\"}",
            System.currentTimeMillis(),
            paymentResponse.getPaymentIntentId()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "MOCK")
                .header("X-Mock-Signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isBadRequest());

        // Step 3: Verify payment status unchanged
        Optional<Payment> payment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(payment.isPresent());
        assertEquals(PaymentStatus.PENDING, payment.get().getStatus());

        // Step 4: Verify order status unchanged
        Optional<Order> order = orderRepository.findById(testOrder.getId());
        assertTrue(order.isPresent());
        assertEquals(OrderStatus.PENDING, order.get().getStatus());
    }

    @Test
    void testWebhookStats() throws Exception {
        // Create multiple webhook events
        for (int i = 0; i < 3; i++) {
            PaymentIntentRequest request = new PaymentIntentRequest();
            request.setOrderId(testOrder.getId());
            request.setAmount(new BigDecimal("100.00"));
            request.setCurrency("BDT");
            request.setProvider("MOCK");

            MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            PaymentIntentResponse paymentResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                PaymentIntentResponse.class
            );

            String webhookPayload = String.format(
                "{\"id\":\"mock_evt_stats_%d_%d\",\"type\":\"payment.succeeded\",\"payment_intent_id\":\"%s\"}",
                i, System.currentTimeMillis(),
                paymentResponse.getPaymentIntentId()
            );

            mockMvc.perform(post("/market/payments/webhook")
                    .param("provider", "MOCK")
                    .header("X-Mock-Signature", "valid_signature")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(webhookPayload))
                    .andExpect(status().isOk());
        }

        // Check webhook stats
        mockMvc.perform(post("/market/payments/webhook/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(3))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.by_provider.MOCK").value(3));
    }

    @Test
    void testUnsupportedProvider() throws Exception {
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("BDT");
        request.setProvider("UNSUPPORTED");

        mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOrderNotFound() throws Exception {
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(UUID.randomUUID()); // Non-existent order
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("BDT");
        request.setProvider("MOCK");

        mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAmountMismatch() throws Exception {
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("999.00")); // Different from order amount
        request.setCurrency("BDT");
        request.setProvider("MOCK");

        mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}