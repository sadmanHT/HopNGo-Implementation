package com.hopngo.booking.integration;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.repository.BookingRepository;
import com.hopngo.booking.service.BookingCancellationService;
import com.hopngo.booking.dto.CancellationRequest;
import com.hopngo.booking.event.RefundSucceededEvent;
import com.hopngo.booking.event.RefundFailedEvent;
import com.hopngo.booking.event.RefundEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class RefundSagaIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingCancellationService bookingCancellationService;

    @Autowired
    private RefundEventListener refundEventListener;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Booking booking;
    private CancellationRequest cancellationRequest;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setUserId("user123");
        booking.setListingId(UUID.randomUUID());
        booking.setPaymentId(UUID.randomUUID());
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().plusDays(7));
        booking.setCheckOutDate(LocalDate.now().plusDays(10));
        booking.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        // Set cancellation policies for full refund
        booking.setCancellationPolicies("free_until_hours=48,partial_pct=50,cutoff_hours=24");
        
        booking = bookingRepository.save(booking);

        cancellationRequest = new CancellationRequest("Integration test cancellation");
    }

    @Test
    void testRefundSagaSuccess_FullFlow() {
        // Step 1: Cancel booking (this should trigger RefundRequestedEvent)
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            cancellationRequest
        );

        // Verify cancellation response
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals("Integration test cancellation", response.getCancellationReason());

        // Verify booking status changed
        Optional<Booking> cancelledBooking = bookingRepository.findById(booking.getId());
        assertTrue(cancelledBooking.isPresent());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.get().getStatus());
        assertEquals("Integration test cancellation", cancelledBooking.get().getCancellationReason());

        // Step 2: Simulate successful refund event from market-service
        RefundSucceededEvent successEvent = new RefundSucceededEvent(
            UUID.randomUUID(), // refundId
            booking.getId(),    // bookingId
            booking.getPaymentId(), // paymentId
            new BigDecimal("100.00"), // refundedAmount
            "USD",              // currency
            "ref_success123",   // providerRefundId
            "stripe",           // paymentProvider
            LocalDateTime.now() // processedAt
        );

        // Publish the success event
        eventPublisher.publishEvent(successEvent);

        // Wait for async processing and verify booking metadata updated
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Booking> updatedBooking = bookingRepository.findById(booking.getId());
            assertTrue(updatedBooking.isPresent());
            
            Map<String, Object> metadata = updatedBooking.get().getMetadataAsMap();
            assertNotNull(metadata);
            assertEquals("COMPLETED", metadata.get("refund_status"));
            assertEquals("ref_success123", metadata.get("refund_reference"));
            assertEquals("100.00", metadata.get("refund_amount").toString());
        });
    }

    @Test
    void testRefundSagaFailure_CompensationFlow() {
        // Step 1: Cancel booking
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            cancellationRequest
        );

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, 
            bookingRepository.findById(booking.getId()).get().getStatus());

        // Step 2: Simulate failed refund event from market-service
        RefundFailedEvent failedEvent = new RefundFailedEvent(
            UUID.randomUUID(), // refundId
            booking.getId(),    // bookingId
            booking.getPaymentId(), // paymentId
            new BigDecimal("100.00"), // attemptedAmount
            "USD",              // currency
            "stripe",           // paymentProvider
            "INSUFFICIENT_FUNDS", // failureReason
            LocalDateTime.now() // failedAt
        );

        // Publish the failure event
        eventPublisher.publishEvent(failedEvent);

        // Wait for async processing and verify compensation actions
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Booking> compensatedBooking = bookingRepository.findById(booking.getId());
            assertTrue(compensatedBooking.isPresent());
            
            // Booking should be restored to CONFIRMED status for compensation
            assertEquals(BookingStatus.CONFIRMED, compensatedBooking.get().getStatus());
            
            Map<String, Object> metadata = compensatedBooking.get().getMetadataAsMap();
            assertNotNull(metadata);
            assertEquals("FAILED", metadata.get("refund_status"));
            assertEquals("INSUFFICIENT_FUNDS", metadata.get("refund_error_code"));
            assertEquals("Insufficient funds for refund", metadata.get("refund_error_message"));
            assertTrue(metadata.containsKey("compensation_applied"));
        });
    }

    @Test
    void testPartialRefundScenario() {
        // Set check-in to 36 hours from now for partial refund
        booking.setCheckInDate(LocalDate.now().plusDays(1)); // 1 day from now
        booking = bookingRepository.save(booking);

        // Cancel booking
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            cancellationRequest
        );

        // Should get booking cancelled 
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());

        // Simulate successful partial refund
        RefundSucceededEvent successEvent = new RefundSucceededEvent(
            UUID.randomUUID(), // refundId
            booking.getId(),    // bookingId
            booking.getPaymentId(), // paymentId
            new BigDecimal("50.00"), // refundedAmount
            "USD",              // currency
            "ref_partial123",   // providerRefundId
            "stripe",           // paymentProvider
            LocalDateTime.now() // processedAt
        );

        eventPublisher.publishEvent(successEvent);

        // Verify partial refund metadata
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Booking> updatedBooking = bookingRepository.findById(booking.getId());
            assertTrue(updatedBooking.isPresent());
            
            Map<String, Object> metadata = updatedBooking.get().getMetadataAsMap();
            assertEquals("50.00", metadata.get("refund_amount").toString());
            assertEquals("PARTIAL", metadata.get("refund_type"));
        });
    }

    @Test
    void testNoRefundScenario() {
        // Set check-in to 12 hours from now (past cutoff)
        booking.setCheckInDate(LocalDate.now()); // today (short notice)
        booking = bookingRepository.save(booking);

        // Cancel booking
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            cancellationRequest
        );

        // Should get booking cancelled
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());

        // Verify booking is cancelled but no refund events are triggered
        Optional<Booking> cancelledBooking = bookingRepository.findById(booking.getId());
        assertTrue(cancelledBooking.isPresent());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.get().getStatus());
        
        Map<String, Object> metadata = cancelledBooking.get().getMetadataAsMap();
        if (metadata != null) {
            assertEquals("NO_REFUND", metadata.get("refund_status"));
        }
    }
}