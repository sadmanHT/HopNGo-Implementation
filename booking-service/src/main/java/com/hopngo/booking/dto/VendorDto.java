package com.hopngo.booking.dto;

import com.hopngo.booking.entity.Vendor.VendorStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record VendorDto(
    UUID id,
    String businessName,
    String contactEmail,
    String contactPhone,
    String description,
    VendorStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}