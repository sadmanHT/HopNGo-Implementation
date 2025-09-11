package com.hopngo.market.service;

import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.PaymentStatus;
import com.hopngo.market.entity.Refund;
import com.hopngo.market.entity.RefundStatus;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.repository.RefundRepository;
import com.hopngo.market.payment.PaymentProvider;
import com.hopngo.market.payment.RefundResponse;
import com.hopngo.market.event.RefundRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProvider stripeProvider;

    @Mock
    private PaymentProvider bkashProvider;

    @Mock
    private PaymentProvider nagadProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RefundService refundService;

    private Payment payment;
    private Refund refund;
    private RefundRequestedEvent refundEvent;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId(1L);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProvider("stripe");
        payment.setProviderTransactionId("pi_test123");
        payment.setCreatedAt(LocalDateTime.now());

        refund = new Refund();
        refund.setId(1L);
        refund.setPayment(payment);
        refund.setAmount(new BigDecimal("50.00"));
        refund.setStatus(RefundStatus.PENDING);
        refund.setReason("Customer request");
        refund.setCreatedAt(LocalDateTime.now());

        refundEvent = new RefundRequestedEvent(
            1L, // bookingId
            1L, // paymentId
            new BigDecimal("50.00"), // refundAmount
            "Customer request", // reason
            "user123" // userId
        );
    }

    @Test
    void testProcessBookingRefund_Success() {
        when(paymentRepository.findByOrderBookingId(1L)).thenReturn(Optional.of(payment));
        when(refundRepository.existsByPaymentIdAndStatus(1L, RefundStatus.PENDING)).thenReturn(false);
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);

        refundService.processBookingRefund(refundEvent);

        verify(refundRepository).save(argThat(r -> 
            r.getPayment().equals(payment) &&
            r.getAmount().equals(new BigDecimal("50.00")) &&
            r.getReason().equals("Customer request") &&
            r.getStatus() == RefundStatus.PENDING
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testProcessBookingRefund_PaymentNotFound() {
        when(paymentRepository.findByOrderBookingId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            refundService.processBookingRefund(refundEvent)
        );
    }

    @Test
    void testProcessBookingRefund_RefundAlreadyExists() {
        when(paymentRepository.findByOrderBookingId(1L)).thenReturn(Optional.of(payment));
        when(refundRepository.existsByPaymentIdAndStatus(1L, RefundStatus.PENDING)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> 
            refundService.processBookingRefund(refundEvent)
        );
    }

    @Test
    void testProcessRefund_StripeSuccess() {
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(stripeProvider.refundPayment(eq("pi_test123"), eq(new BigDecimal("50.00")), any()))
            .thenReturn(RefundResponse.success("re_test123", "Refund successful"));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);

        refundService.processRefund(1L);

        verify(refundRepository).save(argThat(r -> 
            r.getStatus() == RefundStatus.COMPLETED &&
            r.getProviderRefundId().equals("re_test123")
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testProcessRefund_BkashSuccess() {
        payment.setProvider("bkash");
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(bkashProvider.refundPayment(eq("pi_test123"), eq(new BigDecimal("50.00")), any()))
            .thenReturn(RefundResponse.success("bkash_ref123", "Refund successful"));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);

        refundService.processRefund(1L);

        verify(refundRepository).save(argThat(r -> 
            r.getStatus() == RefundStatus.COMPLETED &&
            r.getProviderRefundId().equals("bkash_ref123")
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testProcessRefund_NagadSuccess() {
        payment.setProvider("nagad");
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(nagadProvider.refundPayment(eq("pi_test123"), eq(new BigDecimal("50.00")), any()))
            .thenReturn(RefundResponse.success("nagad_ref123", "Refund successful"));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);

        refundService.processRefund(1L);

        verify(refundRepository).save(argThat(r -> 
            r.getStatus() == RefundStatus.COMPLETED &&
            r.getProviderRefundId().equals("nagad_ref123")
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testProcessRefund_ProviderFailure() {
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(stripeProvider.refundPayment(eq("pi_test123"), eq(new BigDecimal("50.00")), any()))
            .thenReturn(RefundResponse.failure("INSUFFICIENT_FUNDS", "Insufficient funds for refund"));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);

        refundService.processRefund(1L);

        verify(refundRepository).save(argThat(r -> 
            r.getStatus() == RefundStatus.FAILED &&
            r.getFailureReason().equals("Insufficient funds for refund")
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testProcessRefund_RefundNotFound() {
        when(refundRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            refundService.processRefund(1L)
        );
    }

    @Test
    void testGetRefundStatus_Success() {
        when(refundRepository.findByPaymentId(1L)).thenReturn(Optional.of(refund));

        Optional<Refund> result = refundService.getRefundStatus(1L);

        assertTrue(result.isPresent());
        assertEquals(refund, result.get());
    }

    @Test
    void testGetUserRefunds_Success() {
        when(refundRepository.findByPayment_Order_UserId("user123")).thenReturn(java.util.List.of(refund));

        var refunds = refundService.getUserRefunds("user123");

        assertEquals(1, refunds.size());
        assertEquals(refund, refunds.get(0));
    }

    @Test
    void testRetryFailedRefund_Success() {
        refund.setStatus(RefundStatus.FAILED);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(stripeProvider.refundPayment(eq("pi_test123"), eq(new BigDecimal("50.00")), any()))
            .thenReturn(RefundResponse.success("re_retry123", "Refund successful on retry"));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);

        refundService.retryFailedRefund(1L);

        verify(refundRepository).save(argThat(r -> 
            r.getStatus() == RefundStatus.COMPLETED &&
            r.getProviderRefundId().equals("re_retry123")
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testRetryFailedRefund_NotFailedStatus() {
        refund.setStatus(RefundStatus.COMPLETED);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThrows(RuntimeException.class, () -> 
            refundService.retryFailedRefund(1L)
        );
    }
}