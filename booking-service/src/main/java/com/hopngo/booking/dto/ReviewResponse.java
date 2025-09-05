package com.hopngo.booking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID bookingId,
    String userId,
    String userName,
    UUID listingId,
    String listingTitle,
    Integer rating,
    String comment,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}