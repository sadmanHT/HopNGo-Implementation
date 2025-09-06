package com.hopngo.emergency.controller;

import com.hopngo.emergency.dto.EmergencyContactRequest;
import com.hopngo.emergency.dto.EmergencyContactResponse;
import com.hopngo.emergency.dto.EmergencyTriggerRequest;
import com.hopngo.emergency.service.EmergencyContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/emergency")
@Tag(name = "Emergency", description = "Emergency contacts and notification management")
@Validated
public class EmergencyController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmergencyController.class);
    
    @Autowired
    private EmergencyContactService emergencyContactService;
    
    /**
     * Get all emergency contacts for the current user
     */
    @GetMapping("/contacts")
    @Operation(summary = "Get emergency contacts", description = "Retrieve all emergency contacts for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Emergency contacts retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<List<EmergencyContactResponse>> getContacts(
            @Parameter(description = "User ID from authentication header", required = true)
            @RequestHeader("X-User-Id") String userId) {
        
        logger.debug("GET /emergency/contacts for user: {}", userId);
        
        List<EmergencyContactResponse> contacts = emergencyContactService.getContactsByUserId(userId);
        return ResponseEntity.ok(contacts);
    }
    
    /**
     * Create a new emergency contact
     */
    @PostMapping("/contacts")
    @Operation(summary = "Create emergency contact", description = "Create a new emergency contact for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Emergency contact created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data or contact limit exceeded")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<EmergencyContactResponse> createContact(
            @Parameter(description = "User ID from authentication header", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EmergencyContactRequest request) {
        
        logger.debug("POST /emergency/contacts for user: {}", userId);
        
        try {
            EmergencyContactResponse response = emergencyContactService.createContact(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create emergency contact for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an existing emergency contact
     */
    @PatchMapping("/contacts/{id}")
    @Operation(summary = "Update emergency contact", description = "Update an existing emergency contact")
    @ApiResponse(responseCode = "200", description = "Emergency contact updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "404", description = "Emergency contact not found")
    public ResponseEntity<EmergencyContactResponse> updateContact(
            @Parameter(description = "User ID from authentication header", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Emergency contact ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody EmergencyContactRequest request) {
        
        logger.debug("PATCH /emergency/contacts/{} for user: {}", id, userId);
        
        try {
            EmergencyContactResponse response = emergencyContactService.updateContact(userId, id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update emergency contact {} for user {}: {}", id, userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete an emergency contact
     */
    @DeleteMapping("/contacts/{id}")
    @Operation(summary = "Delete emergency contact", description = "Delete an existing emergency contact")
    @ApiResponse(responseCode = "204", description = "Emergency contact deleted successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "404", description = "Emergency contact not found")
    public ResponseEntity<Void> deleteContact(
            @Parameter(description = "User ID from authentication header", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Emergency contact ID", required = true)
            @PathVariable Long id) {
        
        logger.debug("DELETE /emergency/contacts/{} for user: {}", id, userId);
        
        try {
            emergencyContactService.deleteContact(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete emergency contact {} for user {}: {}", id, userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Trigger emergency notification
     */
    @PostMapping("/trigger")
    @Operation(summary = "Trigger emergency", description = "Trigger emergency notification to all contacts")
    @ApiResponse(responseCode = "200", description = "Emergency notification triggered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data or no contacts configured")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "500", description = "Failed to send emergency notification")
    public ResponseEntity<Map<String, String>> triggerEmergency(
            @Parameter(description = "User ID from authentication header", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EmergencyTriggerRequest request) {
        
        logger.info("POST /emergency/trigger for user: {}", userId);
        
        try {
            emergencyContactService.triggerEmergency(userId, request);
            
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Emergency notification sent to all contacts"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.warn("Failed to trigger emergency for user {}: {}", userId, e.getMessage());
            Map<String, String> errorResponse = Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (RuntimeException e) {
            logger.error("Failed to trigger emergency for user {}: {}", userId, e.getMessage(), e);
            Map<String, String> errorResponse = Map.of(
                    "status", "error",
                    "message", "Failed to send emergency notification"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the emergency service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = Map.of(
                "status", "UP",
                "service", "emergency-service"
        );
        return ResponseEntity.ok(response);
    }
}