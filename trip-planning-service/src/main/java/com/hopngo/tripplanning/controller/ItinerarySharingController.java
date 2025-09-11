package com.hopngo.tripplanning.controller;

import com.hopngo.tripplanning.dto.*;
import com.hopngo.tripplanning.service.ItinerarySharingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = "*")
public class ItinerarySharingController {

    private static final Logger logger = LoggerFactory.getLogger(ItinerarySharingController.class);
    
    private final ItinerarySharingService sharingService;

    @Autowired
    public ItinerarySharingController(ItinerarySharingService sharingService) {
        this.sharingService = sharingService;
    }

    /**
     * Create or update sharing configuration for an itinerary
     * POST /api/trips/{id}/share
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareItinerary(
            @PathVariable UUID id,
            @Valid @RequestBody ShareItineraryRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.info("POST /api/trips/{}/share - User: {}, Visibility: {}", id, userId, request.getVisibility());
        
        try {
            ShareItineraryResponse response = sharingService.shareItinerary(id, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Failed to share itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to share itinerary", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error sharing itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Get shared itinerary by token (public access, no authentication required)
     * GET /api/trips/share/{token}
     */
    @GetMapping("/share/{token}")
    public ResponseEntity<?> getSharedItinerary(@PathVariable String token) {
        
        logger.info("GET /api/trips/share/{}", token);
        
        try {
            Optional<ItineraryResponse> itinerary = sharingService.getSharedItinerary(token);
            
            if (itinerary.isPresent()) {
                return ResponseEntity.ok(itinerary.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Shared itinerary not found", "The shared link is invalid or the itinerary is private"));
            }
        } catch (Exception e) {
            logger.error("Unexpected error fetching shared itinerary with token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Add comment to an itinerary
     * POST /api/trips/{id}/comments
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.info("POST /api/trips/{}/comments - User: {}", id, userId);
        
        try {
            CommentResponse response = sharingService.addComment(id, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Failed to add comment to itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to add comment", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error adding comment to itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Get comments for an itinerary
     * GET /api/trips/{id}/comments
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable UUID id) {
        
        logger.info("GET /api/trips/{}/comments", id);
        
        try {
            List<CommentResponse> comments = sharingService.getComments(id);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            logger.error("Unexpected error fetching comments for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Get version history for an itinerary
     * GET /api/trips/{id}/versions
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getVersionHistory(
            @PathVariable UUID id,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.info("GET /api/trips/{}/versions - User: {}", id, userId);
        
        try {
            List<VersionResponse> versions = sharingService.getVersionHistory(id, userId);
            return ResponseEntity.ok(versions);
        } catch (RuntimeException e) {
            logger.error("Failed to fetch version history for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to fetch version history", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error fetching version history for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Revert itinerary to a specific version
     * POST /api/trips/{id}/revert?version={version}
     */
    @PostMapping("/{id}/revert")
    public ResponseEntity<?> revertToVersion(
            @PathVariable UUID id,
            @RequestParam Integer version,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.info("POST /api/trips/{}/revert?version={} - User: {}", id, version, userId);
        
        try {
            ItineraryResponse response = sharingService.revertToVersion(id, version, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Failed to revert itinerary: {} to version: {}", id, version, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to revert itinerary", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error reverting itinerary: {} to version: {}", id, version, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Remove sharing configuration for an itinerary
     * DELETE /api/trips/{id}/share
     */
    @DeleteMapping("/{id}/share")
    public ResponseEntity<?> removeSharing(
            @PathVariable UUID id,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.info("DELETE /api/trips/{}/share - User: {}", id, userId);
        
        try {
            boolean removed = sharingService.removeSharing(id, userId);
            if (removed) {
                return ResponseEntity.ok(new SuccessResponse("Sharing removed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Sharing not found", "No sharing configuration found for this itinerary"));
            }
        } catch (RuntimeException e) {
            logger.error("Failed to remove sharing for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to remove sharing", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error removing sharing for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Success response DTO
     */
    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}