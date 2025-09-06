package com.hopngo.booking.mapper;

import com.hopngo.booking.dto.VendorCreateRequest;
import com.hopngo.booking.dto.VendorResponse;
import com.hopngo.booking.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VendorMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "name", target = "businessName")
    @Mapping(source = "email", target = "contactEmail")
    @Mapping(source = "phone", target = "contactPhone")
    Vendor toEntity(VendorCreateRequest request);
    
    VendorResponse toResponse(Vendor vendor);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "name", target = "businessName")
    @Mapping(source = "email", target = "contactEmail")
    @Mapping(source = "phone", target = "contactPhone")
    void updateEntity(VendorCreateRequest request, @MappingTarget Vendor vendor);
}