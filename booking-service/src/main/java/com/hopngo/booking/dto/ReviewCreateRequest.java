package com.hopngo.booking.dto;

import jakarta.validation.constraints.*;

public record ReviewCreateRequest(
    @NotNull(message = "Booking ID is required")
    Long bookingId,
    
    @NotNull(message = "User ID is required")
    Long userId,
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    Integer rating,
    
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    String comment
) {}