package com.hopngo.booking.service;

import com.hopngo.booking.entity.*;
import com.hopngo.booking.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OutboxService {
    
    private final OutboxEventRepository outboxEventRepository;
    
    @Autowired
    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }
    
    // Vendor Events
    public void publishVendorCreatedEvent(Vendor vendor) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("vendorId", vendor.getId().toString());
        eventData.put("userId", vendor.getUserId());
        eventData.put("businessName", vendor.getBusinessName());
        eventData.put("contactEmail", vendor.getContactEmail());
        eventData.put("status", vendor.getStatus().toString());
        eventData.put("createdAt", vendor.getCreatedAt() != null ? vendor.getCreatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Vendor", 
            vendor.getId().toString(), 
            "vendor.created", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishVendorUpdatedEvent(Vendor vendor) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("vendorId", vendor.getId().toString());
        eventData.put("userId", vendor.getUserId());
        eventData.put("businessName", vendor.getBusinessName());
        eventData.put("contactEmail", vendor.getContactEmail());
        eventData.put("status", vendor.getStatus().toString());
        eventData.put("updatedAt", vendor.getUpdatedAt() != null ? vendor.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Vendor", 
            vendor.getId().toString(), 
            "vendor.updated", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishVendorSuspendedEvent(Vendor vendor, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("vendorId", vendor.getId().toString());
        eventData.put("userId", vendor.getUserId());
        eventData.put("businessName", vendor.getBusinessName());
        eventData.put("status", vendor.getStatus().toString());
        eventData.put("reason", reason);
        eventData.put("suspendedAt", vendor.getUpdatedAt() != null ? vendor.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Vendor", 
            vendor.getId().toString(), 
            "vendor.suspended", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishVendorActivatedEvent(Vendor vendor) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("vendorId", vendor.getId().toString());
        eventData.put("userId", vendor.getUserId());
        eventData.put("businessName", vendor.getBusinessName());
        eventData.put("status", vendor.getStatus().toString());
        eventData.put("activatedAt", vendor.getUpdatedAt() != null ? vendor.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Vendor", 
            vendor.getId().toString(), 
            "vendor.activated", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishVendorDeactivatedEvent(Vendor vendor) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("vendorId", vendor.getId().toString());
        eventData.put("userId", vendor.getUserId());
        eventData.put("businessName", vendor.getBusinessName());
        eventData.put("status", vendor.getStatus().toString());
        eventData.put("deactivatedAt", vendor.getUpdatedAt() != null ? vendor.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Vendor", 
            vendor.getId().toString(), 
            "vendor.deactivated", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    // Listing Events
    public void publishListingCreatedEvent(Listing listing) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("listingId", listing.getId().toString());
        eventData.put("vendorId", listing.getVendor().getId().toString());
        eventData.put("title", listing.getTitle());
        eventData.put("category", listing.getCategory());
        eventData.put("basePrice", listing.getBasePrice().toString());
        eventData.put("status", listing.getStatus().toString());
        eventData.put("createdAt", listing.getCreatedAt() != null ? listing.getCreatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Listing", 
            listing.getId().toString(), 
            "listing.created", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishListingUpdatedEvent(Listing listing) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("listingId", listing.getId().toString());
        eventData.put("vendorId", listing.getVendor().getId().toString());
        eventData.put("title", listing.getTitle());
        eventData.put("category", listing.getCategory());
        eventData.put("basePrice", listing.getBasePrice().toString());
        eventData.put("status", listing.getStatus().toString());
        eventData.put("updatedAt", listing.getUpdatedAt() != null ? listing.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Listing", 
            listing.getId().toString(), 
            "listing.updated", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    // Booking Events
    public void publishBookingCreatedEvent(Booking booking) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("bookingId", booking.getId().toString());
        eventData.put("userId", booking.getUserId());
        eventData.put("listingId", booking.getListing().getId().toString());
        eventData.put("vendorId", booking.getVendor().getId().toString());
        eventData.put("startDate", booking.getStartDate().toString());
        eventData.put("endDate", booking.getEndDate().toString());
        eventData.put("guests", booking.getGuests());
        eventData.put("totalAmount", booking.getTotalAmount().toString());
        eventData.put("status", booking.getStatus().toString());
        eventData.put("bookingReference", booking.getBookingReference());
        eventData.put("createdAt", booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Booking", 
            booking.getId().toString(), 
            "booking.created", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishBookingConfirmedEvent(Booking booking) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("bookingId", booking.getId().toString());
        eventData.put("userId", booking.getUserId());
        eventData.put("listingId", booking.getListing().getId().toString());
        eventData.put("vendorId", booking.getVendor().getId().toString());
        eventData.put("startDate", booking.getStartDate().toString());
        eventData.put("endDate", booking.getEndDate().toString());
        eventData.put("totalAmount", booking.getTotalAmount().toString());
        eventData.put("status", booking.getStatus().toString());
        eventData.put("bookingReference", booking.getBookingReference());
        eventData.put("confirmedAt", booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Booking", 
            booking.getId().toString(), 
            "booking.confirmed", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishBookingCancelledEvent(Booking booking) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("bookingId", booking.getId().toString());
        eventData.put("userId", booking.getUserId());
        eventData.put("listingId", booking.getListing().getId().toString());
        eventData.put("vendorId", booking.getVendor().getId().toString());
        eventData.put("startDate", booking.getStartDate().toString());
        eventData.put("endDate", booking.getEndDate().toString());
        eventData.put("totalAmount", booking.getTotalAmount().toString());
        eventData.put("status", booking.getStatus().toString());
        eventData.put("bookingReference", booking.getBookingReference());
        eventData.put("cancelledAt", booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Booking", 
            booking.getId().toString(), 
            "booking.cancelled", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    // Review Events
    public void publishReviewCreatedEvent(Review review) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("reviewId", review.getId().toString());
        eventData.put("bookingId", review.getBooking().getId().toString());
        eventData.put("userId", review.getUserId());
        eventData.put("listingId", review.getListing().getId().toString());
        eventData.put("vendorId", review.getVendor().getId().toString());
        eventData.put("rating", review.getRating());
        eventData.put("title", review.getTitle());
        eventData.put("comment", review.getComment());
        eventData.put("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Review", 
            review.getId().toString(), 
            "review.created", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishReviewUpdatedEvent(Review review) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("reviewId", review.getId().toString());
        eventData.put("bookingId", review.getBooking().getId().toString());
        eventData.put("userId", review.getUserId());
        eventData.put("listingId", review.getListing().getId().toString());
        eventData.put("vendorId", review.getVendor().getId().toString());
        eventData.put("rating", review.getRating());
        eventData.put("title", review.getTitle());
        eventData.put("comment", review.getComment());
        eventData.put("updatedAt", review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Review", 
            review.getId().toString(), 
            "review.updated", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    public void publishReviewDeletedEvent(Review review) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("reviewId", review.getId().toString());
        eventData.put("bookingId", review.getBooking().getId().toString());
        eventData.put("userId", review.getUserId());
        eventData.put("listingId", review.getListing().getId().toString());
        eventData.put("vendorId", review.getVendor().getId().toString());
        eventData.put("deletedAt", review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null);
        
        OutboxEvent event = new OutboxEvent(
            "Review", 
            review.getId().toString(), 
            "review.deleted", 
            eventData
        );
        
        outboxEventRepository.save(event);
    }
    
    // Utility methods
    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.findPendingEventsOrderByCreatedAt();
    }
    
    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEventsOlderThan(LocalDateTime maxCreatedAt) {
        return outboxEventRepository.findPendingEventsOlderThan(maxCreatedAt);
    }
    
    public void markEventAsProcessed(UUID eventId) {
        outboxEventRepository.markAsProcessed(eventId, LocalDateTime.now());
    }
    
    public void markEventAsFailed(UUID eventId) {
        outboxEventRepository.markAsFailed(eventId);
    }
    
    public void markEventsAsProcessed(List<UUID> eventIds) {
        outboxEventRepository.markMultipleAsProcessed(eventIds, LocalDateTime.now());
    }
    
    @Transactional(readOnly = true)
    public long getPendingEventCount() {
        return outboxEventRepository.countPendingEvents();
    }
    
    @Transactional(readOnly = true)
    public long getFailedEventCount() {
        return outboxEventRepository.countFailedEvents();
    }
    
    public int cleanupProcessedEvents(LocalDateTime cutoffDate) {
        return outboxEventRepository.deleteProcessedEventsOlderThan(cutoffDate);
    }
}