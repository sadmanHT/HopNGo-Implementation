package com.hopngo.booking.mapper;

import com.hopngo.booking.dto.BookingCreateRequest;
import com.hopngo.booking.dto.BookingResponse;
import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "guests", source = "numberOfGuests")
    @Mapping(target = "startDate", source = "checkInDate")
    @Mapping(target = "endDate", source = "checkOutDate")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "specialRequests", source = "specialRequests")
    @Mapping(target = "bookingReference", ignore = true)
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "review", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancellationPolicies", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "metadataAsMap", ignore = true)
    @Mapping(target = "cancellationPoliciesAsMap", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Booking toEntity(BookingCreateRequest request);
    
    @Mapping(target = "listingId", expression = "java(booking.getListing() != null ? booking.getListing().getId() : null)")
    @Mapping(target = "listingTitle", expression = "java(booking.getListing() != null ? booking.getListing().getTitle() : null)")
    @Mapping(target = "userId", expression = "java(booking.getUserId())")
    @Mapping(target = "userName", expression = "java(getUserName(booking.getUserId()))")
    @Mapping(target = "checkInDate", source = "startDate")
    @Mapping(target = "checkOutDate", source = "endDate")
    @Mapping(target = "numberOfGuests", source = "guests")
    @Mapping(target = "totalPrice", source = "totalAmount")
    @Mapping(target = "specialRequests", expression = "java(booking.getSpecialRequests())")
    BookingResponse toResponse(Booking booking);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "guests", source = "numberOfGuests")
    @Mapping(target = "startDate", source = "checkInDate")
    @Mapping(target = "endDate", source = "checkOutDate")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "specialRequests", source = "specialRequests")
    @Mapping(target = "bookingReference", ignore = true)
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "review", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancellationPolicies", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "metadataAsMap", ignore = true)
    @Mapping(target = "cancellationPoliciesAsMap", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(BookingCreateRequest request, @MappingTarget Booking booking);
    
    default String getUserName(String userId) {
        // This would typically call a user service to get the user name
        // For now, return a placeholder
        return "User " + userId;
    }
}