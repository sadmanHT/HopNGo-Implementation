package com.hopngo.booking.mapper;

import com.hopngo.booking.dto.VendorResponseDto;
import com.hopngo.booking.entity.VendorResponse;
import org.springframework.stereotype.Component;

@Component
public class VendorResponseMapper {
    
    public static VendorResponseDto toResponse(VendorResponse vendorResponse) {
        if (vendorResponse == null) {
            return null;
        }
        
        return new VendorResponseDto(
            vendorResponse.getId(),
            vendorResponse.getReview().getId(),
            vendorResponse.getVendorUserId(),
            vendorResponse.getMessage(),
            vendorResponse.getCreatedAt(),
            vendorResponse.getUpdatedAt()
        );
    }
}