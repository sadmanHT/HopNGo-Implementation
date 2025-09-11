package com.hopngo.booking.integration;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.repository.BookingRepository;
import com.hopngo.booking.service.BookingCancellationService;
import com.hopngo.booking.dto.CancellationRequest;
import com.hopngo.booking.event.RefundSucceededEvent;
import com.hopngo.booking.event.RefundFailedEvent;
import com.hopngo.booking.listener.RefundEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
        booking.setListingId(1L);
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDateTime.now().plusDays(7));
        booking.setCheckOutDate(LocalDateTime.now().plusDays(10));
        booking.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        // Set cancellation policies for full refund
        Map<String, Object> policies = Map.of(
            "free_until_hours", 48,
            "partial_pct", 50,
            "cutoff_hours", 24
        );
        booking.setCancellationPolicies(policies);
        
        booking = bookingRepository.save(booking);

        cancellationRequest = new CancellationRequest();
        cancellationRequest.setReason("Integration test cancellation");
    }

    @Test
    void testRefundSagaSuccess_FullFlow() {
        // Step 1: Cancel booking (this should trigger RefundRequestedEvent)
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            "user123", 
            cancellationRequest
        );

        // Verify cancellation response
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("100.00"), response.getRefundAmount());
        assertEquals("PENDING", response.getRefundStatus());

        // Verify booking status changed
        Optional<Booking> cancelledBooking = bookingRepository.findById(booking.getId());
        assertTrue(cancelledBooking.isPresent());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.get().getStatus());
        assertEquals("Integration test cancellation", cancelledBooking.get().getCancellationReason());

        // Step 2: Simulate successful refund event from market-service
        RefundSucceededEvent successEvent = new RefundSucceededEvent(
            booking.getId(),
            1L, // paymentId
            new BigDecimal("100.00"),
            "ref_success123",
            "Refund processed successfully"
        );

        // Publish the success event
        eventPublisher.publishEvent(successEvent);

        // Wait for async processing and verify booking metadata updated
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Booking> updatedBooking = bookingRepository.findById(booking.getId());
            assertTrue(updatedBooking.isPresent());
            
            Map<String, Object> metadata = updatedBooking.get().getMetadata();
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
            "user123", 
            cancellationRequest
        );

        assertTrue(response.isSuccess());
        assertEquals(BookingStatus.CANCELLED, 
            bookingRepository.findById(booking.getId()).get().getStatus());

        // Step 2: Simulate failed refund event from market-service
        RefundFailedEvent failedEvent = new RefundFailedEvent(
            booking.getId(),
            1L, // paymentId
            new BigDecimal("100.00"),
            "INSUFFICIENT_FUNDS",
            "Insufficient funds for refund"
        );

        // Publish the failure event
        eventPublisher.publishEvent(failedEvent);

        // Wait for async processing and verify compensation actions
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Booking> compensatedBooking = bookingRepository.findById(booking.getId());
            assertTrue(compensatedBooking.isPresent());
            
            // Booking should be restored to CONFIRMED status for compensation
            assertEquals(BookingStatus.CONFIRMED, compensatedBooking.get().getStatus());
            
            Map<String, Object> metadata = compensatedBooking.get().getMetadata();
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
        booking.setCheckInDate(LocalDateTime.now().plusHours(36));
        booking = bookingRepository.save(booking);

        // Cancel booking
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            "user123", 
            cancellationRequest
        );

        // Should get 50% refund based on policy
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("50.00"), response.getRefundAmount());

        // Simulate successful partial refund
        RefundSucceededEvent successEvent = new RefundSucceededEvent(
            booking.getId(),
            1L,
            new BigDecimal("50.00"),
            "ref_partial123",
            "Partial refund processed"
        );

        eventPublisher.publishEvent(successEvent);

        // Verify partial refund metadata
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Booking> updatedBooking = bookingRepository.findById(booking.getId());
            assertTrue(updatedBooking.isPresent());
            
            Map<String, Object> metadata = updatedBooking.get().getMetadata();
            assertEquals("50.00", metadata.get("refund_amount").toString());
            assertEquals("PARTIAL", metadata.get("refund_type"));
        });
    }

    @Test
    void testNoRefundScenario() {
        // Set check-in to 12 hours from now (past cutoff)
        booking.setCheckInDate(LocalDateTime.now().plusHours(12));
        booking = bookingRepository.save(booking);

        // Cancel booking
        var response = bookingCancellationService.cancelBooking(
            booking.getId(), 
            "user123", 
            cancellationRequest
        );

        // Should get no refund
        assertTrue(response.isSuccess());
        assertEquals(BigDecimal.ZERO, response.getRefundAmount());
        assertEquals("NO_REFUND", response.getRefundStatus());

        // Verify booking is cancelled but no refund events are triggered
        Optional<Booking> cancelledBooking = bookingRepository.findById(booking.getId());
        assertTrue(cancelledBooking.isPresent());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.get().getStatus());
        
        Map<String, Object> metadata = cancelledBooking.get().getMetadata();
        if (metadata != null) {
            assertEquals("NO_REFUND", metadata.get("refund_status"));
        }
    }
}