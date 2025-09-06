package com.hopngo.booking.service;

import com.hopngo.booking.entity.Listing;
import com.hopngo.search.client.helper.ListingsIndexHelper;
import com.hopngo.search.client.model.ListingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "search.opensearch.enabled", havingValue = "true", matchIfMissing = false)
public class SearchIndexingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingService.class);
    
    private final ListingsIndexHelper listingsIndexHelper;
    
    @Value("${search.opensearch.enabled:false}")
    private boolean searchEnabled;
    
    @Autowired(required = false)
    public SearchIndexingService(ListingsIndexHelper listingsIndexHelper) {
        this.listingsIndexHelper = listingsIndexHelper;
    }
    
    public void indexListing(Listing listing) {
        if (!isIndexingEnabled()) {
            logger.debug("Search indexing is disabled, skipping listing: {}", listing.getId());
            return;
        }
        
        try {
            ListingDocument document = convertToDocument(listing);
            listingsIndexHelper.indexDocument(document);
            logger.debug("Successfully indexed listing: {}", listing.getId());
        } catch (Exception e) {
            logger.error("Failed to index listing: {}", listing.getId(), e);
        }
    }
    
    public void updateListing(Listing listing) {
        if (!isIndexingEnabled()) {
            logger.debug("Search indexing is disabled, skipping listing update: {}", listing.getId());
            return;
        }
        
        try {
            ListingDocument document = convertToDocument(listing);
            listingsIndexHelper.updateDocument(document);
            logger.debug("Successfully updated listing in search index: {}", listing.getId());
        } catch (Exception e) {
            logger.error("Failed to update listing in search index: {}", listing.getId(), e);
        }
    }
    
    public void deleteListing(String listingId) {
        if (!isIndexingEnabled()) {
            logger.debug("Search indexing is disabled, skipping listing deletion: {}", listingId);
            return;
        }
        
        try {
            listingsIndexHelper.deleteDocument(listingId);
            logger.debug("Successfully deleted listing from search index: {}", listingId);
        } catch (Exception e) {
            logger.error("Failed to delete listing from search index: {}", listingId, e);
        }
    }
    
    public void bulkIndexListings(List<Listing> listings) {
        if (!isIndexingEnabled()) {
            logger.debug("Search indexing is disabled, skipping bulk indexing of {} listings", listings.size());
            return;
        }
        
        try {
            List<ListingDocument> documents = listings.stream()
                .filter(listing -> Listing.ListingStatus.ACTIVE.equals(listing.getStatus()))
                .map(this::convertToDocument)
                .collect(Collectors.toList());
            
            if (!documents.isEmpty()) {
                listingsIndexHelper.bulkIndex(documents);
                logger.info("Successfully bulk indexed {} listings", documents.size());
            } else {
                logger.debug("No active listings to index");
            }
        } catch (Exception e) {
            logger.error("Failed to bulk index {} listings", listings.size(), e);
        }
    }
    
    public boolean isIndexingEnabled() {
        return searchEnabled && listingsIndexHelper != null;
    }
    
    private ListingDocument convertToDocument(Listing listing) {
        ListingDocument doc = new ListingDocument();
        doc.setId(listing.getId().toString());
        doc.setTitle(listing.getTitle());
        doc.setDescription(listing.getDescription());
        doc.setCategory(listing.getCategory());
        doc.setBasePrice(listing.getBasePrice());
        doc.setCurrency(listing.getCurrency());
        doc.setMaxGuests(listing.getMaxGuests());
        doc.setAmenities(listing.getAmenities() != null ? List.of(listing.getAmenities()) : List.of());
        doc.setImages(listing.getImages() != null ? List.of(listing.getImages()) : List.of());
        doc.setAddress(listing.getAddress());
        doc.setLatitude(listing.getLatitude());
        doc.setLongitude(listing.getLongitude());
        doc.setStatus(listing.getStatus().name());
        doc.setVendorId(listing.getVendor().getId().toString());
        doc.setVendorName(listing.getVendor().getBusinessName());
        doc.setCreatedAt(listing.getCreatedAt());
        doc.setUpdatedAt(listing.getUpdatedAt());
        return doc;
    }
}