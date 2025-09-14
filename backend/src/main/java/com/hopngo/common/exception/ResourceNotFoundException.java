package com.hopngo.common.exception;

/**
 * Exception for when a requested resource is not found (404)
 * These are expected exceptions that don't need to be sent to Sentry
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final String errorCode;
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "RESOURCE_NOT_FOUND";
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
        this.errorCode = "RESOURCE_NOT_FOUND";
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String message, String errorCode, boolean isCustomError) {
        super(message);
        this.errorCode = errorCode;
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId, String errorCode) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
        this.errorCode = errorCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    // Common resource not found factory methods
    public static ResourceNotFoundException user(String userId) {
        return new ResourceNotFoundException("User", userId, "USER_NOT_FOUND");
    }
    
    public static ResourceNotFoundException booking(String bookingId) {
        return new ResourceNotFoundException("Booking", bookingId, "BOOKING_NOT_FOUND");
    }
    
    public static ResourceNotFoundException trip(String tripId) {
        return new ResourceNotFoundException("Trip", tripId, "TRIP_NOT_FOUND");
    }
    
    public static ResourceNotFoundException payment(String paymentId) {
        return new ResourceNotFoundException("Payment", paymentId, "PAYMENT_NOT_FOUND");
    }
    
    public static ResourceNotFoundException vehicle(String vehicleId) {
        return new ResourceNotFoundException("Vehicle", vehicleId, "VEHICLE_NOT_FOUND");
    }
    
    public static ResourceNotFoundException driver(String driverId) {
        return new ResourceNotFoundException("Driver", driverId, "DRIVER_NOT_FOUND");
    }
    
    public static ResourceNotFoundException route(String routeId) {
        return new ResourceNotFoundException("Route", routeId, "ROUTE_NOT_FOUND");
    }
}