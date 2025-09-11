package com.hopngo.ai.controller;

import com.hopngo.ai.dto.BulkEmbeddingRequest;
import com.hopngo.ai.dto.BulkEmbeddingResponse;
import com.hopngo.ai.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administrative endpoints for AI service management")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final AdminService adminService;
    
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    @PostMapping("/embeddings/reindex")
    @Operation(
        summary = "Reindex all content embeddings",
        description = "Regenerates embeddings for all content in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reindexing started successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public Mono<ResponseEntity<BulkEmbeddingResponse>> reindexAllEmbeddings(
            @Parameter(description = "Content type to reindex (stay, tour, all)", example = "all")
            @RequestParam(defaultValue = "all") String contentType,
            
            @Parameter(description = "Batch size for processing", example = "100")
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int batchSize,
            
            @Parameter(description = "Force reindex even if embeddings exist")
            @RequestParam(defaultValue = "false") boolean force) {
        
        logger.info("Starting reindex for content type: {} with batch size: {}, force: {}", 
            contentType, batchSize, force);
        
        BulkEmbeddingRequest request = new BulkEmbeddingRequest();
        request.setContentType(contentType);
        request.setBatchSize(batchSize);
        request.setForceReindex(force);
        
        return adminService.reindexAllEmbeddings(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Reindexing completed for content type: {}", contentType))
            .doOnError(error -> logger.error("Error during reindexing for content type: {}", contentType, error));
    }
    
    @PostMapping("/embeddings/backfill")
    @Operation(
        summary = "Backfill embeddings for specific content",
        description = "Generates embeddings for content that doesn't have them yet"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backfill completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public Mono<ResponseEntity<BulkEmbeddingResponse>> backfillEmbeddings(
            @Parameter(description = "Bulk embedding backfill request")
            @Valid @RequestBody BulkEmbeddingRequest request) {
        
        logger.info("Starting embedding backfill for {} items", 
            request.getContentIds() != null ? request.getContentIds().size() : "all");
        
        return adminService.backfillEmbeddings(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Embedding backfill completed successfully"))
            .doOnError(error -> logger.error("Error during embedding backfill", error));
    }
    
    @DeleteMapping("/embeddings/clear")
    @Operation(
        summary = "Clear embeddings from vector database",
        description = "Removes embeddings from Qdrant vector database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Embeddings cleared successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public Mono<ResponseEntity<String>> clearEmbeddings(
            @Parameter(description = "Content type to clear (stay, tour, all)", example = "all")
            @RequestParam(defaultValue = "all") String contentType,
            
            @Parameter(description = "Confirm deletion by setting to true")
            @RequestParam(defaultValue = "false") boolean confirm) {
        
        if (!confirm) {
            return Mono.just(ResponseEntity.badRequest()
                .body("Must set confirm=true to proceed with deletion"));
        }
        
        logger.warn("Clearing embeddings for content type: {}", contentType);
        
        return adminService.clearEmbeddings(contentType)
            .map(count -> ResponseEntity.ok("Cleared " + count + " embeddings for content type: " + contentType))
            .doOnSuccess(result -> logger.info("Successfully cleared embeddings for content type: {}", contentType))
            .doOnError(error -> logger.error("Error clearing embeddings for content type: {}", contentType, error));
    }
    
    @GetMapping("/embeddings/stats")
    @Operation(
        summary = "Get embedding statistics",
        description = "Returns statistics about embeddings in the vector database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getEmbeddingStats() {
        logger.info("Retrieving embedding statistics");
        
        return adminService.getEmbeddingStats()
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully retrieved embedding statistics"))
            .doOnError(error -> logger.error("Error retrieving embedding statistics", error));
    }
    
    @PostMapping("/embeddings/validate")
    @Operation(
        summary = "Validate embedding consistency",
        description = "Checks for inconsistencies between content database and vector database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public Mono<ResponseEntity<Map<String, Object>>> validateEmbeddings(
            @Parameter(description = "Content type to validate (stay, tour, all)", example = "all")
            @RequestParam(defaultValue = "all") String contentType) {
        
        logger.info("Starting embedding validation for content type: {}", contentType);
        
        return adminService.validateEmbeddings(contentType)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Embedding validation completed for content type: {}", contentType))
            .doOnError(error -> logger.error("Error during embedding validation for content type: {}", contentType, error));
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Check admin service health",
        description = "Returns the health status of the admin service"
    )
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Admin service is healthy");
    }
}