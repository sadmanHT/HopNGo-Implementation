package com.hopngo.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryCreateRequest(
    @NotNull(message = "Listing ID is required")
    UUID listingId,
    
    @NotNull(message = "Date is required")
    LocalDate date,
    
    @NotNull(message = "Available quantity is required")
    @PositiveOrZero(message = "Available quantity must be zero or positive")
    Integer availableQuantity,
    
    BigDecimal priceOverride
) {}