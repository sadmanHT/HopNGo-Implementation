package com.hopngo.support.controller;

import com.hopngo.support.dto.CannedReplyCreateRequest;
import com.hopngo.support.dto.CannedReplyResponse;
import com.hopngo.support.entity.CannedReply;
import com.hopngo.support.service.CannedReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/support/canned-replies")
@PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
@Tag(name = "Canned Replies", description = "Management of pre-written responses for agents")
public class CannedReplyController {

    private final CannedReplyService cannedReplyService;

    @Autowired
    public CannedReplyController(CannedReplyService cannedReplyService) {
        this.cannedReplyService = cannedReplyService;
    }

    @GetMapping
    @Operation(summary = "Get canned replies", description = "Get all canned replies with optional filtering")
    public ResponseEntity<List<CannedReplyResponse>> getCannedReplies(
            @Parameter(description = "Filter by category") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Search query") @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CannedReply> replies;
        
        if (query != null && !query.trim().isEmpty()) {
            // Search canned replies
            replies = cannedReplyService.searchCannedReplies(query.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            // Filter by category
            replies = cannedReplyService.getCannedRepliesByCategory(category.trim(), pageable);
        } else {
            // Get all canned replies
            replies = cannedReplyService.getAllCannedReplies(pageable);
        }
        
        List<CannedReplyResponse> response = replies.getContent().stream()
                .map(CannedReplyResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get canned reply by ID", description = "Get a specific canned reply by its ID")
    public ResponseEntity<CannedReplyResponse> getCannedReply(
            @Parameter(description = "Canned reply ID") @PathVariable Long id) {
        
        CannedReply reply = cannedReplyService.getCannedReplyById(id);
        if (reply == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(CannedReplyResponse.from(reply));
    }

    @PostMapping
    @Operation(summary = "Create canned reply", description = "Create a new canned reply")
    public ResponseEntity<CannedReplyResponse> createCannedReply(
            @Valid @RequestBody CannedReplyCreateRequest request,
            Authentication authentication) {
        
        // Set creator information
        request.setCreatedBy(authentication.getName());
        
        CannedReply reply = cannedReplyService.createCannedReply(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CannedReplyResponse.from(reply));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update canned reply", description = "Update an existing canned reply")
    public ResponseEntity<CannedReplyResponse> updateCannedReply(
            @Parameter(description = "Canned reply ID") @PathVariable Long id,
            @Valid @RequestBody CannedReplyCreateRequest request,
            Authentication authentication) {
        
        CannedReply existingReply = cannedReplyService.getCannedReplyById(id);
        if (existingReply == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user can update this reply (only creator or admin)
        if (!cannedReplyService.canUserModifyReply(existingReply, authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        CannedReply updatedReply = cannedReplyService.updateCannedReply(id, request);
        return ResponseEntity.ok(CannedReplyResponse.from(updatedReply));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete canned reply", description = "Delete a canned reply")
    public ResponseEntity<Void> deleteCannedReply(
            @Parameter(description = "Canned reply ID") @PathVariable Long id,
            Authentication authentication) {
        
        CannedReply existingReply = cannedReplyService.getCannedReplyById(id);
        if (existingReply == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user can delete this reply (only creator or admin)
        if (!cannedReplyService.canUserModifyReply(existingReply, authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        cannedReplyService.deleteCannedReply(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    @Operation(summary = "Get canned reply categories", description = "Get all available canned reply categories")
    public ResponseEntity<List<String>> getCannedReplyCategories() {
        List<String> categories = cannedReplyService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my canned replies", description = "Get canned replies created by the current user")
    public ResponseEntity<List<CannedReplyResponse>> getMyCannedReplies(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CannedReply> replies = cannedReplyService.getCannedRepliesByCreator(authentication.getName(), pageable);
        
        List<CannedReplyResponse> response = replies.getContent().stream()
                .map(CannedReplyResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular canned replies", description = "Get most frequently used canned replies")
    public ResponseEntity<List<CannedReplyResponse>> getPopularCannedReplies(
            @Parameter(description = "Number of replies to return") @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        Page<CannedReply> replies = cannedReplyService.getPopularCannedReplies(pageable);
        
        List<CannedReplyResponse> response = replies.getContent().stream()
                .map(CannedReplyResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/use")
    @Operation(summary = "Mark canned reply as used", description = "Increment usage count for analytics")
    public ResponseEntity<Void> markCannedReplyAsUsed(
            @Parameter(description = "Canned reply ID") @PathVariable Long id) {
        
        cannedReplyService.incrementUsageCount(id);
        return ResponseEntity.ok().build();
    }
}