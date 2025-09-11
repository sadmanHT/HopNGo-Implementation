package com.hopngo.booking.dto;

import com.hopngo.booking.entity.CancellationPolicies;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListingCreateRequest(
    @NotNull(message = "Vendor ID is required")
    UUID vendorId,
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,
    
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    BigDecimal basePrice,
    
    @NotNull(message = "Max guests is required")
    @Min(value = 1, message = "Max guests must be at least 1")
    @Max(value = 50, message = "Max guests cannot exceed 50")
    Integer maxGuests,
    
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double latitude,
    
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double longitude,
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    String address,
    
    List<String> amenities,
    
    List<String> images,
    
    CancellationPolicies cancellationPolicies
) {}