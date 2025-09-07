package com.hopngo.market.service;

import com.hopngo.market.entity.*;
import com.hopngo.market.repository.WebhookEventRepository;
import com.hopngo.market.service.payment.MockPaymentProvider;
import com.hopngo.market.entity.PaymentProvider;
import com.hopngo.market.service.payment.StripePaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookService focusing on idempotency and signature verification.
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private EventPublishingService eventPublishingService;

    @Mock
    private MockPaymentProvider mockPaymentProvider;

    @Mock
    private StripePaymentProvider stripePaymentProvider;

    @Mock
    private com.hopngo.market.repository.PaymentRepository paymentRepository;

    @InjectMocks
    private WebhookService webhookService;

    private List<com.hopngo.market.service.payment.PaymentProvider> paymentProviders;

    private String testPayload;
    private HttpHeaders testHeaders;
    private Payment testPayment;
    private Order testOrder;
    
    @Mock
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Setup payment providers list
        paymentProviders = Arrays.asList(mockPaymentProvider, stripePaymentProvider);
        ReflectionTestUtils.setField(webhookService, "paymentProviders", paymentProviders);
        
        // Setup test data
        testPayload = "{\"id\":\"evt_test_123\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test_123\",\"status\":\"succeeded\"}}}";
        
        testHeaders = new HttpHeaders();
        testHeaders.set("Stripe-Signature", "t=1234567890,v1=test_signature");
        testHeaders.set("Content-Type", "application/json");
        
        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        
        testPayment = new Payment();
        testPayment.setId(UUID.randomUUID());
        testPayment.setPaymentIntentId("pi_test_123");
        testPayment.setOrder(testOrder);
        testPayment.setAmount(new BigDecimal("100.00"));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setProvider(PaymentProvider.STRIPE);
        
        // Setup provider names
        when(mockPaymentProvider.name()).thenReturn("MOCK");
        when(stripePaymentProvider.name()).thenReturn("STRIPE");
    }

    @Test
    void testProcessWebhook_NewEvent_Success() {
        // Arrange
        when(webhookEventRepository.findByWebhookId("evt_test_123"))
            .thenReturn(Optional.empty());
        when(stripePaymentProvider.verifyWebhook(mockRequest)).thenReturn(true);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(testPayment));
        when(paymentService.getProviderByName("STRIPE")).thenReturn(stripePaymentProvider);
        
        WebhookEvent savedEvent = new WebhookEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setWebhookId("evt_test_123");
        savedEvent.setStatus(WebhookEventStatus.RECEIVED);
        
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // Act
        boolean result = webhookService.processWebhook("STRIPE", testPayload, 
            testHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertTrue(result);
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
        verify(eventPublishingService).publishPaymentSucceededEvent(testPayment);
        verify(paymentService).updatePaymentStatus(testPayment, PaymentStatus.SUCCEEDED);
    }

    @Test
    void testProcessWebhook_DuplicateEvent_ReturnsIdempotent() {
        // Arrange
        WebhookEvent existingEvent = new WebhookEvent();
        existingEvent.setId(UUID.randomUUID());
        existingEvent.setWebhookId("evt_test_123");
        existingEvent.setProvider("STRIPE");
        existingEvent.setStatus(WebhookEventStatus.PROCESSED);
        existingEvent.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        
        when(webhookEventRepository.findByWebhookId("evt_test_123"))
            .thenReturn(Optional.of(existingEvent));

        // Act
        boolean result = webhookService.processWebhook("STRIPE", testPayload, 
            testHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertTrue(result);
        verify(webhookEventRepository, never()).save(any());
        verify(eventPublishingService, never()).publishPaymentSucceededEvent(any());
        verify(paymentService, never()).updatePaymentStatus(any(), any());
    }

    @Test
    void testProcessWebhook_InvalidSignature_ThrowsException() {
        // Arrange
        when(webhookEventRepository.findByWebhookId("evt_test_123"))
            .thenReturn(Optional.empty());
        when(stripePaymentProvider.verifyWebhook(mockRequest)).thenReturn(false);
        when(paymentService.getProviderByName("STRIPE")).thenReturn(stripePaymentProvider);

        // Act & Assert
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> webhookService.processWebhook("STRIPE", testPayload, 
                testHeaders.toSingleValueMap(), mockRequest)
        );
        
        assertEquals("Invalid webhook signature", exception.getMessage());
        verify(webhookEventRepository, never()).save(any());
        verify(eventPublishingService, never()).publishPaymentSucceededEvent(any());
    }

    @Test
    void testProcessWebhook_PaymentNotFound_HandlesGracefully() {
        // Arrange
        String failedPayload = "{\"id\":\"evt_test_123\",\"type\":\"payment_intent.payment_failed\",\"data\":{\"object\":{\"id\":\"pi_test_123\",\"status\":\"failed\"}}}";
        
        when(webhookEventRepository.findByWebhookId("evt_test_123"))
            .thenReturn(Optional.empty());
        when(stripePaymentProvider.verifyWebhook(mockRequest)).thenReturn(true);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.empty());
        
        WebhookEvent savedEvent = new WebhookEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setStatus(WebhookEventStatus.PROCESSING);
        savedEvent.setEventType("payment_intent.payment_failed");
        
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // Act
        boolean result = webhookService.processWebhook("STRIPE", failedPayload, 
            testHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertFalse(result); // The webhook processing should fail when payment is not found
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
        verify(paymentRepository).findByPaymentIntentId("pi_test_123");
    }

    @Test
    void testProcessWebhook_PaymentFailed_Success() {
        // Arrange
        String failedPayload = "{\"id\":\"evt_test_456\",\"type\":\"payment_intent.payment_failed\",\"data\":{\"object\":{\"id\":\"pi_test_123\",\"status\":\"failed\"}}}";
        
        when(webhookEventRepository.findByWebhookIdAndProvider("evt_test_456", "STRIPE"))
            .thenReturn(Optional.empty());
        when(stripePaymentProvider.verifyWebhook(mockRequest)).thenReturn(true);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(testPayment));
        when(paymentService.getProviderByName("STRIPE")).thenReturn(stripePaymentProvider);
        
        WebhookEvent savedEvent = new WebhookEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setStatus(WebhookEventStatus.PROCESSED);
        
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // Act
        boolean result = webhookService.processWebhook("STRIPE", failedPayload, testHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertTrue(result);
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
        verify(eventPublishingService).publishPaymentFailedEvent(testPayment);
        verify(paymentService).updatePaymentStatus(testPayment, PaymentStatus.FAILED);
    }

    @Test
    void testProcessWebhook_PaymentCanceled_Success() {
        // Arrange
        String canceledPayload = "{\"id\":\"evt_test_789\",\"type\":\"payment_intent.canceled\",\"data\":{\"object\":{\"id\":\"pi_test_123\",\"status\":\"canceled\"}}}";
        
        when(webhookEventRepository.findByWebhookId("evt_test_789"))
            .thenReturn(Optional.empty());
        when(stripePaymentProvider.verifyWebhook(mockRequest)).thenReturn(true);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(testPayment));
        when(paymentService.getProviderByName("STRIPE")).thenReturn(stripePaymentProvider);
        
        WebhookEvent savedEvent = new WebhookEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setStatus(WebhookEventStatus.PROCESSED);
        
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // Act
        boolean result = webhookService.processWebhook("STRIPE", canceledPayload, testHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertTrue(result);
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
        verify(eventPublishingService).publishPaymentCanceledEvent(testPayment);
        verify(paymentService).updatePaymentStatus(testPayment, PaymentStatus.CANCELLED);
    }

    @Test
    void testProcessWebhook_UnsupportedEventType_HandlesGracefully() {
        // Arrange
        String unsupportedPayload = "{\"id\":\"evt_test_999\",\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_test_123\"}}}";
        
        when(webhookEventRepository.findByWebhookId("evt_test_999"))
            .thenReturn(Optional.empty());
        when(stripePaymentProvider.verifyWebhook(mockRequest)).thenReturn(true);
        
        WebhookEvent savedEvent = new WebhookEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setStatus(WebhookEventStatus.PROCESSED);
        
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // Act
        boolean result = webhookService.processWebhook("STRIPE", unsupportedPayload, 
            testHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertTrue(result);
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
        verify(eventPublishingService, never()).publishPaymentSucceededEvent(any());
        verify(eventPublishingService, never()).publishPaymentFailedEvent(any());
        verify(eventPublishingService, never()).publishPaymentCanceledEvent(any());
    }

    @Test
    void testProcessWebhook_MockProvider_Success() {
        // Arrange
        String mockPayload = "{\"id\":\"mock_evt_123\",\"type\":\"payment.succeeded\",\"payment_intent_id\":\"pi_test_123\"}";
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("X-Mock-Signature", "mock_signature");
        
        when(webhookEventRepository.findByWebhookId("mock_evt_123"))
            .thenReturn(Optional.empty());
        when(mockPaymentProvider.verifyWebhook(mockRequest)).thenReturn(true);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(testPayment));
        when(paymentService.getProviderByName("MOCK")).thenReturn(mockPaymentProvider);
        
        WebhookEvent savedEvent = new WebhookEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setStatus(WebhookEventStatus.PROCESSED);
        
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // Act
        boolean result = webhookService.processWebhook("MOCK", mockPayload, 
            mockHeaders.toSingleValueMap(), mockRequest);

        // Assert
        assertTrue(result);
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
        verify(eventPublishingService).publishPaymentSucceededEvent(testPayment);
        verify(paymentService).updatePaymentStatus(testPayment, PaymentStatus.SUCCEEDED);
    }

    @Test
    void testGetWebhookStats_Success() {
        // Arrange
        when(webhookEventRepository.countByStatus(WebhookEventStatus.RECEIVED)).thenReturn(5L);
        when(webhookEventRepository.countByStatus(WebhookEventStatus.PROCESSING)).thenReturn(2L);
        when(webhookEventRepository.countByStatus(WebhookEventStatus.PROCESSED)).thenReturn(100L);
        when(webhookEventRepository.countByStatus(WebhookEventStatus.FAILED)).thenReturn(3L);
        when(webhookEventRepository.countByProvider("STRIPE")).thenReturn(80L);
        when(webhookEventRepository.countByProvider("MOCK")).thenReturn(30L);

        // Act
        Map<String, Object> stats = webhookService.getWebhookStats();

        // Assert
        assertNotNull(stats);
        assertEquals(5L, stats.get("received"));
        assertEquals(2L, stats.get("processing"));
        assertEquals(100L, stats.get("processed"));
        assertEquals(3L, stats.get("failed"));
        assertEquals(110L, stats.get("total"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> providerStats = (Map<String, Long>) stats.get("by_provider");
        assertEquals(80L, providerStats.get("STRIPE"));
        assertEquals(30L, providerStats.get("MOCK"));
    }

    @Test
    void testProcessWebhook_UnsupportedProvider_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> webhookService.processWebhook("UNSUPPORTED", testPayload, 
                testHeaders.toSingleValueMap(), mockRequest)
        );
        
        assertEquals("Payment provider not supported: UNSUPPORTED", exception.getMessage());
        verify(webhookEventRepository, never()).save(any());
    }
}