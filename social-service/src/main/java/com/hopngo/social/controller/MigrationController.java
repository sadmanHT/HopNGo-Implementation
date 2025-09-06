package com.hopngo.social.controller;

import com.hopngo.social.service.GeohashMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/social/migration")
@Tag(name = "Migration", description = "Data migration endpoints")
public class MigrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);
    
    @Autowired
    private GeohashMigrationService migrationService;
    
    @PostMapping("/geohash/backfill")
    @Operation(summary = "Backfill geohashes", description = "Generate geohashes for existing posts that don't have them")
    public ResponseEntity<Map<String, Object>> backfillGeohashes(
            @Parameter(description = "Batch size for processing") 
            @RequestParam(defaultValue = "100") int batchSize) {
        
        try {
            logger.info("Starting geohash backfill with batch size: {}", batchSize);
            
            migrationService.backfillGeohashes();
            long processedCount = 0; // Method doesn't return count
            
            logger.info("Geohash backfill completed. Processed {} posts", processedCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Geohash backfill completed successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error during geohash backfill", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/geohash/regenerate")
    @Operation(summary = "Regenerate geohashes", description = "Regenerate geohashes for all posts with specified precision")
    public ResponseEntity<Map<String, Object>> regenerateGeohashes(
            @Parameter(description = "Geohash precision (5-7)") 
            @RequestParam(defaultValue = "6") int precision,
            @Parameter(description = "Batch size for processing") 
            @RequestParam(defaultValue = "100") int batchSize) {
        
        try {
            if (precision < 5 || precision > 7) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Precision must be between 5 and 7"
                ));
            }
            
            logger.info("Starting geohash regeneration with precision: {} and batch size: {}", precision, batchSize);
            
            long processedCount = migrationService.regenerateGeohashes(precision);
            
            logger.info("Geohash regeneration completed. Processed {} posts", processedCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "processedCount", processedCount,
                "precision", precision,
                "message", "Geohash regeneration completed successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error during geohash regeneration", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}