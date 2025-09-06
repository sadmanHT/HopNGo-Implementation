package com.hopngo.booking.service;

import com.hopngo.booking.entity.Vendor;
import com.hopngo.booking.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class VendorService {
    
    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);
    
    private final VendorRepository vendorRepository;
    private final OutboxService outboxService;
    private final AuthIntegrationService authIntegrationService;
    
    @Autowired
    public VendorService(VendorRepository vendorRepository, 
                        OutboxService outboxService,
                        AuthIntegrationService authIntegrationService) {
        this.vendorRepository = vendorRepository;
        this.outboxService = outboxService;
        this.authIntegrationService = authIntegrationService;
    }
    
    public Vendor createVendor(String userId, String businessName, String contactEmail, 
                              String description, String contactPhone, String address,
                              BigDecimal latitude, BigDecimal longitude) {
        
        // Check if vendor already exists for this user
        if (vendorRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Vendor profile already exists for user: " + userId);
        }
        
        Vendor vendor = new Vendor(userId, businessName, contactEmail);
        vendor.setDescription(description);
        vendor.setContactPhone(contactPhone);
        vendor.setAddress(address);
        vendor.setLatitude(latitude);
        vendor.setLongitude(longitude);
        vendor.setStatus(Vendor.VendorStatus.ACTIVE);
        
        Vendor savedVendor = vendorRepository.save(vendor);
        
        // Publish vendor created event
        outboxService.publishVendorCreatedEvent(savedVendor);
        
        return savedVendor;
    }
    
    @Transactional(readOnly = true)
    public Optional<Vendor> findByUserId(String userId) {
        return vendorRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Vendor> findById(UUID vendorId) {
        return vendorRepository.findById(vendorId);
    }
    
    @Transactional(readOnly = true)
    public List<Vendor> findActiveVendors() {
        return vendorRepository.findByStatus(Vendor.VendorStatus.ACTIVE);
    }
    
    @Transactional(readOnly = true)
    public List<Vendor> searchVendors(String businessName) {
        return vendorRepository.findByStatusAndBusinessNameContaining(
            Vendor.VendorStatus.ACTIVE, businessName);
    }
    
    @Transactional(readOnly = true)
    public List<Vendor> findVendorsNearLocation(Double latitude, Double longitude, Double radiusKm) {
        return vendorRepository.findActiveVendorsWithinRadius(latitude, longitude, radiusKm);
    }
    
    public Vendor updateVendor(UUID vendorId, String businessName, String description,
                              String contactEmail, String contactPhone, String address,
                              BigDecimal latitude, BigDecimal longitude) {
        
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));
        
        if (businessName != null) vendor.setBusinessName(businessName);
        if (description != null) vendor.setDescription(description);
        if (contactEmail != null) vendor.setContactEmail(contactEmail);
        if (contactPhone != null) vendor.setContactPhone(contactPhone);
        if (address != null) vendor.setAddress(address);
        if (latitude != null) vendor.setLatitude(latitude);
        if (longitude != null) vendor.setLongitude(longitude);
        
        Vendor updatedVendor = vendorRepository.save(vendor);
        
        // Publish vendor updated event
        outboxService.publishVendorUpdatedEvent(updatedVendor);
        
        return updatedVendor;
    }
    
    public void suspendVendor(UUID vendorId, String reason) {
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));
        
        vendor.setStatus(Vendor.VendorStatus.SUSPENDED);
        vendorRepository.save(vendor);
        
        // Publish vendor suspended event
        outboxService.publishVendorSuspendedEvent(vendor, reason);
    }
    
    public void activateVendor(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));
        
        vendor.setStatus(Vendor.VendorStatus.ACTIVE);
        vendorRepository.save(vendor);
        
        // Publish vendor activated event
        outboxService.publishVendorActivatedEvent(vendor);
    }
    
    public void deactivateVendor(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));
        
        vendor.setStatus(Vendor.VendorStatus.INACTIVE);
        vendorRepository.save(vendor);
        
        // Publish vendor deactivated event
        outboxService.publishVendorDeactivatedEvent(vendor);
    }
    
    @Transactional(readOnly = true)
    public boolean isVendor(String userId) {
        return vendorRepository.existsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public boolean isActiveVendor(String userId) {
        return vendorRepository.findByUserId(userId)
            .map(vendor -> vendor.getStatus() == Vendor.VendorStatus.ACTIVE)
            .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public void validateVendorOwnership(UUID vendorId, String userId) {
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));
        
        if (!vendor.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this vendor profile");
        }
    }
    
    @Transactional(readOnly = true)
    public void validateProviderRole(String userId) {
        if (!isActiveVendor(userId)) {
            throw new SecurityException("User must be an active vendor to perform this action");
        }
        
        // Check KYC verification status
        if (!authIntegrationService.isVerifiedProvider(userId)) {
            logger.warn("Unverified provider {} attempted to create listing", userId);
            throw new SecurityException("Provider must complete KYC verification to create listings. Please complete your verification process.");
        }
        
        logger.debug("Provider {} validation successful - active vendor and verified", userId);
    }
    
    @Transactional(readOnly = true)
    public long getActiveVendorCount() {
        return vendorRepository.countActiveVendors();
    }
    
    @Transactional(readOnly = true)
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }
}