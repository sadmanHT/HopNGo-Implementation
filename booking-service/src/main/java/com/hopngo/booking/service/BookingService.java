package com.hopngo.booking.service;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import com.hopngo.booking.entity.Listing;
import com.hopngo.booking.entity.Vendor;
import com.hopngo.booking.entity.Inventory;
import com.hopngo.booking.repository.BookingRepository;
import com.hopngo.booking.repository.InventoryRepository;
import com.hopngo.booking.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final InventoryRepository inventoryRepository;
    private final ListingService listingService;
    private final OutboxService outboxService;
    private final AiEmbeddingService aiEmbeddingService;
    
    @Autowired
    public BookingService(BookingRepository bookingRepository,
                         ListingRepository listingRepository,
                         InventoryRepository inventoryRepository,
                         ListingService listingService,
                         OutboxService outboxService,
                         AiEmbeddingService aiEmbeddingService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.inventoryRepository = inventoryRepository;
        this.listingService = listingService;
        this.outboxService = outboxService;
        this.aiEmbeddingService = aiEmbeddingService;
    }
    
    @CacheEvict(value = "bookings", allEntries = true)
    public Booking createBooking(String userId, UUID listingId, LocalDate startDate, 
                                LocalDate endDate, Integer guests, String specialRequests) {
        
        // Validate dates
        if (startDate.isAfter(endDate) || startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid booking dates");
        }
        
        // Find listing
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing is not available for booking");
        }
        
        // Validate guest count
        if (guests > listing.getMaxGuests()) {
            throw new IllegalArgumentException("Guest count exceeds listing capacity");
        }
        
        // Check for conflicting bookings
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
            listingId, startDate, endDate
        );
        if (!conflictingBookings.isEmpty()) {
            throw new IllegalArgumentException("Booking conflicts with existing reservations");
        }
        
        // Reserve inventory with pessimistic locking
        try {
            boolean reserved = reserveInventoryForPeriod(listingId, startDate, endDate, guests);
            if (!reserved) {
                throw new IllegalArgumentException("Insufficient availability for the requested period");
            }
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Booking conflict detected. Please try again.");
        }
        
        // Calculate total amount
        BigDecimal totalAmount = listingService.calculateTotalPrice(listingId, startDate, endDate, guests);
        
        // Create booking
        Booking booking = new Booking(userId, listing, startDate, endDate, guests);
        booking.setTotalAmount(totalAmount);
        booking.setCurrency(listing.getCurrency());
        booking.setSpecialRequests(specialRequests);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // Generate and upsert embeddings for search
        aiEmbeddingService.processBookingEmbedding(
            savedBooking.getId(), userId, listingId,
            listing.getTitle(), startDate, endDate, guests,
            specialRequests, totalAmount
        );

        // Publish booking created event
        outboxService.publishBookingCreatedEvent(savedBooking);
        
        // Publish add to cart event for analytics funnel tracking
        try {
            outboxService.publishAddToCartEvent(
                listingId.toString(),
                listing.getVendor().getId().toString(),
                userId
            );
        } catch (Exception e) {
            // Log error but don't fail the booking creation
            System.err.println("Failed to emit add to cart event: " + e.getMessage());
        }
        
        return savedBooking;
    }
    
    private boolean reserveInventoryForPeriod(UUID listingId, LocalDate startDate, 
                                            LocalDate endDate, Integer guests) {
        
        int reservedCount = inventoryRepository.reserveInventory(listingId, startDate, endDate, guests);
        
        // Calculate expected number of days
        long expectedDays = startDate.until(endDate).getDays();
        
        if (reservedCount < expectedDays) {
            // Rollback reservations if not all days were reserved
            inventoryRepository.releaseInventory(listingId, startDate, endDate, guests);
            return false;
        }
        return true;
    }
    
    private void rollbackInventoryReservations(UUID listingId, LocalDate startDate, 
                                              LocalDate endDate, Integer guests) {
        inventoryRepository.releaseInventory(listingId, startDate, endDate, guests);
    }
    
    @Transactional(readOnly = true)
    public Optional<Booking> findById(UUID bookingId) {
        return bookingRepository.findById(bookingId);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "bookings", key = "'user:' + #userId + ':' + #page + ':' + #size")
    public Page<Booking> findByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "bookings", key = "'vendor:' + #vendorUserId + ':' + #page + ':' + #size")
    public Page<Booking> findByVendorUserId(String vendorUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookingRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Optional<Booking> findByBookingReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference);
    }
    
    @CacheEvict(value = "bookings", allEntries = true)
    public Booking confirmBooking(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // Validate ownership (vendor can confirm)
        if (!booking.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User cannot confirm this booking");
        }
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking cannot be confirmed in current status: " + booking.getStatus());
        }
        
        booking.confirm();
        Booking confirmedBooking = bookingRepository.save(booking);
        
        // Publish booking confirmed event
        outboxService.publishBookingConfirmedEvent(confirmedBooking);
        
        return confirmedBooking;
    }
    
    @CacheEvict(value = "bookings", allEntries = true)
    public Booking cancelBooking(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // Validate ownership (user or vendor can cancel)
        if (!booking.getUserId().equals(userId) && !booking.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User cannot cancel this booking");
        }
        
        if (!booking.canBeCancelled()) {
            throw new IllegalArgumentException("Booking cannot be cancelled in current status: " + booking.getStatus());
        }
        
        // Release inventory
        releaseInventoryForBooking(booking);
        
        booking.cancel();
        Booking cancelledBooking = bookingRepository.save(booking);
        
        // Publish booking cancelled event
        outboxService.publishBookingCancelledEvent(cancelledBooking);
        
        return cancelledBooking;
    }
    
    private void releaseInventoryForBooking(Booking booking) {
        inventoryRepository.releaseInventory(
            booking.getListing().getId(), 
            booking.getStartDate(), 
            booking.getEndDate(), 
            booking.getGuests()
        );
    }
    
    @Transactional(readOnly = true)
    public List<Booking> findBookingsEligibleForReview(String userId) {
        return bookingRepository.findBookingsEligibleForReview(userId, LocalDate.now());
    }
    
    @Transactional(readOnly = true)
    public boolean canUserReviewBooking(UUID bookingId, String userId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return false;
        }
        
        Booking booking = bookingOpt.get();
        return booking.canBeReviewed() && booking.getUserId().equals(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Booking> findExpiredPendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // 24 hours to confirm
        return bookingRepository.findExpiredPendingBookings(cutoff);
    }
    
    public void expirePendingBookings() {
        List<Booking> expiredBookings = findExpiredPendingBookings();
        
        for (Booking booking : expiredBookings) {
            // Release inventory
            releaseInventoryForBooking(booking);
            
            // Cancel booking
            booking.cancel();
            bookingRepository.save(booking);
            
            // Publish booking expired event
            outboxService.publishBookingCancelledEvent(booking);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Booking> findByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public long countBookingsByUser(String userId) {
        return bookingRepository.countByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public long countBookingsByVendor(UUID vendorId) {
        return bookingRepository.countByVendorId(vendorId);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateVendorRevenue(UUID vendorId, LocalDate startDate, LocalDate endDate) {
        return bookingRepository.calculateVendorRevenue(vendorId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }
    
    @Transactional(readOnly = true)
    public List<Booking> findCompletedBookings(String userId) {
        return bookingRepository.findCompletedBookings(LocalDate.now());
    }
    
    private String generateBookingReference() {
        // Generate a unique booking reference (e.g., HNG-YYYYMMDD-XXXX)
        String prefix = "HNG";
        String date = LocalDate.now().toString().replace("-", "");
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("%s-%s-%04d", prefix, date, random);
    }
    
    @Transactional(readOnly = true)
    public void validateBookingOwnership(UUID bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this booking");
        }
    }
    
    @Transactional(readOnly = true)
    public void validateVendorBookingAccess(UUID bookingId, String vendorUserId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        if (!booking.getVendor().getUserId().equals(vendorUserId)) {
            throw new SecurityException("Vendor does not have access to this booking");
        }
    }
}