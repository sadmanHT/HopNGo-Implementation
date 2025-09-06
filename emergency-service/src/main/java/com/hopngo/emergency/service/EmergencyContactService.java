package com.hopngo.emergency.service;

import com.hopngo.emergency.dto.*;
import com.hopngo.emergency.entity.EmergencyContact;
import com.hopngo.emergency.repository.EmergencyContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmergencyContactService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmergencyContactService.class);
    private static final int MAX_CONTACTS_PER_USER = 10;
    
    @Autowired
    private EmergencyContactRepository contactRepository;
    
    @Autowired
    private StreamBridge streamBridge;
    
    /**
     * Get all emergency contacts for a user
     */
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getContactsByUserId(String userId) {
        logger.debug("Fetching emergency contacts for user: {}", userId);
        
        List<EmergencyContact> contacts = contactRepository.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId);
        return contacts.stream()
                .map(EmergencyContactResponse::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Create a new emergency contact
     */
    public EmergencyContactResponse createContact(String userId, EmergencyContactRequest request) {
        logger.debug("Creating emergency contact for user: {}", userId);
        
        // Check contact limit
        long contactCount = contactRepository.countByUserId(userId);
        if (contactCount >= MAX_CONTACTS_PER_USER) {
            throw new IllegalArgumentException("Maximum number of emergency contacts (" + MAX_CONTACTS_PER_USER + ") reached");
        }
        
        // If this is set as primary, ensure no other primary contacts exist
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            resetPrimaryContacts(userId);
        }
        
        EmergencyContact contact = new EmergencyContact(
                userId,
                request.getName(),
                request.getPhone(),
                request.getRelation(),
                request.getIsPrimary()
        );
        
        EmergencyContact savedContact = contactRepository.save(contact);
        logger.info("Created emergency contact with ID: {} for user: {}", savedContact.getId(), userId);
        
        return new EmergencyContactResponse(savedContact);
    }
    
    /**
     * Update an existing emergency contact
     */
    public EmergencyContactResponse updateContact(String userId, Long contactId, EmergencyContactRequest request) {
        logger.debug("Updating emergency contact {} for user: {}", contactId, userId);
        
        Optional<EmergencyContact> contactOpt = contactRepository.findByIdAndUserId(contactId, userId);
        if (contactOpt.isEmpty()) {
            throw new IllegalArgumentException("Emergency contact not found or access denied");
        }
        
        EmergencyContact contact = contactOpt.get();
        
        // If this is being set as primary, ensure no other primary contacts exist
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(contact.getIsPrimary())) {
            resetPrimaryContacts(userId);
        }
        
        // Update fields
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setRelation(request.getRelation());
        contact.setIsPrimary(request.getIsPrimary());
        
        EmergencyContact updatedContact = contactRepository.save(contact);
        logger.info("Updated emergency contact with ID: {} for user: {}", contactId, userId);
        
        return new EmergencyContactResponse(updatedContact);
    }
    
    /**
     * Delete an emergency contact
     */
    public void deleteContact(String userId, Long contactId) {
        logger.debug("Deleting emergency contact {} for user: {}", contactId, userId);
        
        Optional<EmergencyContact> contactOpt = contactRepository.findByIdAndUserId(contactId, userId);
        if (contactOpt.isEmpty()) {
            throw new IllegalArgumentException("Emergency contact not found or access denied");
        }
        
        contactRepository.deleteByIdAndUserId(contactId, userId);
        logger.info("Deleted emergency contact with ID: {} for user: {}", contactId, userId);
    }
    
    /**
     * Trigger emergency notification
     */
    public void triggerEmergency(String userId, EmergencyTriggerRequest request) {
        logger.info("Triggering emergency for user: {} at location: {}", userId, request.getLocation());
        
        // Get all contacts for the user
        List<EmergencyContact> contacts = contactRepository.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId);
        
        if (contacts.isEmpty()) {
            logger.warn("No emergency contacts found for user: {}", userId);
            throw new IllegalStateException("No emergency contacts configured. Please add emergency contacts before triggering.");
        }
        
        // Convert to event DTOs
        List<EmergencyTriggeredEvent.EmergencyContactInfo> contactInfos = contacts.stream()
                .map(contact -> new EmergencyTriggeredEvent.EmergencyContactInfo(
                        contact.getName(),
                        contact.getPhone(),
                        contact.getRelation(),
                        contact.getIsPrimary()
                ))
                .collect(Collectors.toList());
        
        // Create location DTO
        EmergencyTriggeredEvent.Location location = new EmergencyTriggeredEvent.Location(
                request.getLocation().getLat(),
                request.getLocation().getLng()
        );
        
        // Create and publish event
        EmergencyTriggeredEvent event = new EmergencyTriggeredEvent(
                userId,
                "User " + userId, // TODO: Get actual user name from user service
                location,
                request.getNote(),
                contactInfos
        );
        
        try {
            streamBridge.send("emergency-triggered", event);
            logger.info("Emergency event published successfully for user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to publish emergency event for user: {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to send emergency notification", e);
        }
    }
    
    /**
     * Reset all primary contacts for a user (set to false)
     */
    private void resetPrimaryContacts(String userId) {
        List<EmergencyContact> primaryContacts = contactRepository.findByUserIdAndIsPrimaryTrue(userId);
        for (EmergencyContact contact : primaryContacts) {
            contact.setIsPrimary(false);
            contactRepository.save(contact);
        }
        logger.debug("Reset {} primary contacts for user: {}", primaryContacts.size(), userId);
    }
}