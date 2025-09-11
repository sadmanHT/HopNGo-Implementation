package com.hopngo.booking.controller;

import com.hopngo.booking.dto.ReviewFlagCreateRequest;
import com.hopngo.booking.dto.ReviewFlagResolveRequest;
import com.hopngo.booking.dto.ReviewFlagDto;
import com.hopngo.booking.entity.ReviewFlagStatus;
import com.hopngo.booking.service.ReviewFlagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/reviews")
public class ReviewFlagController {
    
    private final ReviewFlagService reviewFlagService;
    
    @Autowired
    public ReviewFlagController(ReviewFlagService reviewFlagService) {
        this.reviewFlagService = reviewFlagService;
    }
    
    @PostMapping("/{reviewId}/flag")
    public ResponseEntity<ReviewFlagDto> flagReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewFlagCreateRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        ReviewFlagDto response = reviewFlagService.createReviewFlag(
            reviewId, request, userId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/flags/{flagId}/resolve")
    public ResponseEntity<ReviewFlagDto> resolveReviewFlag(
            @PathVariable UUID flagId,
            @Valid @RequestBody ReviewFlagResolveRequest request,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        ReviewFlagDto response = reviewFlagService.resolveReviewFlag(
            flagId, request, userId, userRole
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{reviewId}/flags")
    public ResponseEntity<List<ReviewFlagDto>> getReviewFlags(
            @PathVariable UUID reviewId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        List<ReviewFlagDto> flags = reviewFlagService.findByReviewId(reviewId, userId, userRole);
        return ResponseEntity.ok(flags);
    }
    
    @GetMapping("/flags")
    public ResponseEntity<List<ReviewFlagDto>> getAllReviewFlags(
            @RequestParam(required = false) ReviewFlagStatus status,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        List<ReviewFlagDto> flags;
        if (status != null) {
            flags = reviewFlagService.findByStatus(status, userId, userRole);
        } else {
            flags = reviewFlagService.findAllAccessible(userId, userRole);
        }
        
        return ResponseEntity.ok(flags);
    }
    
    @GetMapping("/flags/{flagId}")
    public ResponseEntity<ReviewFlagDto> getReviewFlag(
            @PathVariable UUID flagId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        ReviewFlagDto flag = reviewFlagService.findById(flagId, userId, userRole);
        return ResponseEntity.ok(flag);
    }
    
    @GetMapping("/flags/stats")
    public ResponseEntity<Map<String, Object>> getReviewFlagStats(
            @RequestHeader("X-User-Role") String userRole) {
        
        // Only admins can view stats
        if (!"ADMIN".equals(userRole)) {
            throw new SecurityException("Only admins can view review flag statistics");
        }
        
        // TODO: Implement statistics method in ReviewFlagService
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("message", "Statistics not yet implemented");
        return ResponseEntity.ok(stats);
    }
}