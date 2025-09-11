package com.hopngo.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorResponseCreateRequest(
    @NotBlank(message = "Response message cannot be blank")
    @Size(max = 2000, message = "Response message cannot exceed 2000 characters")
    String message
) {}