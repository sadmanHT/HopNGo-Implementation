package com.hopngo.booking.mapper;

import com.hopngo.booking.dto.ReviewFlagDto;
import com.hopngo.booking.entity.ReviewFlag;
import org.springframework.stereotype.Component;

@Component
public class ReviewFlagMapper {
    
    public static ReviewFlagDto toResponse(ReviewFlag reviewFlag) {
        if (reviewFlag == null) {
            return null;
        }
        
        return new ReviewFlagDto(
            reviewFlag.getId(),
            reviewFlag.getReview().getId(),
            reviewFlag.getReporterUserId(),
            reviewFlag.getReason(),
            reviewFlag.getStatus(),
            reviewFlag.getDecisionNote(),
            reviewFlag.getResolvedByUserId(),
            reviewFlag.getCreatedAt(),
            reviewFlag.getUpdatedAt()
        );
    }
}