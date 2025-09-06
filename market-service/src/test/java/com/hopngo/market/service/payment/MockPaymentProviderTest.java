package com.hopngo.market.service.payment;

import com.hopngo.market.dto.PaymentIntentRequest;
import com.hopngo.market.dto.PaymentIntentResponse;
import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MockPaymentProvider.
 */
class MockPaymentProviderTest {

    private MockPaymentProvider mockPaymentProvider;
    private PaymentIntentRequest testRequest;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        mockPaymentProvider = new MockPaymentProvider();
        
        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        
        testRequest = new PaymentIntentRequest();
        testRequest.setOrderId(testOrder.getId());
        testRequest.setAmount(new BigDecimal("100.00"));
        testRequest.setCurrency("BDT");
        testRequest.setProvider("MOCK");
    }

    @Test
    void testName() {
        // Act
        String name = mockPaymentProvider.name();
        
        // Assert
        assertEquals("MOCK", name);
    }

    @Test
    void testCreatePaymentIntent_Success() {
        // Act
        PaymentIntentResponse response = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getPaymentIntentId());
        assertNotNull(response.getClientSecret());
        assertEquals("requires_payment_method", response.getStatus());
        assertEquals("BDT", response.getCurrency());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        
        // Verify ID format
        assertTrue(response.getPaymentIntentId().startsWith("pi_mock_"));
        assertTrue(response.getClientSecret().startsWith("pi_mock_"));
        assertTrue(response.getClientSecret().endsWith("_secret_test"));
    }

    @Test
    void testCreatePaymentIntent_DifferentAmounts() {
        // Test with different amounts
        testRequest.setAmount(new BigDecimal("250.50"));
        testOrder.setTotalAmount(new BigDecimal("250.50"));
        
        // Act
        PaymentIntentResponse response = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert
        assertEquals(new BigDecimal("250.50"), response.getAmount());
    }

    @Test
    void testCreatePaymentIntent_DifferentCurrency() {
        // Test with different currency
        testRequest.setCurrency("USD");
        
        // Act
        PaymentIntentResponse response = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void testCreatePaymentIntent_UniqueIds() {
        // Act - Create multiple payment intents
        PaymentIntentResponse response1 = mockPaymentProvider.createPaymentIntent(testOrder);
        PaymentIntentResponse response2 = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert - Each should have unique IDs
        assertNotEquals(response1.getPaymentIntentId(), response2.getPaymentIntentId());
        assertNotEquals(response1.getClientSecret(), response2.getClientSecret());
    }

    @Test
    void testVerifyWebhookSignature_ValidSignature_ReturnsTrue() {
        String rawBody = "{\"event\": \"payment_succeeded\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Mock-Signature", "valid_signature");
        
        boolean result = mockPaymentProvider.verifyWebhookSignature(rawBody, headers);
        
        assertTrue(result);
    }

    @Test
    void testVerifyWebhookSignature_InvalidSignature_ReturnsFalse() {
        String rawBody = "{\"event\": \"payment_failed\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Mock-Signature", "invalid_signature");
        
        boolean result = mockPaymentProvider.verifyWebhookSignature(rawBody, headers);
        
        assertFalse(result);
    }

    @Test
    void testVerifyWebhookSignature_MissingSignature_ReturnsFalse() {
        String rawBody = "{\"event\": \"payment_pending\"}";
        HttpHeaders headers = new HttpHeaders();
        
        boolean result = mockPaymentProvider.verifyWebhookSignature(rawBody, headers);
        
        assertFalse(result);
    }

    @Test
    void testVerifyWebhook_EmptyPayload() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("".getBytes());
        request.addHeader("Mock-Signature", "mock_valid_signature");
        
        // Act
        boolean isValid = mockPaymentProvider.verifyWebhook(request);
        
        // Assert
        assertTrue(isValid); // Mock provider only checks signature header
    }

    @Test
    void testVerifyWebhook_NullRequest() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            mockPaymentProvider.verifyWebhook(null);
        });
    }

    @Test
    void testVerifyWebhook_NoHeaders() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{\"id\":\"mock_evt_123\",\"type\":\"payment.succeeded\"}".getBytes());
        // No headers added
        
        // Act
        boolean isValid = mockPaymentProvider.verifyWebhook(request);
        
        // Assert
        assertFalse(isValid);
    }

    @Test
    void testCreatePaymentIntent_NullRequest() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            mockPaymentProvider.createPaymentIntent(null);
        });
    }

    @Test
    void testCreatePaymentIntent_NullOrder() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            mockPaymentProvider.createPaymentIntent(null);
        });
    }

    @Test
    void testCreatePaymentIntent_ZeroAmount() {
        // Arrange
        testRequest.setAmount(BigDecimal.ZERO);
        testOrder.setTotalAmount(BigDecimal.ZERO);
        
        // Act
        PaymentIntentResponse response = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getAmount());
    }

    @Test
    void testCreatePaymentIntent_LargeAmount() {
        // Arrange
        BigDecimal largeAmount = new BigDecimal("999999.99");
        testRequest.setAmount(largeAmount);
        testOrder.setTotalAmount(largeAmount);
        
        // Act
        PaymentIntentResponse response = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert
        assertNotNull(response);
        assertEquals(largeAmount, response.getAmount());
    }

    @Test
    void testCreatePaymentIntent_SpecialCharactersInCurrency() {
        // Arrange
        testRequest.setCurrency("€UR"); // Currency with special character
        
        // Act
        PaymentIntentResponse response = mockPaymentProvider.createPaymentIntent(testOrder);
        
        // Assert
        assertEquals("€UR", response.getCurrency());
    }
}