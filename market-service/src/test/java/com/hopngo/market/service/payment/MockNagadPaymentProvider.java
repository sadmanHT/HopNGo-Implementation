package com.hopngo.market.service.payment;

import com.hopngo.market.entity.Order;
import com.hopngo.market.dto.PaymentIntentResponse;
import com.hopngo.market.dto.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock implementation of NagadPaymentProvider for testing purposes.
 * This mock simulates Nagad API responses without making actual API calls.
 */
@TestComponent
@Profile("test")
public class MockNagadPaymentProvider implements PaymentProvider {

    @Override
    public String name() {
        return "NAGAD";
    }

    @Override
    public PaymentIntentResponse createPaymentIntent(Order order) {
        // Simulate Nagad payment intent creation
        String paymentId = "nagad_pi_" + UUID.randomUUID().toString().substring(0, 8);
        String clientSecret = "nagad_secret_" + UUID.randomUUID().toString().substring(0, 16);
        
        return new PaymentIntentResponse(
            clientSecret,
            paymentId,
            order.getTotalAmount().longValue() * 100, // Convert to paisa
            order.getCurrency(),
            "requires_payment_method",
            "NAGAD"
        );
    }

    @Override
    public boolean verifyWebhook(HttpServletRequest request) {
        // Mock webhook verification - check for test signature
        String signature = request.getHeader("X-Nagad-Signature");
        return "valid_nagad_signature".equals(signature);
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, HttpHeaders headers) {
        // Mock webhook signature verification using HttpHeaders
        String signature = headers.getFirst("X-Nagad-Signature");
        return "valid_nagad_signature".equals(signature);
    }

    @Override
    public RefundResponse processRefund(String paymentId, BigDecimal refundAmount, String currency, String reason) {
        // Mock refund processing for Nagad
        String refundId = "nagad_ref_" + UUID.randomUUID().toString().substring(0, 8);
        return RefundResponse.success(refundId, refundAmount, currency);
    }
}