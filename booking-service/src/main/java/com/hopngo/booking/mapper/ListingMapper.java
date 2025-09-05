package com.hopngo.booking.mapper;

import com.hopngo.booking.dto.ListingCreateRequest;
import com.hopngo.booking.dto.ListingResponse;
import com.hopngo.booking.entity.Listing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ListingMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Listing toEntity(ListingCreateRequest request);
    
    @Mapping(source = "vendor.id", target = "vendorId")
    @Mapping(source = "vendor.businessName", target = "vendorName")
    ListingResponse toResponse(Listing listing);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ListingCreateRequest request, @MappingTarget Listing listing);
}