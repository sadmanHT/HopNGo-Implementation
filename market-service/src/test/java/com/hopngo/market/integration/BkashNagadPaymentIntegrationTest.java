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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BKash and Nagad payment providers using mock implementations.
 * Tests the complete payment workflow including webhooks and order status updates.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class BkashNagadPaymentIntegrationTest {

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
        testOrder.setTotalAmount(new BigDecimal("250.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    void testBkashPaymentWorkflow_Success() throws Exception {
        // Step 1: Create BKash payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("BDT");
        request.setProvider("BKASH");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").exists())
                .andExpect(jsonPath("$.paymentIntentId").value(org.hamcrest.Matchers.startsWith("bkash_pi_")))
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.clientSecret").value(org.hamcrest.Matchers.startsWith("bkash_secret_")))
                .andExpect(jsonPath("$.status").value("requires_payment_method"))
                .andExpect(jsonPath("$.provider").value("BKASH"))
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Verify payment was created in database
        Optional<Payment> createdPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(createdPayment.isPresent());
        assertEquals(PaymentStatus.PENDING, createdPayment.get().getStatus());
        assertEquals("BKASH", createdPayment.get().getProvider());

        // Step 2: Simulate BKash webhook for successful payment
        String webhookPayload = String.format(
            "{\"eventType\":\"payment.completed\",\"paymentID\":\"%s\",\"trxID\":\"bkash_txn_%d\",\"amount\":\"250.00\",\"currency\":\"BDT\"}",
            paymentResponse.getPaymentIntentId(),
            System.currentTimeMillis()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "BKASH")
                .header("X-Bkash-Signature", "valid_bkash_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Verify payment status updated
        Optional<Payment> updatedPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(updatedPayment.isPresent());
        assertEquals(PaymentStatus.SUCCEEDED, updatedPayment.get().getStatus());
        assertNotNull(updatedPayment.get().getProviderTransactionId());
        assertTrue(updatedPayment.get().getProviderTransactionId().startsWith("bkash_txn_"));

        // Step 4: Verify order status updated
        Optional<Order> updatedOrder = orderRepository.findById(testOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.PAID, updatedOrder.get().getStatus());

        // Step 5: Verify webhook event was recorded
        Optional<WebhookEvent> webhookEvent = webhookEventRepository.findByProvider("BKASH")
            .stream().findFirst();
        assertTrue(webhookEvent.isPresent());
        assertEquals(WebhookEventStatus.PROCESSED, webhookEvent.get().getStatus());
        assertEquals("BKASH", webhookEvent.get().getProvider());
    }

    @Test
    void testNagadPaymentWorkflow_Success() throws Exception {
        // Step 1: Create Nagad payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("BDT");
        request.setProvider("NAGAD");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").exists())
                .andExpect(jsonPath("$.paymentIntentId").value(org.hamcrest.Matchers.startsWith("nagad_pi_")))
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.clientSecret").value(org.hamcrest.Matchers.startsWith("nagad_secret_")))
                .andExpect(jsonPath("$.status").value("requires_payment_method"))
                .andExpect(jsonPath("$.provider").value("NAGAD"))
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Verify payment was created in database
        Optional<Payment> createdPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(createdPayment.isPresent());
        assertEquals(PaymentStatus.PENDING, createdPayment.get().getStatus());
        assertEquals("NAGAD", createdPayment.get().getProvider());

        // Step 2: Simulate Nagad webhook for successful payment
        String webhookPayload = String.format(
            "{\"status\":\"success\",\"orderId\":\"%s\",\"txnId\":\"nagad_txn_%d\",\"amount\":\"250.00\",\"currency\":\"BDT\"}",
            paymentResponse.getPaymentIntentId(),
            System.currentTimeMillis()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "NAGAD")
                .header("X-Nagad-Signature", "valid_nagad_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Verify payment status updated
        Optional<Payment> updatedPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(updatedPayment.isPresent());
        assertEquals(PaymentStatus.SUCCEEDED, updatedPayment.get().getStatus());
        assertNotNull(updatedPayment.get().getProviderTransactionId());
        assertTrue(updatedPayment.get().getProviderTransactionId().startsWith("nagad_txn_"));

        // Step 4: Verify order status updated
        Optional<Order> updatedOrder = orderRepository.findById(testOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.PAID, updatedOrder.get().getStatus());

        // Step 5: Verify webhook event was recorded
        Optional<WebhookEvent> webhookEvent = webhookEventRepository.findByProvider("NAGAD")
            .stream().findFirst();
        assertTrue(webhookEvent.isPresent());
        assertEquals(WebhookEventStatus.PROCESSED, webhookEvent.get().getStatus());
        assertEquals("NAGAD", webhookEvent.get().getProvider());
    }

    @Test
    void testBkashPaymentFailure() throws Exception {
        // Step 1: Create BKash payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("BDT");
        request.setProvider("BKASH");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Step 2: Simulate BKash webhook for failed payment
        String webhookPayload = String.format(
            "{\"eventType\":\"payment.failed\",\"paymentID\":\"%s\",\"trxID\":\"bkash_txn_%d\",\"errorMessage\":\"Insufficient balance\"}",
            paymentResponse.getPaymentIntentId(),
            System.currentTimeMillis()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "BKASH")
                .header("X-Bkash-Signature", "valid_bkash_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Verify payment status updated to failed
        Optional<Payment> updatedPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(updatedPayment.isPresent());
        assertEquals(PaymentStatus.FAILED, updatedPayment.get().getStatus());

        // Step 4: Verify order status updated to cancelled
        Optional<Order> updatedOrder = orderRepository.findById(testOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.CANCELLED, updatedOrder.get().getStatus());
    }

    @Test
    void testNagadPaymentFailure() throws Exception {
        // Step 1: Create Nagad payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("BDT");
        request.setProvider("NAGAD");

        MvcResult createResult = mockMvc.perform(post("/market/payments/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentIntentResponse paymentResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), 
            PaymentIntentResponse.class
        );

        // Step 2: Simulate Nagad webhook for failed payment
        String webhookPayload = String.format(
            "{\"status\":\"failed\",\"orderId\":\"%s\",\"txnId\":\"nagad_txn_%d\",\"errorMessage\":\"Transaction declined\"}",
            paymentResponse.getPaymentIntentId(),
            System.currentTimeMillis()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "NAGAD")
                .header("X-Nagad-Signature", "valid_nagad_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        // Step 3: Verify payment status updated to failed
        Optional<Payment> updatedPayment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(updatedPayment.isPresent());
        assertEquals(PaymentStatus.FAILED, updatedPayment.get().getStatus());

        // Step 4: Verify order status updated to cancelled
        Optional<Order> updatedOrder = orderRepository.findById(testOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.CANCELLED, updatedOrder.get().getStatus());
    }

    @Test
    void testInvalidWebhookSignature_BKash() throws Exception {
        // Step 1: Create BKash payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("BDT");
        request.setProvider("BKASH");

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
            "{\"eventType\":\"payment.completed\",\"paymentID\":\"%s\",\"trxID\":\"bkash_txn_%d\"}",
            paymentResponse.getPaymentIntentId(),
            System.currentTimeMillis()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "BKASH")
                .header("X-Bkash-Signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isBadRequest());

        // Step 3: Verify payment status unchanged
        Optional<Payment> payment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(payment.isPresent());
        assertEquals(PaymentStatus.PENDING, payment.get().getStatus());
    }

    @Test
    void testInvalidWebhookSignature_Nagad() throws Exception {
        // Step 1: Create Nagad payment intent
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId(testOrder.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("BDT");
        request.setProvider("NAGAD");

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
            "{\"status\":\"success\",\"orderId\":\"%s\",\"txnId\":\"nagad_txn_%d\"}",
            paymentResponse.getPaymentIntentId(),
            System.currentTimeMillis()
        );

        mockMvc.perform(post("/market/payments/webhook")
                .param("provider", "NAGAD")
                .header("X-Nagad-Signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isBadRequest());

        // Step 3: Verify payment status unchanged
        Optional<Payment> payment = paymentRepository.findByPaymentIntentId(paymentResponse.getPaymentIntentId());
        assertTrue(payment.isPresent());
        assertEquals(PaymentStatus.PENDING, payment.get().getStatus());
    }
}