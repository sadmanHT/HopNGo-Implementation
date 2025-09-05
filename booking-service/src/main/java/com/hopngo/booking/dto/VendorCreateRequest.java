package com.hopngo.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String phone,
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description
) {}