package com.hopngo.market.service;

// Removed unused PaymentProviderConfiguration import
import com.hopngo.market.dto.PaymentIntentRequest;
import com.hopngo.market.dto.PaymentIntentResponse;
import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.OrderStatus;
import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.PaymentStatus;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.service.payment.MockPaymentProvider;
import com.hopngo.market.service.payment.PaymentProvider;
import com.hopngo.market.service.payment.StripePaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService focusing on provider selection and payment processing.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private MockPaymentProvider mockPaymentProvider;

    @Mock
    private StripePaymentProvider stripePaymentProvider;

    // Remove non-existent paymentProperties mock

    @InjectMocks
    private PaymentService paymentService;

    private List<PaymentProvider> paymentProviders;
    private Order testOrder;
    private PaymentIntentRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup payment providers list
        paymentProviders = Arrays.asList(mockPaymentProvider, stripePaymentProvider);
        ReflectionTestUtils.setField(paymentService, "paymentProviders", paymentProviders);
        ReflectionTestUtils.setField(paymentService, "defaultProviderName", "MOCK");

        // Setup test data
        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(OrderStatus.PENDING);

        testRequest = new PaymentIntentRequest();
        testRequest.setOrderId(testOrder.getId());
        testRequest.setAmount(new BigDecimal("100.00"));
        testRequest.setCurrency("BDT");
        testRequest.setProvider("MOCK");

        // Setup mock provider names
        when(mockPaymentProvider.name()).thenReturn("MOCK");
        when(stripePaymentProvider.name()).thenReturn("STRIPE");
    }

    @Test
    void testCreatePaymentIntent_WithSpecificProvider_Success() {
        // Arrange
        when(orderService.getOrderById(testOrder.getId())).thenReturn(testOrder);
        
        PaymentIntentResponse expectedResponse = new PaymentIntentResponse();
        expectedResponse.setClientSecret("pi_test_client_secret");
        expectedResponse.setPaymentIntentId("pi_test_123");
        expectedResponse.setStatus("requires_payment_method");
        
        when(mockPaymentProvider.createPaymentIntent(any(Order.class))).thenReturn(expectedResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        // Act
        Payment response = paymentService.createPaymentIntent(testOrder);

        // Assert
        assertNotNull(response);
        assertEquals("pi_test_client_secret", response.getClientSecret());
        assertEquals("pi_test_123", response.getPaymentIntentId());
        assertEquals("requires_payment_method", response.getStatus());
        
        verify(mockPaymentProvider).createPaymentIntent(eq(testOrder));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testCreatePaymentIntent_WithStripeProvider_Success() {
        // Arrange
        testRequest.setProvider("STRIPE");
        when(orderService.getOrderById(testOrder.getId())).thenReturn(testOrder);
        
        PaymentIntentResponse expectedResponse = new PaymentIntentResponse();
        expectedResponse.setClientSecret("pi_stripe_client_secret");
        expectedResponse.setPaymentIntentId("pi_stripe_123");
        expectedResponse.setStatus("requires_payment_method");
        
        when(stripePaymentProvider.createPaymentIntent(any(Order.class))).thenReturn(expectedResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        // Act
        Payment response = paymentService.createPaymentIntent(testOrder);

        // Assert
        assertNotNull(response);
        assertEquals("pi_stripe_client_secret", response.getClientSecret());
        assertEquals("pi_stripe_123", response.getPaymentIntentId());
        
        verify(stripePaymentProvider).createPaymentIntent(eq(testOrder));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testCreatePaymentIntent_WithUnsupportedProvider_ThrowsException() {
        // Arrange
        testRequest.setProvider("UNSUPPORTED");
        when(orderService.getOrderById(testOrder.getId())).thenReturn(testOrder);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createPaymentIntent(testOrder)
        );
        
        assertEquals("Payment provider not supported: UNSUPPORTED", exception.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testCreatePaymentIntent_WithDefaultProvider_Success() {
        // Arrange
        testRequest.setProvider(null); // No provider specified
        
        // Use default provider name directly
        when(orderService.getOrderById(testOrder.getId())).thenReturn(testOrder);
        
        PaymentIntentResponse expectedResponse = new PaymentIntentResponse();
        expectedResponse.setClientSecret("pi_default_client_secret");
        expectedResponse.setPaymentIntentId("pi_default_123");
        
        when(mockPaymentProvider.createPaymentIntent(any(Order.class))).thenReturn(expectedResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        // Act
        Payment response = paymentService.createPaymentIntent(testOrder);

        // Assert
        assertNotNull(response);
        assertEquals("pi_default_client_secret", response.getClientSecret());
        verify(mockPaymentProvider).createPaymentIntent(eq(testOrder));
    }

    @Test
    void testCreatePaymentIntent_OrderNotFound_ThrowsException() {
        // Arrange
        when(orderService.getOrderById(testOrder.getId())).thenThrow(new IllegalArgumentException("Order not found: " + testOrder.getId()));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createPaymentIntent(testOrder)
        );
        
        assertEquals("Order not found: " + testOrder.getId(), exception.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testCreatePaymentIntent_OrderAlreadyPaid_ThrowsException() {
        // Arrange
        testOrder.setStatus(OrderStatus.PAID);
        when(orderService.getOrderById(testOrder.getId())).thenReturn(testOrder);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> paymentService.createPaymentIntent(testOrder)
        );
        
        assertEquals("Order is already paid: " + testOrder.getId(), exception.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testCreatePaymentIntent_AmountMismatch_ThrowsException() {
        // Arrange
        testRequest.setAmount(new BigDecimal("200.00")); // Different from order amount
        when(orderService.getOrderById(testOrder.getId())).thenReturn(testOrder);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createPaymentIntent(testOrder)
        );
        
        assertEquals("Payment amount does not match order total", exception.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testFindByPaymentIntentId_Success() {
        // Arrange
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPaymentIntentId("pi_test_123");
        payment.setStatus(PaymentStatus.PENDING);
        
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));

        // Act
        Optional<Payment> result = paymentService.findByPaymentIntentId("pi_test_123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("pi_test_123", result.get().getPaymentIntentId());
        assertEquals(PaymentStatus.PENDING, result.get().getStatus());
    }

    @Test
    void testFindByPaymentIntentId_NotFound_ReturnsNull() {
        // Arrange
        when(paymentRepository.findByPaymentIntentId("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<Payment> result = paymentService.findByPaymentIntentId("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdatePaymentStatus_Success() {
        // Arrange
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.PENDING);
        
        when(paymentRepository.save(payment)).thenReturn(payment);

        // Act
        Payment result = paymentService.updatePaymentStatus(payment, PaymentStatus.SUCCEEDED);

        // Assert
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertNotNull(result);
        verify(paymentRepository).save(payment);
    }

    @Test
    void testGetProviderByName_Success() {
        // Act
        PaymentProvider mockProvider = paymentService.getProviderByName("MOCK");
        PaymentProvider stripeProvider = paymentService.getProviderByName("STRIPE");

        // Assert
        assertNotNull(mockProvider);
        assertNotNull(stripeProvider);
        assertEquals(mockPaymentProvider, mockProvider);
        assertEquals(stripePaymentProvider, stripeProvider);
    }

    @Test
    void testGetProviderByName_NotFound_ReturnsNull() {
        // Act
        PaymentProvider result = paymentService.getProviderByName("NONEXISTENT");

        // Assert
        assertNull(result);
    }
}