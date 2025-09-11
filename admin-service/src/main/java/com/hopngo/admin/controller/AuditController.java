package com.hopngo.admin.controller;

import com.hopngo.admin.dto.AdminAuditResponse;
import com.hopngo.admin.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/admin/audit")
@Tag(name = "Admin Audit", description = "Admin audit log endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    
    private final AdminAuditService auditService;
    
    public AuditController(AdminAuditService auditService) {
        this.auditService = auditService;
    }
    
    @GetMapping
    @Operation(
        summary = "Get audit log",
        description = "Retrieve paginated audit log with optional filters"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit log")
    public ResponseEntity<Page<AdminAuditResponse>> getAuditLog(
            @Parameter(description = "Filter by actor user ID")
            @RequestParam(required = false) String actorUserId,
            
            @Parameter(description = "Filter by target type")
            @RequestParam(required = false) String targetType,
            
            @Parameter(description = "Filter by target ID")
            @RequestParam(required = false) String targetId,
            
            @Parameter(description = "Filter by action")
            @RequestParam(required = false) String action,
            
            @Parameter(description = "Filter by start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "Filter by end date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        
        Page<AdminAuditResponse> auditLog = auditService.getAuditLog(
            actorUserId, targetType, targetId, action, 
            startDate != null ? startDate.toInstant(ZoneOffset.UTC) : null,
            endDate != null ? endDate.toInstant(ZoneOffset.UTC) : null,
            pageable
        );
        
        return ResponseEntity.ok(auditLog);
    }
    
    @GetMapping("/target/{targetType}/{targetId}")
    @Operation(
        summary = "Get recent audit entries for target",
        description = "Retrieve recent audit entries for a specific target"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved target audit entries")
    public ResponseEntity<List<AdminAuditResponse>> getRecentAuditsByTarget(
            @Parameter(description = "Target type")
            @PathVariable String targetType,
            
            @Parameter(description = "Target ID")
            @PathVariable String targetId,
            
            @Parameter(description = "Maximum number of entries to return")
            @RequestParam(defaultValue = "10") int limit) {
        
        List<AdminAuditResponse> auditEntries = auditService.getRecentAuditsByTarget(
            targetType, targetId, limit
        );
        
        return ResponseEntity.ok(auditEntries);
    }
    
    @GetMapping("/stats/actor/{actorUserId}")
    @Operation(
        summary = "Get audit count by actor",
        description = "Get count of audit entries for a specific actor within date range"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved actor audit count")
    public ResponseEntity<Long> getAuditCountByActor(
            @Parameter(description = "Actor user ID")
            @PathVariable String actorUserId,
            
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        long count = auditService.countAuditsByActor(actorUserId, startDate.toInstant(ZoneOffset.UTC), endDate.toInstant(ZoneOffset.UTC));
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/action/{action}")
    @Operation(
        summary = "Get audit count by action",
        description = "Get count of audit entries for a specific action within date range"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved action audit count")
    public ResponseEntity<Long> getAuditCountByAction(
            @Parameter(description = "Action name")
            @PathVariable String action,
            
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        long count = auditService.countAuditsByAction(action, startDate.toInstant(ZoneOffset.UTC), endDate.toInstant(ZoneOffset.UTC));
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/target/{targetType}/{targetId}")
    @Operation(
        summary = "Get audit count by target",
        description = "Get count of audit entries for a specific target within date range"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved target audit count")
    public ResponseEntity<Long> getAuditCountByTarget(
            @Parameter(description = "Target type")
            @PathVariable String targetType,
            
            @Parameter(description = "Target ID")
            @PathVariable String targetId,
            
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        long count = auditService.countAuditsByTarget(targetType, targetId, startDate.toInstant(ZoneOffset.UTC), endDate.toInstant(ZoneOffset.UTC));
        return ResponseEntity.ok(count);
    }
}