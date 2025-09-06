package com.hopngo.admin.controller;

import com.hopngo.admin.dto.ModerationDecisionRequest;
import com.hopngo.admin.dto.ModerationItemResponse;
import com.hopngo.admin.entity.ModerationItem.ModerationStatus;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import com.hopngo.admin.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/moderation")
@Tag(name = "Admin Moderation", description = "Admin moderation management endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class ModerationController {
    
    private final ModerationService moderationService;
    
    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }
    
    @GetMapping
    @Operation(
        summary = "Get moderation items",
        description = "Retrieve paginated list of moderation items with optional filters"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved moderation items")
    public ResponseEntity<Page<ModerationItemResponse>> getModerationItems(
            @Parameter(description = "Filter by moderation status")
            @RequestParam(required = false) ModerationStatus status,
            
            @Parameter(description = "Filter by moderation type")
            @RequestParam(required = false) ModerationItemType type,
            
            @Parameter(description = "Filter by assignee user ID")
            @RequestParam(required = false) String assigneeUserId,
            
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        Page<ModerationItemResponse> items = moderationService.getModerationItems(
            status, type, assigneeUserId, pageable
        );
        
        return ResponseEntity.ok(items);
    }
    
    @PostMapping("/{itemId}/approve")
    @Operation(
        summary = "Approve moderation item",
        description = "Approve a pending moderation item"
    )
    @ApiResponse(responseCode = "200", description = "Successfully approved moderation item")
    @ApiResponse(responseCode = "400", description = "Invalid request or item not in pending status")
    @ApiResponse(responseCode = "404", description = "Moderation item not found")
    public ResponseEntity<ModerationItemResponse> approveModerationItem(
            @Parameter(description = "Moderation item ID")
            @PathVariable Long itemId,
            
            @Valid @RequestBody ModerationDecisionRequest request,
            Authentication authentication) {
        
        String adminUserId = authentication.getName();
        ModerationItemResponse response = moderationService.approveModerationItem(itemId, request, adminUserId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{itemId}/reject")
    @Operation(
        summary = "Reject moderation item",
        description = "Reject a pending moderation item"
    )
    @ApiResponse(responseCode = "200", description = "Successfully rejected moderation item")
    @ApiResponse(responseCode = "400", description = "Invalid request or item not in pending status")
    @ApiResponse(responseCode = "404", description = "Moderation item not found")
    public ResponseEntity<ModerationItemResponse> rejectModerationItem(
            @Parameter(description = "Moderation item ID")
            @PathVariable Long itemId,
            
            @Valid @RequestBody ModerationDecisionRequest request,
            Authentication authentication) {
        
        String adminUserId = authentication.getName();
        ModerationItemResponse response = moderationService.rejectModerationItem(itemId, request, adminUserId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{itemId}/remove")
    @Operation(
        summary = "Remove content and mark moderation item",
        description = "Remove the flagged content from the source service and mark moderation item as removed"
    )
    @ApiResponse(responseCode = "200", description = "Successfully removed content")
    @ApiResponse(responseCode = "400", description = "Invalid request or item not in pending status")
    @ApiResponse(responseCode = "404", description = "Moderation item not found")
    @ApiResponse(responseCode = "500", description = "Failed to remove content from source service")
    public ResponseEntity<ModerationItemResponse> removeModerationItem(
            @Parameter(description = "Moderation item ID")
            @PathVariable Long itemId,
            
            @Valid @RequestBody ModerationDecisionRequest request,
            Authentication authentication) {
        
        String adminUserId = authentication.getName();
        ModerationItemResponse response = moderationService.removeModerationItem(itemId, request, adminUserId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/users/{userId}/ban")
    @Operation(
        summary = "Ban user",
        description = "Ban a user account"
    )
    @ApiResponse(responseCode = "200", description = "Successfully banned user")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "500", description = "Failed to ban user in auth service")
    public ResponseEntity<Void> banUser(
            @Parameter(description = "User ID to ban")
            @PathVariable String userId,
            
            @Valid @RequestBody ModerationDecisionRequest request,
            Authentication authentication) {
        
        String adminUserId = authentication.getName();
        moderationService.banUser(userId, request, adminUserId);
        
        return ResponseEntity.ok().build();
    }
}