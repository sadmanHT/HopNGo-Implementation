package com.hopngo.booking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VendorResponseDto(
    UUID id,
    UUID reviewId,
    String vendorUserId,
    String message,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}