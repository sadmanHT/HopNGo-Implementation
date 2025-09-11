package com.hopngo.booking.service;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.Inventory;
import com.hopngo.booking.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InventoryService {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    
    private final InventoryRepository inventoryRepository;
    private final ListingService listingService;
    
    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, ListingService listingService) {
        this.inventoryRepository = inventoryRepository;
        this.listingService = listingService;
    }
    
    /**
     * Release inventory for a cancelled booking
     */
    public void releaseInventoryForBooking(Booking booking) {
        log.info("Releasing inventory for booking: {}", booking.getId());
        
        LocalDate currentDate = booking.getStartDate();
        LocalDate endDate = booking.getEndDate();
        UUID listingId = booking.getListing().getId();
        Integer guests = booking.getGuests();
        
        while (!currentDate.isAfter(endDate)) {
            releaseInventoryForDate(listingId, currentDate, guests);
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("Successfully released inventory for booking: {}", booking.getId());
    }
    
    /**
     * Release inventory for a specific date
     */
    private void releaseInventoryForDate(UUID listingId, LocalDate date, Integer guests) {
        var inventoryOpt = inventoryRepository.findByListingIdAndDate(listingId, date);
        
        if (inventoryOpt.isEmpty()) {
            log.warn("No inventory found for listing {} on date {}", listingId, date);
            return;
        }
        
        Inventory inventory = inventoryOpt.get();
        
        // Release the reserved quantity
        int currentReserved = inventory.getReservedQuantity();
        int newReserved = Math.max(0, currentReserved - guests);
        
        inventory.setReservedQuantity(newReserved);
        inventoryRepository.save(inventory);
        
        log.debug("Released {} guests for listing {} on date {}, reserved quantity: {} -> {}", 
                guests, listingId, date, currentReserved, newReserved);
    }
    
    /**
     * Check if inventory is available for booking confirmation
     */
    @Transactional(readOnly = true)
    public boolean isInventoryAvailable(UUID listingId, LocalDate startDate, LocalDate endDate, Integer guests) {
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            if (!isInventoryAvailableForDate(listingId, currentDate, guests)) {
                return false;
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return true;
    }
    
    /**
     * Check if inventory is available for a specific date
     */
    @Transactional(readOnly = true)
    private boolean isInventoryAvailableForDate(UUID listingId, LocalDate date, Integer guests) {
        var inventoryOpt = inventoryRepository.findByListingIdAndDate(listingId, date);
        
        if (inventoryOpt.isEmpty()) {
            log.warn("No inventory found for listing {} on date {}", listingId, date);
            return false;
        }
        
        Inventory inventory = inventoryOpt.get();
        int availableQuantity = inventory.getAvailableQuantity() - inventory.getReservedQuantity();
        
        return availableQuantity >= guests;
    }
    
    /**
     * Get inventory status for a listing and date range
     */
    @Transactional(readOnly = true)
    public List<Inventory> getInventoryForPeriod(UUID listingId, LocalDate startDate, LocalDate endDate) {
        return inventoryRepository.findByListingIdAndDateBetween(listingId, startDate, endDate);
    }
    
    /**
     * Create or update inventory for a specific date
     */
    public Inventory createOrUpdateInventory(UUID listingId, LocalDate date, Integer availableQuantity) {
        var existingInventory = inventoryRepository.findByListingIdAndDate(listingId, date);
        
        Inventory inventory;
        if (existingInventory.isEmpty()) {
            // Fetch the Listing entity
            var listing = listingService.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
            
            inventory = new Inventory();
            inventory.setListing(listing);
            inventory.setDate(date);
            inventory.setAvailableQuantity(availableQuantity);
            inventory.setReservedQuantity(0);
        } else {
            inventory = existingInventory.get();
            inventory.setAvailableQuantity(availableQuantity);
        }
        
        return inventoryRepository.save(inventory);
    }
}