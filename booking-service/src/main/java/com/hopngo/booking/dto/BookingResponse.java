package com.hopngo.booking.dto;

import com.hopngo.booking.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
    UUID id,
    UUID listingId,
    String listingTitle,
    String userId,
    String userName,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    Integer numberOfGuests,
    BigDecimal totalPrice,
    String specialRequests,
    BookingStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}