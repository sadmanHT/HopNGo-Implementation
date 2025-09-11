package com.hopngo.booking.service;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.repository.BookingRepository;
import com.hopngo.booking.dto.CancellationRequest;
import com.hopngo.booking.dto.CancellationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCancellationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingCancellationService bookingCancellationService;

    private Booking booking;
    private CancellationRequest cancellationRequest;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setUserId("user123");
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDateTime.now().plusDays(7));
        booking.setCheckOutDate(LocalDateTime.now().plusDays(10));
        booking.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        // Set cancellation policies
        Map<String, Object> policies = Map.of(
            "free_until_hours", 48,
            "partial_pct", 50,
            "cutoff_hours", 24
        );
        booking.setCancellationPolicies(policies);

        cancellationRequest = new CancellationRequest();
        cancellationRequest.setReason("Change of plans");
    }

    @Test
    void testCalculateRefundAmount_WithinFreeWindow() {
        // Booking created 1 day ago, check-in in 7 days (6 days = 144 hours until check-in)
        // Free cancellation within 48 hours of check-in, so 144 > 48 = full refund
        
        BigDecimal refundAmount = bookingCancellationService.calculateRefundAmount(booking);
        
        assertEquals(new BigDecimal("100.00"), refundAmount);
    }

    @Test
    void testCalculateRefundAmount_WithinPartialWindow() {
        // Set check-in to 36 hours from now (within partial window)
        booking.setCheckInDate(LocalDateTime.now().plusHours(36));
        
        BigDecimal refundAmount = bookingCancellationService.calculateRefundAmount(booking);
        
        // Should get 50% refund
        assertEquals(new BigDecimal("50.00"), refundAmount);
    }

    @Test
    void testCalculateRefundAmount_PastCutoff() {
        // Set check-in to 12 hours from now (past cutoff)
        booking.setCheckInDate(LocalDateTime.now().plusHours(12));
        
        BigDecimal refundAmount = bookingCancellationService.calculateRefundAmount(booking);
        
        // Should get no refund
        assertEquals(BigDecimal.ZERO, refundAmount);
    }

    @Test
    void testCancelBooking_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        CancellationResponse response = bookingCancellationService.cancelBooking(1L, "user123", cancellationRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("100.00"), response.getRefundAmount());
        assertEquals("PENDING", response.getRefundStatus());
        assertNotNull(response.getBookingReference());
        
        verify(bookingRepository).save(argThat(b -> 
            b.getStatus() == BookingStatus.CANCELLED &&
            b.getCancellationReason().equals("Change of plans")
        ));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void testCancelBooking_BookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            bookingCancellationService.cancelBooking(1L, "user123", cancellationRequest)
        );
    }

    @Test
    void testCancelBooking_UnauthorizedUser() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class, () -> 
            bookingCancellationService.cancelBooking(1L, "otheruser", cancellationRequest)
        );
    }

    @Test
    void testCancelBooking_AlreadyCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class, () -> 
            bookingCancellationService.cancelBooking(1L, "user123", cancellationRequest)
        );
    }

    @Test
    void testCanCancelBooking_ValidBooking() {
        assertTrue(bookingCancellationService.canCancelBooking(booking));
    }

    @Test
    void testCanCancelBooking_AlreadyCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);
        assertFalse(bookingCancellationService.canCancelBooking(booking));
    }

    @Test
    void testCanCancelBooking_AlreadyCompleted() {
        booking.setStatus(BookingStatus.COMPLETED);
        assertFalse(bookingCancellationService.canCancelBooking(booking));
    }

    @Test
    void testCanCancelBooking_CheckInPassed() {
        booking.setCheckInDate(LocalDateTime.now().minusDays(1));
        assertFalse(bookingCancellationService.canCancelBooking(booking));
    }
}