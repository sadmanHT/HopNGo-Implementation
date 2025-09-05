package com.hopngo.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingUpdateRequest(
    @NotBlank(message = "Status is required")
    String status
) {}