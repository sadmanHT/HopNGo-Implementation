package com.hopngo.booking.service;

import com.hopngo.booking.entity.Listing;
import com.hopngo.booking.entity.Vendor;
import com.hopngo.booking.entity.Inventory;
import com.hopngo.booking.repository.ListingRepository;
import com.hopngo.booking.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ListingService {
    
    private final ListingRepository listingRepository;
    private final InventoryRepository inventoryRepository;
    private final VendorService vendorService;
    private final OutboxService outboxService;
    
    @Autowired
    public ListingService(ListingRepository listingRepository, 
                         InventoryRepository inventoryRepository,
                         VendorService vendorService,
                         OutboxService outboxService) {
        this.listingRepository = listingRepository;
        this.inventoryRepository = inventoryRepository;
        this.vendorService = vendorService;
        this.outboxService = outboxService;
    }
    
    public Listing createListing(String userId, String title, String description, String category,
                                BigDecimal basePrice, String currency, Integer maxGuests,
                                String[] amenities, String[] images, String address,
                                BigDecimal latitude, BigDecimal longitude) {
        
        // Validate that user is an active vendor
        vendorService.validateProviderRole(userId);
        
        Vendor vendor = vendorService.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user: " + userId));
        
        Listing listing = new Listing(vendor, title, category, basePrice);
        listing.setDescription(description);
        listing.setCurrency(currency != null ? currency : "USD");
        listing.setMaxGuests(maxGuests != null ? maxGuests : 1);
        listing.setAmenities(amenities);
        listing.setImages(images);
        listing.setAddress(address);
        listing.setLatitude(latitude);
        listing.setLongitude(longitude);
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        
        Listing savedListing = listingRepository.save(listing);
        
        // Publish listing created event
        outboxService.publishListingCreatedEvent(savedListing);
        
        return savedListing;
    }
    
    @Transactional(readOnly = true)
    public Optional<Listing> findById(UUID listingId) {
        return listingRepository.findById(listingId);
    }
    
    @Transactional(readOnly = true)
    public List<Listing> findByVendorId(UUID vendorId) {
        return listingRepository.findByVendorId(vendorId);
    }
    
    @Transactional(readOnly = true)
    public List<Listing> findByVendorUserId(String userId) {
        Vendor vendor = vendorService.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user: " + userId));
        return listingRepository.findByVendorId(vendor.getId());
    }
    
    @Transactional(readOnly = true)
    public List<Listing> searchListings(Double latitude, Double longitude, Double radiusKm,
                                       LocalDate fromDate, LocalDate toDate,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       String category, Integer maxGuests, List<String> amenities) {
        
        List<Listing> listings;
        
        if (latitude != null && longitude != null && radiusKm != null) {
            // Search with geo-location
            listings = listingRepository.searchListings(
                latitude, longitude, radiusKm, category, minPrice, maxPrice, maxGuests, amenities
            );
        } else {
            // Search without geo-location
            listings = listingRepository.findActiveListingsWithFilters(
                category, minPrice, maxPrice, maxGuests
            );
            
            // Filter by amenities if provided
            if (amenities != null && !amenities.isEmpty()) {
                listings = listings.stream()
                    .filter(listing -> hasAnyAmenity(listing, amenities))
                    .collect(Collectors.toList());
            }
        }
        
        // Filter by availability if dates are provided
        if (fromDate != null && toDate != null) {
            listings = listings.stream()
                .filter(listing -> isAvailableForPeriod(listing.getId(), fromDate, toDate, 1))
                .collect(Collectors.toList());
        }
        
        return listings;
    }
    
    private boolean hasAnyAmenity(Listing listing, List<String> requiredAmenities) {
        if (listing.getAmenities() == null || listing.getAmenities().length == 0) {
            return false;
        }
        
        for (String amenity : listing.getAmenities()) {
            if (requiredAmenities.contains(amenity)) {
                return true;
            }
        }
        return false;
    }
    
    @Transactional(readOnly = true)
    public boolean isAvailableForPeriod(UUID listingId, LocalDate startDate, LocalDate endDate, Integer guests) {
        long daysBetween = startDate.until(endDate).getDays();
        if (daysBetween <= 0) {
            return false;
        }
        
        return inventoryRepository.isAvailableForPeriod(
            listingId, startDate, endDate.minusDays(1), guests, daysBetween
        );
    }
    
    public Listing updateListing(UUID listingId, String userId, String title, String description,
                                String category, BigDecimal basePrice, String currency,
                                Integer maxGuests, String[] amenities, String[] images,
                                String address, BigDecimal latitude, BigDecimal longitude) {
        
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        
        // Validate ownership
        if (!listing.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User does not own this listing");
        }
        
        if (title != null) listing.setTitle(title);
        if (description != null) listing.setDescription(description);
        if (category != null) listing.setCategory(category);
        if (basePrice != null) listing.setBasePrice(basePrice);
        if (currency != null) listing.setCurrency(currency);
        if (maxGuests != null) listing.setMaxGuests(maxGuests);
        if (amenities != null) listing.setAmenities(amenities);
        if (images != null) listing.setImages(images);
        if (address != null) listing.setAddress(address);
        if (latitude != null) listing.setLatitude(latitude);
        if (longitude != null) listing.setLongitude(longitude);
        
        Listing updatedListing = listingRepository.save(listing);
        
        // Publish listing updated event
        outboxService.publishListingUpdatedEvent(updatedListing);
        
        return updatedListing;
    }
    
    public void deactivateListing(UUID listingId, String userId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        
        // Validate ownership
        if (!listing.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User does not own this listing");
        }
        
        listing.setStatus(Listing.ListingStatus.INACTIVE);
        listingRepository.save(listing);
    }
    
    public void activateListing(UUID listingId, String userId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        
        // Validate ownership
        if (!listing.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User does not own this listing");
        }
        
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listingRepository.save(listing);
    }
    
    public void createInventoryForPeriod(UUID listingId, String userId, LocalDate startDate, 
                                        LocalDate endDate, Integer availableQuantity, 
                                        BigDecimal priceOverride) {
        
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        
        // Validate ownership
        if (!listing.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User does not own this listing");
        }
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            Optional<Inventory> existingInventory = inventoryRepository.findByListingIdAndDate(listingId, currentDate);
            
            if (existingInventory.isEmpty()) {
                Inventory inventory = new Inventory(listing, currentDate, availableQuantity);
                inventory.setPriceOverride(priceOverride);
                inventoryRepository.save(inventory);
            }
            
            currentDate = currentDate.plusDays(1);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Inventory> getInventoryForPeriod(UUID listingId, LocalDate startDate, LocalDate endDate) {
        return inventoryRepository.findByListingIdAndDateBetween(listingId, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPrice(UUID listingId, LocalDate startDate, LocalDate endDate, Integer guests) {
        List<Inventory> inventoryList = inventoryRepository.findByListingIdAndDateBetween(
            listingId, startDate, endDate.minusDays(1)
        );
        
        if (inventoryList.isEmpty()) {
            // Fallback to base price if no inventory exists
            Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
            
            long days = startDate.until(endDate).getDays();
            return listing.getBasePrice().multiply(BigDecimal.valueOf(days));
        }
        
        return inventoryList.stream()
            .map(Inventory::getEffectivePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transactional(readOnly = true)
    public Double getAverageRating(UUID listingId) {
        return listingRepository.getAverageRating(listingId);
    }
    
    @Transactional(readOnly = true)
    public long getReviewCount(UUID listingId) {
        return listingRepository.getReviewCount(listingId);
    }
    
    @Transactional(readOnly = true)
    public List<Listing> getActiveListings() {
        return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE);
    }
    
    @Transactional(readOnly = true)
    public void validateListingOwnership(UUID listingId, String userId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        
        if (!listing.getVendor().getUserId().equals(userId)) {
            throw new SecurityException("User does not own this listing");
        }
    }
}