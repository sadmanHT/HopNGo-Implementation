package com.hopngo.booking.controller;

import com.hopngo.booking.dto.*;
import com.hopngo.booking.entity.*;
import com.hopngo.booking.mapper.*;
import com.hopngo.booking.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Booking Service", description = "APIs for managing vendors, listings, bookings, and reviews")
public class BookingController {
    
    private final VendorService vendorService;
    private final ListingService listingService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final VendorMapper vendorMapper;
    private final ListingMapper listingMapper;
    private final BookingMapper bookingMapper;
    private final ReviewMapper reviewMapper;
    
    @Autowired
    public BookingController(VendorService vendorService,
                           ListingService listingService,
                           BookingService bookingService,
                           ReviewService reviewService,
                           VendorMapper vendorMapper,
                           ListingMapper listingMapper,
                           BookingMapper bookingMapper,
                           ReviewMapper reviewMapper) {
        this.vendorService = vendorService;
        this.listingService = listingService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
        this.vendorMapper = vendorMapper;
        this.listingMapper = listingMapper;
        this.bookingMapper = bookingMapper;
        this.reviewMapper = reviewMapper;
    }
    
    // Vendor Management
    
    @PostMapping("/vendors")
    @Operation(summary = "Create vendor profile", description = "Create a new vendor profile for PROVIDER users")
    public ResponseEntity<VendorResponse> createVendor(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody VendorCreateRequest request) {
        
        if (!"PROVIDER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Vendor vendor = vendorMapper.toEntity(request);
        vendor.setUserId(userId);
        vendor = vendorService.createVendor(
            userId, vendor.getBusinessName(), vendor.getContactEmail(),
            vendor.getDescription(), vendor.getContactPhone(),
            vendor.getAddress(), vendor.getLatitude(), vendor.getLongitude()
        );
        
        VendorResponse response = vendorMapper.toResponse(vendor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/vendors/me")
    @Operation(summary = "Get my vendor profile")
    public ResponseEntity<VendorResponse> getMyVendorProfile(
            @RequestHeader("X-User-ID") String userId) {
        
        Optional<Vendor> vendor = vendorService.findByUserId(userId);
        return vendor.map(v -> ResponseEntity.ok(vendorMapper.toResponse(v)))
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/vendors/me")
    @Operation(summary = "Update my vendor profile")
    public ResponseEntity<VendorResponse> updateMyVendorProfile(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody VendorCreateRequest request) {
        
        // First get the vendor to get the vendorId
        Optional<Vendor> existingVendor = vendorService.findByUserId(userId);
        if (existingVendor.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found for user: " + userId);
        }
        
        Vendor vendor = existingVendor.get();
        vendorMapper.updateEntity(request, vendor);
        vendor = vendorService.updateVendor(
            vendor.getId(), vendor.getBusinessName(), vendor.getContactEmail(),
            vendor.getContactPhone(), vendor.getDescription(),
            vendor.getAddress(), vendor.getLatitude(), vendor.getLongitude()
        );
        
        VendorResponse response = vendorMapper.toResponse(vendor);
        return ResponseEntity.ok(response);
    }
    
    // Listing Management
    
    @PostMapping("/listings")
    @Operation(summary = "Create listing", description = "Create a new listing (PROVIDER only)")
    public ResponseEntity<ListingResponse> createListing(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ListingCreateRequest request) {
        
        if (!"PROVIDER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Listing listing = listingMapper.toEntity(request);
        listing = listingService.createListing(
            userId, listing.getTitle(), listing.getDescription(), "ACCOMMODATION", // default category
            listing.getBasePrice(), "USD", listing.getMaxGuests(),
            listing.getAmenities(), listing.getImages(), listing.getAddress(),
            listing.getLatitude(), listing.getLongitude()
        );
        
        ListingResponse response = listingMapper.toResponse(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/listings")
    @Operation(summary = "Search listings", description = "Search listings with filters")
    public ResponseEntity<List<ListingResponse>> searchListings(
            @RequestParam(required = false) @Parameter(description = "Latitude for geo search") Double lat,
            @RequestParam(required = false) @Parameter(description = "Longitude for geo search") Double lng,
            @RequestParam(required = false) @Parameter(description = "Search radius in km") Double radius,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer maxGuests,
            @RequestParam(required = false) List<String> amenities) {
        
        List<Listing> listings = listingService.searchListings(
            lat, lng, radius, from, to, minPrice, maxPrice, category, maxGuests, amenities
        );
        
        List<ListingResponse> responses = listings.stream()
            .map(listingMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/listings/{listingId}")
    @Operation(summary = "Get listing details")
    public ResponseEntity<ListingResponse> getListingDetails(@PathVariable UUID listingId) {
        Optional<Listing> listing = listingService.findById(listingId);
        return listing.map(l -> ResponseEntity.ok(listingMapper.toResponse(l)))
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/listings/me")
    @Operation(summary = "Get my listings")
    public ResponseEntity<List<ListingResponse>> getMyListings(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Listing> listings = listingService.findByVendorUserId(userId);
        List<ListingResponse> responses = listings.stream()
            .map(listingMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/listings/{listingId}")
    @Operation(summary = "Update listing")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable UUID listingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody ListingCreateRequest request) {
        
        Optional<Listing> existingListing = listingService.findById(listingId);
        if (existingListing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Listing listing = existingListing.get();
        listingMapper.updateEntity(request, listing);
        listing = listingService.updateListing(
            listingId, userId, listing.getTitle(), listing.getDescription(),
            "ACCOMMODATION", listing.getBasePrice(), "USD",
            listing.getMaxGuests(), listing.getAmenities(), listing.getImages(),
            listing.getAddress(), listing.getLatitude(), listing.getLongitude()
        );
        
        ListingResponse response = listingMapper.toResponse(listing);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/listings/{listingId}/inventory")
    @Operation(summary = "Create inventory for listing")
    public ResponseEntity<Void> createInventory(
            @PathVariable UUID listingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody InventoryCreateRequest request) {
        
        listingService.createInventoryForPeriod(
            listingId, userId, request.date(), request.date(),
            request.availableQuantity(), request.priceOverride()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("/listings/{listingId}/inventory")
    @Operation(summary = "Get inventory for listing")
    public ResponseEntity<List<Inventory>> getInventory(
            @PathVariable UUID listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Inventory> inventory = listingService.getInventoryForPeriod(listingId, startDate, endDate);
        return ResponseEntity.ok(inventory);
    }
    
    // Booking Management
    
    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new PENDING booking")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody BookingCreateRequest request) {
        
        Booking booking = bookingService.createBooking(
            userId, request.listingId(), request.checkInDate(),
            request.checkOutDate(), request.numberOfGuests(),
            request.specialRequests()
        );
        
        BookingResponse response = bookingMapper.toResponse(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get my bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Booking> bookings = bookingService.findByUserId(userId);
        List<BookingResponse> responses = bookings.stream()
            .map(bookingMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/vendor")
    @Operation(summary = "Get bookings for my listings")
    public ResponseEntity<List<BookingResponse>> getVendorBookings(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Booking> bookings = bookingService.findByVendorUserId(userId);
        List<BookingResponse> responses = bookings.stream()
            .map(bookingMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details")
    public ResponseEntity<BookingResponse> getBookingDetails(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-ID") String userId) {
        
        Optional<Booking> booking = bookingService.findById(bookingId);
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Booking b = booking.get();
        // Check if user owns the booking or is the vendor
        if (!b.getUserId().equals(userId) && !b.getVendor().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        BookingResponse response = bookingMapper.toResponse(b);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{bookingId}")
    @Operation(summary = "Update booking status", description = "Cancel booking if allowed")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody BookingUpdateRequest request) {
        
        if ("CANCELLED".equals(request.status())) {
            Booking booking = bookingService.cancelBooking(bookingId, userId);
            BookingResponse response = bookingMapper.toResponse(booking);
            return ResponseEntity.ok(response);
        } else if ("CONFIRMED".equals(request.status())) {
            Booking booking = bookingService.confirmBooking(bookingId, userId);
            BookingResponse response = bookingMapper.toResponse(booking);
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/reference/{bookingReference}")
    @Operation(summary = "Get booking by reference")
    public ResponseEntity<BookingResponse> getBookingByReference(
            @PathVariable String bookingReference,
            @RequestHeader("X-User-ID") String userId) {
        
        Optional<Booking> booking = bookingService.findByBookingReference(bookingReference);
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Booking b = booking.get();
        // Check if user owns the booking or is the vendor
        if (!b.getUserId().equals(userId) && !b.getVendor().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        BookingResponse response = bookingMapper.toResponse(b);
        return ResponseEntity.ok(response);
    }
    
    // Review Management
    
    @PostMapping("/{bookingId}/review")
    @Operation(summary = "Create review", description = "Create review for completed booking")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        
        Review review = reviewService.createReview(
            bookingId, userId, request.rating(), null, request.comment()
        );
        
        ReviewResponse response = reviewMapper.toResponse(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/reviews/me")
    @Operation(summary = "Get my reviews")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Review> reviews = reviewService.findByUserId(userId);
        List<ReviewResponse> responses = reviews.stream()
            .map(reviewMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/listings/{listingId}/reviews")
    @Operation(summary = "Get reviews for listing")
    public ResponseEntity<List<ReviewResponse>> getListingReviews(@PathVariable UUID listingId) {
        List<Review> reviews = reviewService.findByListingId(listingId);
        List<ReviewResponse> responses = reviews.stream()
            .map(reviewMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/vendors/{vendorId}/reviews")
    @Operation(summary = "Get reviews for vendor")
    public ResponseEntity<List<ReviewResponse>> getVendorReviews(@PathVariable UUID vendorId) {
        List<Review> reviews = reviewService.findByVendorId(vendorId);
        List<ReviewResponse> responses = reviews.stream()
            .map(reviewMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/reviews/{reviewId}")
    @Operation(summary = "Update review")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID reviewId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        
        Review review = reviewService.updateReview(
            reviewId, userId, request.rating(),
            null, request.comment()
        );
        
        ReviewResponse response = reviewMapper.toResponse(review);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "Delete review")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID reviewId,
            @RequestHeader("X-User-ID") String userId) {
        
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
    
    // Statistics and Analytics
    
    @GetMapping("/listings/{listingId}/stats")
    @Operation(summary = "Get listing statistics")
    public ResponseEntity<Map<String, Object>> getListingStats(@PathVariable UUID listingId) {
        Double avgRating = listingService.getAverageRating(listingId);
        long reviewCount = listingService.getReviewCount(listingId);
        
        Map<String, Object> stats = Map.of(
            "averageRating", avgRating != null ? avgRating : 0.0,
            "reviewCount", reviewCount
        );
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/vendors/me/stats")
    @Operation(summary = "Get vendor statistics")
    public ResponseEntity<Map<String, Object>> getVendorStats(
            @RequestHeader("X-User-ID") String userId) {
        
        Optional<Vendor> vendorOpt = vendorService.findByUserId(userId);
        if (vendorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Vendor vendor = vendorOpt.get();
        Double avgRating = reviewService.getAverageRatingForVendor(vendor.getId());
        long reviewCount = reviewService.getReviewCountForVendor(vendor.getId());
        long bookingCount = bookingService.countBookingsByVendor(vendor.getId());
        
        Map<String, Object> stats = Map.of(
            "averageRating", avgRating != null ? avgRating : 0.0,
            "reviewCount", reviewCount,
            "bookingCount", bookingCount
        );
        
        return ResponseEntity.ok(stats);
    }
    
    // Exception Handlers
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation Failed");
        response.put("message", "Invalid input data");
        response.put("errors", errors);
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Bad Request", "message", e.getMessage()));
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Forbidden", "message", e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Internal Server Error", "message", "An unexpected error occurred"));
    }
}