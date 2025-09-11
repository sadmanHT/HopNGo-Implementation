package com.hopngo.booking.controller;

import com.hopngo.booking.dto.ListingResponse;
import com.hopngo.booking.entity.Listing;
import com.hopngo.booking.mapper.ListingMapper;
import com.hopngo.booking.service.ListingService;
import com.hopngo.search.helper.ListingsIndexHelper;
import com.hopngo.search.helper.ListingsIndexHelper.ListingDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings/search")
@Tag(name = "Booking Search", description = "Search endpoints for listings")
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    private final ListingsIndexHelper listingsIndexHelper;
    private final ListingService listingService;
    private final ListingMapper listingMapper;
    
    @Autowired(required = false)
    public SearchController(ListingsIndexHelper listingsIndexHelper,
                           ListingService listingService,
                           ListingMapper listingMapper) {
        this.listingsIndexHelper = listingsIndexHelper;
        this.listingService = listingService;
        this.listingMapper = listingMapper;
    }
    
    @GetMapping("/listings")
    @Operation(summary = "Search listings", description = "Search listings using OpenSearch with fallback to database")
    public ResponseEntity<List<ListingResponse>> searchListings(
            @RequestParam(required = false) @Parameter(description = "Search query") String q,
            @RequestParam(required = false) @Parameter(description = "Latitude for geo search") Double lat,
            @RequestParam(required = false) @Parameter(description = "Longitude for geo search") Double lng,
            @RequestParam(required = false) @Parameter(description = "Search radius in km") Double radius,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer maxGuests,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Listing> listings;
        
        // Try OpenSearch first if available
        if (listingsIndexHelper != null) {
            try {
                // Convert parameters to match ListingsIndexHelper signature
                String vendorId = null; // Not available in this context
                Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
                Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;
                String currency = "USD"; // Default currency
                Double latDouble = lat;
                Double lngDouble = lng;
                String radiusStr = radius != null ? radius.toString() + "km" : null;
                int fromOffset = page * size;
                
                List<ListingDocument> searchResults = listingsIndexHelper.searchListings(
                    q, vendorId, amenities, minPriceDouble, maxPriceDouble, currency,
                    latDouble, lngDouble, radiusStr, size, fromOffset
                );
                
                // Convert search results to listings
                List<String> listingIds = searchResults.stream()
                    .map(ListingDocument::getId)
                    .collect(Collectors.toList());
                
                if (!listingIds.isEmpty()) {
                    listings = listingIds.stream()
                        .map(id -> {
                            try {
                                return listingService.findById(java.util.UUID.fromString(id));
                            } catch (Exception e) {
                                logger.warn("Failed to find listing with ID: {}", id, e);
                                return Optional.<Listing>empty();
                            }
                        })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
                } else {
                    listings = List.of();
                }
                
                logger.debug("OpenSearch returned {} listings for query: {}", listings.size(), q);
            } catch (Exception e) {
                logger.warn("OpenSearch failed, falling back to database search", e);
                listings = fallbackToDatabase(lat, lng, radius, from, to, minPrice, maxPrice, category, maxGuests, amenities);
            }
        } else {
            logger.debug("OpenSearch not available, using database search");
            listings = fallbackToDatabase(lat, lng, radius, from, to, minPrice, maxPrice, category, maxGuests, amenities);
        }
        
        List<ListingResponse> responses = listings.stream()
            .map(listingMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get search suggestions for listings")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam @Parameter(description = "Search prefix") String prefix) {
        
        if (listingsIndexHelper != null) {
            try {
                // Use searchListings with the prefix as query to get suggestions
                List<ListingDocument> results = listingsIndexHelper.searchListings(
                    prefix, null, null, null, null, null, null, null, null, 10, 0
                );
                List<String> suggestions = results.stream()
                    .map(ListingDocument::getTitle)
                    .distinct()
                    .collect(Collectors.toList());
                return ResponseEntity.ok(suggestions);
            } catch (Exception e) {
                logger.warn("Failed to get suggestions from OpenSearch", e);
            }
        }
        
        // Fallback: return empty suggestions
        return ResponseEntity.ok(List.of());
    }
    
    @PostMapping("/reindex")
    @Operation(summary = "Trigger reindexing", description = "Trigger reindexing of all listings (admin only)")
    public ResponseEntity<Map<String, Object>> triggerReindex(
            @RequestHeader("X-User-Role") String userRole) {
        
        // TODO: Add proper admin role check
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        if (listingsIndexHelper == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Search service not available"));
        }
        
        try {
            List<Listing> allListings = listingService.getActiveListings();
            
            List<ListingDocument> documents = allListings.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
            
            listingsIndexHelper.bulkIndexListings(documents);
            
            return ResponseEntity.ok(Map.of(
                "message", "Reindexing completed",
                "indexed_count", documents.size()
            ));
        } catch (Exception e) {
            logger.error("Failed to reindex listings", e);
            return ResponseEntity.status(500).body(Map.of("error", "Reindexing failed: " + e.getMessage()));
        }
    }
    
    private List<Listing> fallbackToDatabase(Double lat, Double lng, Double radius,
                                           LocalDate from, LocalDate to,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           String category, Integer maxGuests,
                                           List<String> amenities) {
        Page<Listing> page = listingService.searchListings(
            lat, lng, radius, from, to, minPrice, maxPrice, category, maxGuests, amenities, 0, 50
        );
        return page.getContent();
    }
    
    private ListingDocument convertToDocument(Listing listing) {
        ListingDocument doc = new ListingDocument();
        doc.setId(listing.getId().toString());
        doc.setTitle(listing.getTitle());
        doc.setDescription(listing.getDescription());
        doc.setPrice(listing.getBasePrice() != null ? listing.getBasePrice().doubleValue() : 0.0);
        doc.setAmenities(listing.getAmenities() != null ? List.of(listing.getAmenities()) : List.of());
        if (listing.getLatitude() != null && listing.getLongitude() != null) {
            ListingsIndexHelper.GeoLocation geo = new ListingsIndexHelper.GeoLocation(
                listing.getLatitude().doubleValue(), listing.getLongitude().doubleValue()
            );
            doc.setGeo(geo);
        }
        doc.setVendorId(listing.getVendor().getId().toString());
        doc.setRating(4.5f); // Default rating as Float
        return doc;
    }
}