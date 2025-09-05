package com.hopngo.booking.mapper;

import com.hopngo.booking.dto.ReviewCreateRequest;
import com.hopngo.booking.dto.ReviewResponse;
import com.hopngo.booking.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReviewMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Review toEntity(ReviewCreateRequest request);
    
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "booking.listing.id", target = "listingId")
    @Mapping(source = "booking.listing.title", target = "listingTitle")
    @Mapping(target = "userName", expression = "java(getUserName(review.getUserId()))")
    ReviewResponse toResponse(Review review);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ReviewCreateRequest request, @MappingTarget Review review);
    
    default String getUserName(String userId) {
        // This would typically call a user service to get the user name
        // For now, return a placeholder
        return "User " + userId;
    }
}