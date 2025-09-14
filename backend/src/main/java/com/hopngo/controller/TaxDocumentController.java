package com.hopngo.controller;

import com.hopngo.entity.TaxDocument;
import com.hopngo.service.TaxDocumentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for tax document management
 */
@RestController
@RequestMapping("/api/v1/tax-documents")
@Tag(name = "Tax Documents", description = "Tax document generation and management API")
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaxDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(TaxDocumentController.class);

    @Autowired
    private TaxDocumentationService taxDocumentationService;

    /**
     * Get all tax documents with optional filtering
     */
    @GetMapping
    @Operation(summary = "Get tax documents", description = "Retrieve tax documents with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tax documents retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getTaxDocuments(
            @Parameter(description = "Jurisdiction code (e.g., BD, US, EU)")
            @RequestParam(required = false) String jurisdiction,
            
            @Parameter(description = "Tax year")
            @RequestParam(required = false) 
            @Min(value = 2020, message = "Tax year must be 2020 or later")
            @Max(value = 2030, message = "Tax year must be 2030 or earlier")
            Integer taxYear,
            
            @Parameter(description = "Document type")
            @RequestParam(required = false) TaxDocument.DocumentType documentType,
            
            @Parameter(description = "Document status")
            @RequestParam(required = false) TaxDocument.DocumentStatus status,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") 
            @Min(value = 0, message = "Page number must be non-negative")
            int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") 
            @Min(value = 1, message = "Page size must be positive")
            @Max(value = 100, message = "Page size must not exceed 100")
            int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            logger.info("Retrieving tax documents - jurisdiction: {}, year: {}, type: {}, status: {}", 
                jurisdiction, taxYear, documentType, status);
            
            // Create sort object
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Apply filters and get documents
            List<TaxDocument> documents = getFilteredDocuments(jurisdiction, taxYear, documentType, status);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("documents", documents);
            response.put("totalElements", documents.size());
            response.put("totalPages", (documents.size() + size - 1) / size);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving tax documents", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve tax documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get tax document by ID
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "Get tax document by ID", description = "Retrieve a specific tax document by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tax document found"),
        @ApiResponse(responseCode = "404", description = "Tax document not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    public ResponseEntity<TaxDocument> getTaxDocumentById(
            @Parameter(description = "Tax document ID", required = true)
            @PathVariable Long documentId) {
        
        try {
            logger.info("Retrieving tax document by ID: {}", documentId);
            
            Optional<TaxDocument> document = taxDocumentationService.getTaxDocumentById(documentId);
            
            if (document.isPresent()) {
                return ResponseEntity.ok(document.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving tax document by ID: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate tax documents for a specific jurisdiction and year
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate tax documents", description = "Generate tax documents for a specific jurisdiction and year")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tax documents generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Generation failed")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    public ResponseEntity<Map<String, Object>> generateTaxDocuments(
            @Parameter(description = "Jurisdiction code", required = true)
            @RequestParam String jurisdiction,
            
            @Parameter(description = "Tax year", required = true)
            @RequestParam 
            @Min(value = 2020, message = "Tax year must be 2020 or later")
            @Max(value = 2030, message = "Tax year must be 2030 or earlier")
            Integer taxYear) {
        
        try {
            logger.info("Generating tax documents for jurisdiction: {} year: {}", jurisdiction, taxYear);
            
            List<TaxDocument> documents = taxDocumentationService.generateTaxDocumentsForJurisdiction(jurisdiction, taxYear);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tax documents generated successfully");
            response.put("jurisdiction", jurisdiction);
            response.put("taxYear", taxYear);
            response.put("documentsGenerated", documents.size());
            response.put("documents", documents);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid jurisdiction: {}", jurisdiction, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid jurisdiction");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error generating tax documents for {} ({})", jurisdiction, taxYear, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate tax documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Download tax document content
     */
    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download tax document", description = "Download the content of a tax document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found or has no content"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    public ResponseEntity<Resource> downloadTaxDocument(
            @Parameter(description = "Tax document ID", required = true)
            @PathVariable Long documentId) {
        
        try {
            logger.info("Downloading tax document: {}", documentId);
            
            Optional<TaxDocument> documentOpt = taxDocumentationService.getTaxDocumentById(documentId);
            
            if (!documentOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            TaxDocument document = documentOpt.get();
            
            if (!document.hasContent()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] content = taxDocumentationService.exportTaxDocumentContent(documentId);
            ByteArrayResource resource = new ByteArrayResource(content);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, document.getContentType());
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length));
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(content.length)
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .body(resource);
            
        } catch (IOException e) {
            logger.error("Error downloading tax document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error downloading tax document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Regenerate a tax document
     */
    @PostMapping("/{documentId}/regenerate")
    @Operation(summary = "Regenerate tax document", description = "Regenerate a specific tax document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document regenerated successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Regeneration failed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> regenerateTaxDocument(
            @Parameter(description = "Tax document ID", required = true)
            @PathVariable Long documentId) {
        
        try {
            logger.info("Regenerating tax document: {}", documentId);
            
            TaxDocument document = taxDocumentationService.regenerateTaxDocument(documentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tax document regenerated successfully");
            response.put("document", document);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Tax document not found: {}", documentId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Tax document not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Error regenerating tax document: {}", documentId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to regenerate tax document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get pending tax documents
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending tax documents", description = "Retrieve tax documents that are in draft or failed status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending documents retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    public ResponseEntity<List<TaxDocument>> getPendingTaxDocuments() {
        
        try {
            logger.info("Retrieving pending tax documents");
            
            List<TaxDocument> documents = taxDocumentationService.getPendingTaxDocuments();
            
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            logger.error("Error retrieving pending tax documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get tax document statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get tax document statistics", description = "Retrieve statistics about tax documents")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getTaxDocumentStatistics(
            @Parameter(description = "Jurisdiction code")
            @RequestParam(required = false) String jurisdiction,
            
            @Parameter(description = "Tax year")
            @RequestParam(required = false) Integer taxYear) {
        
        try {
            logger.info("Retrieving tax document statistics - jurisdiction: {}, year: {}", jurisdiction, taxYear);
            
            Map<String, Object> statistics = calculateStatistics(jurisdiction, taxYear);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error retrieving tax document statistics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Approve a tax document
     */
    @PostMapping("/{documentId}/approve")
    @Operation(summary = "Approve tax document", description = "Approve a completed tax document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document approved successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found"),
        @ApiResponse(responseCode = "400", description = "Document cannot be approved"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveTaxDocument(
            @Parameter(description = "Tax document ID", required = true)
            @PathVariable Long documentId,
            
            @Parameter(description = "Approver name", required = true)
            @RequestParam String approvedBy) {
        
        try {
            logger.info("Approving tax document: {} by: {}", documentId, approvedBy);
            
            Optional<TaxDocument> documentOpt = taxDocumentationService.getTaxDocumentById(documentId);
            
            if (!documentOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            TaxDocument document = documentOpt.get();
            
            if (document.getStatus() != TaxDocument.DocumentStatus.COMPLETED) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Document must be completed before approval");
                errorResponse.put("currentStatus", document.getStatus());
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            document.approve(approvedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tax document approved successfully");
            response.put("document", document);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error approving tax document: {}", documentId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to approve tax document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the tax document service is healthy")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "TaxDocumentController");
        health.put("timestamp", LocalDate.now());
        return ResponseEntity.ok(health);
    }

    // ==================== HELPER METHODS ====================

    private List<TaxDocument> getFilteredDocuments(String jurisdiction, Integer taxYear, 
                                                  TaxDocument.DocumentType documentType, 
                                                  TaxDocument.DocumentStatus status) {
        
        if (jurisdiction != null && taxYear != null) {
            return taxDocumentationService.getTaxDocumentsByJurisdiction(jurisdiction)
                .stream()
                .filter(doc -> doc.getTaxYear().equals(taxYear))
                .filter(doc -> documentType == null || doc.getDocumentType() == documentType)
                .filter(doc -> status == null || doc.getStatus() == status)
                .toList();
        } else if (jurisdiction != null) {
            return taxDocumentationService.getTaxDocumentsByJurisdiction(jurisdiction)
                .stream()
                .filter(doc -> documentType == null || doc.getDocumentType() == documentType)
                .filter(doc -> status == null || doc.getStatus() == status)
                .toList();
        } else if (taxYear != null) {
            return taxDocumentationService.getTaxDocumentsByYear(taxYear)
                .stream()
                .filter(doc -> documentType == null || doc.getDocumentType() == documentType)
                .filter(doc -> status == null || doc.getStatus() == status)
                .toList();
        } else {
            // Return all documents with filters
            return taxDocumentationService.getPendingTaxDocuments()
                .stream()
                .filter(doc -> documentType == null || doc.getDocumentType() == documentType)
                .filter(doc -> status == null || doc.getStatus() == status)
                .toList();
        }
    }

    private Map<String, Object> calculateStatistics(String jurisdiction, Integer taxYear) {
        Map<String, Object> stats = new HashMap<>();
        
        List<TaxDocument> documents;
        
        if (jurisdiction != null && taxYear != null) {
            documents = taxDocumentationService.getTaxDocumentsByJurisdiction(jurisdiction)
                .stream()
                .filter(doc -> doc.getTaxYear().equals(taxYear))
                .toList();
        } else if (jurisdiction != null) {
            documents = taxDocumentationService.getTaxDocumentsByJurisdiction(jurisdiction);
        } else if (taxYear != null) {
            documents = taxDocumentationService.getTaxDocumentsByYear(taxYear);
        } else {
            documents = taxDocumentationService.getPendingTaxDocuments();
        }
        
        // Calculate basic statistics
        stats.put("totalDocuments", documents.size());
        stats.put("completedDocuments", documents.stream().mapToLong(doc -> 
            doc.getStatus() == TaxDocument.DocumentStatus.COMPLETED ? 1 : 0).sum());
        stats.put("approvedDocuments", documents.stream().mapToLong(doc -> 
            doc.getStatus() == TaxDocument.DocumentStatus.APPROVED ? 1 : 0).sum());
        stats.put("pendingDocuments", documents.stream().mapToLong(doc -> 
            doc.getStatus() == TaxDocument.DocumentStatus.DRAFT || 
            doc.getStatus() == TaxDocument.DocumentStatus.FAILED ? 1 : 0).sum());
        
        // Group by document type
        Map<TaxDocument.DocumentType, Long> byType = documents.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                TaxDocument::getDocumentType,
                java.util.stream.Collectors.counting()
            ));
        stats.put("documentsByType", byType);
        
        // Group by status
        Map<TaxDocument.DocumentStatus, Long> byStatus = documents.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                TaxDocument::getStatus,
                java.util.stream.Collectors.counting()
            ));
        stats.put("documentsByStatus", byStatus);
        
        return stats;
    }
}