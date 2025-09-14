package com.hopngo.controller;

import com.hopngo.service.DisputeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DisputeWebhookController.class)
class DisputeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DisputeService disputeService;

    @Autowired
    private ObjectMapper objectMapper;

    private String validStripeSignature;
    private String validBkashSignature;
    private String validNagadSignature;

    @BeforeEach
    void setUp() {
        // Mock valid signatures for testing
        validStripeSignature = "t=1234567890,v1=valid_stripe_signature";
        validBkashSignature = "sha256=valid_bkash_signature";
        validNagadSignature = "sha256=valid_nagad_signature";
    }

    @Test
    void testStripeWebhook_DisputeCreated_Success() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.created",
            "data", Map.of(
                "object", Map.of(
                    "id", "dp_stripe_123",
                    "charge", "ch_stripe_123",
                    "amount", 10000, // $100.00 in cents
                    "reason", "fraudulent",
                    "status", "warning_needs_response",
                    "created", 1234567890
                )
            )
        );

        doNothing().when(disputeService).handleStripeDisputeCreated(
            anyString(), anyString(), any(BigDecimal.class), anyString(), anyString(), any(LocalDateTime.class)
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        verify(disputeService).handleStripeDisputeCreated(
            eq("dp_stripe_123"), eq("ch_stripe_123"), eq(new BigDecimal("100.00")), 
            eq("chargeback"), eq("fraudulent"), any(LocalDateTime.class)
        );
    }

    @Test
    void testStripeWebhook_DisputeUpdated_Success() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.updated",
            "data", Map.of(
                "object", Map.of(
                    "id", "dp_stripe_123",
                    "status", "under_review",
                    "evidence", Map.of(
                        "submission_count", 1
                    ),
                    "created", 1234567890
                )
            )
        );

        doNothing().when(disputeService).handleStripeDisputeUpdated(
            anyString(), anyString(), anyString(), any(LocalDateTime.class)
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));

        verify(disputeService).handleStripeDisputeUpdated(
            eq("dp_stripe_123"), eq("under_review"), anyString(), any(LocalDateTime.class)
        );
    }

    @Test
    void testStripeWebhook_DisputeClosed_Success() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.closed",
            "data", Map.of(
                "object", Map.of(
                    "id", "dp_stripe_123",
                    "status", "won",
                    "evidence", Map.of(
                        "details", Map.of(
                            "submission_count", 1
                        )
                    ),
                    "created", 1234567890
                )
            )
        );

        doNothing().when(disputeService).handleStripeDisputeClosed(
            anyString(), anyString(), anyString(), any(LocalDateTime.class)
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));

        verify(disputeService).handleStripeDisputeClosed(
            eq("dp_stripe_123"), eq("won"), anyString(), any(LocalDateTime.class)
        );
    }

    @Test
    void testStripeWebhook_InvalidSignature() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.created",
            "data", Map.of("object", Map.of("id", "dp_stripe_123"))
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").value("Invalid signature"));

        verify(disputeService, never()).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testStripeWebhook_MissingSignature() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.created",
            "data", Map.of("object", Map.of("id", "dp_stripe_123"))
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").value("Missing signature"));

        verify(disputeService, never()).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testStripeWebhook_UnknownEventType() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "unknown.event.type",
            "data", Map.of("object", Map.of("id", "some_id"))
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ignored"))
            .andExpect(jsonPath("$.message").value("Event type not handled: unknown.event.type"));

        verify(disputeService, never()).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testBkashWebhook_Success() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "event_type", "dispute_created",
            "dispute_id", "dp_bkash_123",
            "transaction_id", "txn_bkash_123",
            "amount", "100.00",
            "dispute_type", "complaint",
            "reason", "service_issue",
            "status", "open",
            "timestamp", "2024-01-15T10:30:00Z"
        );

        doNothing().when(disputeService).handleBkashDispute(
            anyString(), anyString(), any(BigDecimal.class), anyString(), anyString(), anyString(), any(LocalDateTime.class)
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/bkash/disputes")
                .header("X-bKash-Signature", validBkashSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));

        verify(disputeService).handleBkashDispute(
            eq("dp_bkash_123"), eq("txn_bkash_123"), eq(new BigDecimal("100.00")), 
            eq("complaint"), eq("service_issue"), eq("open"), any(LocalDateTime.class)
        );
    }

    @Test
    void testBkashWebhook_InvalidSignature() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "event_type", "dispute_created",
            "dispute_id", "dp_bkash_123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/bkash/disputes")
                .header("X-bKash-Signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").value("Invalid signature"));

        verify(disputeService, never()).handleBkashDispute(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testNagadWebhook_Success() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "event_type", "dispute_created",
            "dispute_id", "dp_nagad_123",
            "transaction_id", "txn_nagad_123",
            "amount", "150.00",
            "dispute_type", "refund_request",
            "reason", "product_not_received",
            "status", "pending",
            "timestamp", "2024-01-15T10:30:00Z"
        );

        doNothing().when(disputeService).handleNagadDispute(
            anyString(), anyString(), any(BigDecimal.class), anyString(), anyString(), anyString(), any(LocalDateTime.class)
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/nagad/disputes")
                .header("X-Nagad-Signature", validNagadSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));

        verify(disputeService).handleNagadDispute(
            eq("dp_nagad_123"), eq("txn_nagad_123"), eq(new BigDecimal("150.00")), 
            eq("refund_request"), eq("product_not_received"), eq("pending"), any(LocalDateTime.class)
        );
    }

    @Test
    void testNagadWebhook_InvalidSignature() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "event_type", "dispute_created",
            "dispute_id", "dp_nagad_123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/nagad/disputes")
                .header("X-Nagad-Signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").value("Invalid signature"));

        verify(disputeService, never()).handleNagadDispute(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testHealthCheck() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/webhooks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.service").value("dispute-webhook"));
    }

    @Test
    void testStripeWebhook_MalformedJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
            .andExpect(status().isBadRequest());

        verify(disputeService, never()).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testStripeWebhook_MissingRequiredFields() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.created",
            "data", Map.of(
                "object", Map.of(
                    "id", "dp_stripe_123"
                    // Missing required fields like charge, amount, etc.
                )
            )
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").contains("Missing required field"));

        verify(disputeService, never()).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testBkashWebhook_MissingRequiredFields() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "event_type", "dispute_created",
            "dispute_id", "dp_bkash_123"
            // Missing required fields like transaction_id, amount, etc.
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/bkash/disputes")
                .header("X-bKash-Signature", validBkashSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").contains("Missing required field"));

        verify(disputeService, never()).handleBkashDispute(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testStripeWebhook_ServiceException() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.created",
            "data", Map.of(
                "object", Map.of(
                    "id", "dp_stripe_123",
                    "charge", "ch_stripe_123",
                    "amount", 10000,
                    "reason", "fraudulent",
                    "status", "warning_needs_response",
                    "created", 1234567890
                )
            )
        );

        doThrow(new RuntimeException("Database error")).when(disputeService)
            .handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").contains("Database error"));

        verify(disputeService).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testStripeWebhook_InvalidAmountFormat() throws Exception {
        // Arrange
        Map<String, Object> webhookPayload = Map.of(
            "type", "charge.dispute.created",
            "data", Map.of(
                "object", Map.of(
                    "id", "dp_stripe_123",
                    "charge", "ch_stripe_123",
                    "amount", "invalid_amount", // Invalid amount format
                    "reason", "fraudulent",
                    "status", "warning_needs_response",
                    "created", 1234567890
                )
            )
        );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/stripe/disputes")
                .header("Stripe-Signature", validStripeSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").contains("Invalid amount format"));

        verify(disputeService, never()).handleStripeDisputeCreated(any(), any(), any(), any(), any(), any());
    }
}