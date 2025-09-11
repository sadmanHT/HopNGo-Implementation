package com.hopngo.booking.dto;

import com.hopngo.booking.entity.ReviewFlagStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewFlagResolveRequest(
    @NotNull(message = "Status cannot be null")
    ReviewFlagStatus status,
    
    @Size(max = 1000, message = "Decision note cannot exceed 1000 characters")
    String decisionNote
) {}