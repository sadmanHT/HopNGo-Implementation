package com.hopngo.booking.dto;

import jakarta.validation.constraints.Size;

public record CancellationRequest(
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    String reason
) {}