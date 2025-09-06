package com.hopngo.social.controller;

import com.hopngo.social.dto.SignedUploadRequest;
import com.hopngo.social.dto.SignedUploadResponse;
import com.hopngo.social.entity.MediaMeta;
import com.hopngo.social.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/social/media")
@Tag(name = "Media", description = "Media upload and management APIs")
public class MediaController {
    
    @Autowired
    private MediaService mediaService;
    
    @PostMapping("/sign-upload")
    @Operation(summary = "Generate signed upload URL for Cloudinary")
    public ResponseEntity<?> signUpload(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody SignedUploadRequest request) {
        try {
            SignedUploadResponse response = mediaService.generateSignedUpload(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate signed upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/save-meta")
    @Operation(summary = "Save media metadata after successful upload")
    public ResponseEntity<MediaMeta> saveMediaMeta(
            @RequestHeader("X-User-ID") String userId,
            @RequestBody Map<String, Object> uploadResult) {
        try {
            MediaMeta mediaMeta = mediaService.saveMediaMeta(userId, uploadResult);
            return ResponseEntity.ok(mediaMeta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/quota")
    @Operation(summary = "Get remaining daily upload quota")
    public ResponseEntity<Map<String, Object>> getQuota(
            @RequestHeader("X-User-ID") String userId) {
        long remainingBytes = mediaService.getRemainingQuota(userId);
        double remainingMB = remainingBytes / (1024.0 * 1024.0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("remainingBytes", remainingBytes);
        response.put("remainingMB", Math.round(remainingMB * 100.0) / 100.0);
        response.put("dailyLimitMB", 200);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-media")
    @Operation(summary = "Get user's uploaded media")
    public ResponseEntity<List<MediaMeta>> getUserMedia(
            @RequestHeader("X-User-ID") String userId) {
        List<MediaMeta> media = mediaService.getUserMedia(userId);
        return ResponseEntity.ok(media);
    }
    
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete media by public ID")
    public ResponseEntity<?> deleteMedia(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable String publicId) {
        try {
            mediaService.deleteMedia(publicId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete media: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/transform-url")
    @Operation(summary = "Get transformed media URL")
    public ResponseEntity<Map<String, String>> getTransformedUrl(
            @RequestBody Map<String, Object> request) {
        String publicId = (String) request.get("publicId");
        String resourceType = (String) request.get("resourceType");
        @SuppressWarnings("unchecked")
        Map<String, Object> transformations = (Map<String, Object>) request.get("transformations");
        
        String transformedUrl = mediaService.getTransformedUrl(publicId, resourceType, transformations);
        
        Map<String, String> response = new HashMap<>();
        response.put("url", transformedUrl);
        
        return ResponseEntity.ok(response);
    }
}