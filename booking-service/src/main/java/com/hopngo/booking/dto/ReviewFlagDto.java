package com.hopngo.booking.dto;

import com.hopngo.booking.entity.ReviewFlagStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewFlagDto(
    UUID id,
    UUID reviewId,
    String reporterUserId,
    String reason,
    ReviewFlagStatus status,
    String decisionNote,
    String resolvedByUserId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}