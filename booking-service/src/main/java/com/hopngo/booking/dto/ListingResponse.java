package com.hopngo.booking.dto;

import com.hopngo.booking.entity.CancellationPolicies;
import com.hopngo.booking.entity.Listing.ListingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
    UUID id,
    UUID vendorId,
    String vendorName,
    String title,
    String description,
    BigDecimal basePrice,
    Integer maxGuests,
    BigDecimal latitude,
    BigDecimal longitude,
    String address,
    List<String> amenities,
    List<String> images,
    ListingStatus status,
    CancellationPolicies cancellationPolicies,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}