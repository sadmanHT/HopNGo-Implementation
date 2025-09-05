package com.hopngo.booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingCreateRequest(
    @NotNull(message = "Listing ID is required")
    UUID listingId,
    
    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    LocalDate checkInDate,
    
    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    LocalDate checkOutDate,
    
    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "Number of guests must be at least 1")
    Integer numberOfGuests,
    
    @Size(max = 1000, message = "Special requests must not exceed 1000 characters")
    String specialRequests
) {
    public BookingCreateRequest {
        if (checkInDate != null && checkOutDate != null && !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
    }
}