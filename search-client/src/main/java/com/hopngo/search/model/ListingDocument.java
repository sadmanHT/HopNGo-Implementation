package com.hopngo.search.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Document model for listing search index
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingDocument {
    
    private String id;
    private String title;
    private String description;
    private String category;
    private BigDecimal basePrice;
    private String currency;
    private Integer maxGuests;
    private List<String> amenities;
    private List<String> images;
    private String address;
    private Double latitude;
    private Double longitude;
    private String status;
    private String vendorId;
    private String vendorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double rating;
    
    // Geo point for OpenSearch
    public String getGeo() {
        if (latitude != null && longitude != null) {
            return latitude + "," + longitude;
        }
        return null;
    }
    
    // Price for search filtering
    public BigDecimal getPrice() {
        return basePrice;
    }
}