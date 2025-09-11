package com.hopngo.booking.service;

import com.hopngo.booking.entity.Listing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingService.class);
    
    public void indexListing(Listing listing) {
        logger.debug("Search indexing not available, skipping listing: {}", listing.getId());
    }
    
    public void updateListing(Listing listing) {
        logger.debug("Search indexing not available, skipping listing update: {}", listing.getId());
    }
    
    public void deleteListing(String listingId) {
        logger.debug("Search indexing not available, skipping listing deletion: {}", listingId);
    }
    
    public boolean isIndexingEnabled() {
        return false;
    }
}
