package com.hopngo.booking.controller;

import com.hopngo.booking.entity.*;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Booking Service", description = "APIs for managing vendors, listings, bookings, and reviews")
public class BookingController {
    
    private final VendorService vendorService;
    private final ListingService listingService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    
    @Autowired
    public BookingController(VendorService vendorService,
                           ListingService listingService,
                           BookingService bookingService,
                           ReviewService reviewService) {
        this.vendorService = vendorService;
        this.listingService = listingService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }
    
    // Vendor Management
    
    @PostMapping("/vendors")
    @Operation(summary = "Create vendor profile", description = "Create a new vendor profile for PROVIDER users")
    public ResponseEntity<Vendor> createVendor(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody CreateVendorRequest request) {
        
        if (!"PROVIDER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Vendor vendor = vendorService.createVendor(
            userId, request.getBusinessName(), request.getContactEmail(),
            null, // description
            null, // contactPhone
            request.getAddress(), request.getLatitude(), request.getLongitude()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(vendor);
    }
    
    @GetMapping("/vendors/me")
    @Operation(summary = "Get my vendor profile")
    public ResponseEntity<Vendor> getMyVendorProfile(
            @RequestHeader("X-User-ID") String userId) {
        
        Optional<Vendor> vendor = vendorService.findByUserId(userId);
        return vendor.map(v -> ResponseEntity.ok(v))
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/vendors/me")
    @Operation(summary = "Update my vendor profile")
    public ResponseEntity<Vendor> updateMyVendorProfile(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody UpdateVendorRequest request) {
        
        // First get the vendor to get the vendorId
        Optional<Vendor> existingVendor = vendorService.findByUserId(userId);
        if (existingVendor.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found for user: " + userId);
        }
        
        Vendor vendor = vendorService.updateVendor(
            existingVendor.get().getId(), request.getBusinessName(), null, // description
            request.getContactEmail(), null, // contactPhone
            request.getAddress(), request.getLatitude(), request.getLongitude()
        );
        
        return ResponseEntity.ok(vendor);
    }
    
    // Listing Management
    
    @PostMapping("/listings")
    @Operation(summary = "Create listing", description = "Create a new listing (PROVIDER only)")
    public ResponseEntity<Listing> createListing(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody CreateListingRequest request) {
        
        if (!"PROVIDER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Listing listing = listingService.createListing(
            userId, request.getTitle(), request.getDescription(), request.getCategory(),
            request.getBasePrice(), request.getCurrency(), request.getMaxGuests(),
            request.getAmenities(), request.getImages(), request.getAddress(),
            request.getLatitude(), request.getLongitude()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(listing);
    }
    
    @GetMapping("/listings")
    @Operation(summary = "Search listings", description = "Search listings with filters")
    public ResponseEntity<List<Listing>> searchListings(
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
        
        return ResponseEntity.ok(listings);
    }
    
    @GetMapping("/listings/{listingId}")
    @Operation(summary = "Get listing details")
    public ResponseEntity<Listing> getListingDetails(@PathVariable UUID listingId) {
        Optional<Listing> listing = listingService.findById(listingId);
        return listing.map(l -> ResponseEntity.ok(l))
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/listings/me")
    @Operation(summary = "Get my listings")
    public ResponseEntity<List<Listing>> getMyListings(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Listing> listings = listingService.findByVendorUserId(userId);
        return ResponseEntity.ok(listings);
    }
    
    @PutMapping("/listings/{listingId}")
    @Operation(summary = "Update listing")
    public ResponseEntity<Listing> updateListing(
            @PathVariable UUID listingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody UpdateListingRequest request) {
        
        Listing listing = listingService.updateListing(
            listingId, userId, request.getTitle(), request.getDescription(),
            request.getCategory(), request.getBasePrice(), request.getCurrency(),
            request.getMaxGuests(), request.getAmenities(), request.getImages(),
            request.getAddress(), request.getLatitude(), request.getLongitude()
        );
        
        return ResponseEntity.ok(listing);
    }
    
    @PostMapping("/listings/{listingId}/inventory")
    @Operation(summary = "Create inventory for listing")
    public ResponseEntity<Void> createInventory(
            @PathVariable UUID listingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateInventoryRequest request) {
        
        listingService.createInventoryForPeriod(
            listingId, userId, request.getStartDate(), request.getEndDate(),
            request.getAvailableQuantity(), request.getPriceOverride()
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
    public ResponseEntity<Booking> createBooking(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateBookingRequest request) {
        
        Booking booking = bookingService.createBooking(
            userId, request.getListingId(), request.getStartDate(),
            request.getEndDate(), request.getGuests(), request.getSpecialRequests()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }
    
    @GetMapping
    @Operation(summary = "Get my bookings")
    public ResponseEntity<List<Booking>> getMyBookings(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Booking> bookings = bookingService.findByUserId(userId);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/vendor")
    @Operation(summary = "Get bookings for my listings")
    public ResponseEntity<List<Booking>> getVendorBookings(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Booking> bookings = bookingService.findByVendorUserId(userId);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details")
    public ResponseEntity<Booking> getBookingDetails(
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
        
        return ResponseEntity.ok(b);
    }
    
    @PatchMapping("/{bookingId}")
    @Operation(summary = "Update booking status", description = "Cancel booking if allowed")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody UpdateBookingRequest request) {
        
        if ("CANCELLED".equals(request.getStatus())) {
            Booking booking = bookingService.cancelBooking(bookingId, userId);
            return ResponseEntity.ok(booking);
        } else if ("CONFIRMED".equals(request.getStatus())) {
            Booking booking = bookingService.confirmBooking(bookingId, userId);
            return ResponseEntity.ok(booking);
        }
        
        return ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/reference/{bookingReference}")
    @Operation(summary = "Get booking by reference")
    public ResponseEntity<Booking> getBookingByReference(
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
        
        return ResponseEntity.ok(b);
    }
    
    // Review Management
    
    @PostMapping("/{bookingId}/review")
    @Operation(summary = "Create review", description = "Create review for completed booking")
    public ResponseEntity<Review> createReview(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateReviewRequest request) {
        
        Review review = reviewService.createReview(
            bookingId, userId, request.getRating(),
            request.getTitle(), request.getComment()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }
    
    @GetMapping("/reviews/me")
    @Operation(summary = "Get my reviews")
    public ResponseEntity<List<Review>> getMyReviews(
            @RequestHeader("X-User-ID") String userId) {
        
        List<Review> reviews = reviewService.findByUserId(userId);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/listings/{listingId}/reviews")
    @Operation(summary = "Get reviews for listing")
    public ResponseEntity<List<Review>> getListingReviews(@PathVariable UUID listingId) {
        List<Review> reviews = reviewService.findByListingId(listingId);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/vendors/{vendorId}/reviews")
    @Operation(summary = "Get reviews for vendor")
    public ResponseEntity<List<Review>> getVendorReviews(@PathVariable UUID vendorId) {
        List<Review> reviews = reviewService.findByVendorId(vendorId);
        return ResponseEntity.ok(reviews);
    }
    
    @PutMapping("/reviews/{reviewId}")
    @Operation(summary = "Update review")
    public ResponseEntity<Review> updateReview(
            @PathVariable UUID reviewId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody UpdateReviewRequest request) {
        
        Review review = reviewService.updateReview(
            reviewId, userId, request.getRating(),
            request.getTitle(), request.getComment()
        );
        
        return ResponseEntity.ok(review);
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

// Request DTOs

class CreateVendorRequest {
    @NotBlank
    private String businessName;
    
    @Email
    @NotBlank
    private String contactEmail;
    
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // Getters and setters
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}

class UpdateVendorRequest {
    private String businessName;
    private String contactEmail;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // Getters and setters
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}

class CreateListingRequest {
    @NotBlank
    private String title;
    
    private String description;
    
    @NotBlank
    private String category;
    
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;
    
    private String currency = "USD";
    
    @Min(1)
    private Integer maxGuests = 1;
    
    private String[] amenities;
    private String[] images;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }
    public String[] getAmenities() { return amenities; }
    public void setAmenities(String[] amenities) { this.amenities = amenities; }
    public String[] getImages() { return images; }
    public void setImages(String[] images) { this.images = images; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}

class UpdateListingRequest {
    private String title;
    private String description;
    private String category;
    private BigDecimal basePrice;
    private String currency;
    private Integer maxGuests;
    private String[] amenities;
    private String[] images;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }
    public String[] getAmenities() { return amenities; }
    public void setAmenities(String[] amenities) { this.amenities = amenities; }
    public String[] getImages() { return images; }
    public void setImages(String[] images) { this.images = images; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}

class CreateInventoryRequest {
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    @NotNull
    @Min(1)
    private Integer availableQuantity;
    
    private BigDecimal priceOverride;
    
    // Getters and setters
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public BigDecimal getPriceOverride() { return priceOverride; }
    public void setPriceOverride(BigDecimal priceOverride) { this.priceOverride = priceOverride; }
}

class CreateBookingRequest {
    @NotNull
    private UUID listingId;
    
    @NotNull
    @FutureOrPresent
    private LocalDate startDate;
    
    @NotNull
    @Future
    private LocalDate endDate;
    
    @NotNull
    @Min(1)
    private Integer guests;
    
    private String specialRequests;
    
    // Getters and setters
    public UUID getListingId() { return listingId; }
    public void setListingId(UUID listingId) { this.listingId = listingId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
}

class UpdateBookingRequest {
    @NotBlank
    private String status;
    
    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class CreateReviewRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
    
    private String title;
    private String comment;
    
    // Getters and setters
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}

class UpdateReviewRequest {
    @Min(1)
    @Max(5)
    private Integer rating;
    
    private String title;
    private String comment;
    
    // Getters and setters
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}