package com.hopngo.market.service.payment;

import com.hopngo.market.entity.Order;
import com.hopngo.market.dto.PaymentIntentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock payment provider implementation for testing and development.
 * Simulates payment processing without actual payment gateway integration.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MockPaymentProvider.class);
    
    @Override
    public PaymentIntentResponse createPaymentIntent(Order order) {
        logger.info("Creating mock payment intent for order: {}", order.getId());
        
        // Generate mock payment intent ID
        String paymentIntentId = "pi_mock_" + UUID.randomUUID().toString().substring(0, 8);
        String clientSecret = paymentIntentId + "_secret_" + UUID.randomUUID().toString().substring(0, 8);
        
        PaymentIntentResponse response = new PaymentIntentResponse(
            clientSecret,
            paymentIntentId,
            order.getTotalAmount().longValue() * 100, // Convert to cents
            order.getCurrency(),
            "requires_payment_method",
            "MOCK"
        );
        
        logger.info("Mock payment intent created: {}", paymentIntentId);
        return response;
    }
    
    @Override
    public boolean verifyWebhook(HttpServletRequest request) {
        logger.info("Verifying mock webhook request");
        
        // For mock provider, always return true as there's no real signature to verify
        // In a real implementation, this would verify webhook signatures
        String mockSignature = request.getHeader("Mock-Signature");
        
        if (mockSignature == null) {
            logger.warn("Mock webhook missing signature header");
            return false;
        }
        
        // Simple mock verification - just check if signature starts with "mock_"
        boolean isValid = mockSignature.startsWith("mock_");
        
        if (isValid) {
            logger.info("Mock webhook signature verified successfully");
        } else {
            logger.warn("Mock webhook signature verification failed");
        }
        
        return isValid;
    }
    
    @Override
    public boolean verifyWebhookSignature(String rawBody, HttpHeaders headers) {
        logger.info("Verifying mock webhook signature with raw body and headers");
        
        // For mock provider, check for X-Mock-Signature header
        String mockSignature = headers.getFirst("X-Mock-Signature");
        
        if (mockSignature == null) {
            logger.warn("Mock webhook missing X-Mock-Signature header");
            return false;
        }
        
        // Simple mock verification - just check if signature is "valid_signature"
        boolean isValid = "valid_signature".equals(mockSignature);
        
        if (isValid) {
            logger.info("Mock webhook signature verified successfully");
        } else {
            logger.warn("Mock webhook signature verification failed");
        }
        
        return isValid;
    }
    
    @Override
    public String name() {
        return "MOCK";
    }
}