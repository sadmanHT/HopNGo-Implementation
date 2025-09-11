package com.hopngo.ai.service;

import com.hopngo.ai.dto.BulkEmbeddingRequest;
import com.hopngo.ai.dto.BulkEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    
    // In-memory job tracking (in production, use Redis or database)
    private final Map<String, BulkEmbeddingResponse> activeJobs = new ConcurrentHashMap<>();
    
    public AdminService(EmbeddingService embeddingService, QdrantService qdrantService) {
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }
    
    /**
     * Reindex all embeddings in the system
     */
    public Mono<BulkEmbeddingResponse> reindexAllEmbeddings(BulkEmbeddingRequest request) {
        String jobId = UUID.randomUUID().toString();
        logger.info("Starting reindex job: {} for content type: {}", jobId, request.getContentType());
        
        // Get total count of items to process
        int totalItems = getTotalItemsCount(request.getContentType());
        
        BulkEmbeddingResponse response = new BulkEmbeddingResponse(jobId, totalItems);
        response.setMessage("Reindexing started for " + totalItems + " items");
        activeJobs.put(jobId, response);
        
        if (request.isAsync()) {
            // Process asynchronously
            processReindexAsync(jobId, request)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    result -> logger.info("Reindex job {} completed successfully", jobId),
                    error -> {
                        logger.error("Reindex job {} failed", jobId, error);
                        response.markFailed("Reindexing failed: " + error.getMessage());
                    }
                );
            
            return Mono.just(response);
        } else {
            // Process synchronously
            return processReindexAsync(jobId, request);
        }
    }
    
    /**
     * Backfill embeddings for content that doesn't have them
     */
    public Mono<BulkEmbeddingResponse> backfillEmbeddings(BulkEmbeddingRequest request) {
        String jobId = UUID.randomUUID().toString();
        logger.info("Starting backfill job: {} for {} items", jobId, 
            request.getContentIds() != null ? request.getContentIds().size() : "all");
        
        // Get items that need embeddings
        List<String> itemsToProcess = getItemsNeedingEmbeddings(request);
        
        BulkEmbeddingResponse response = new BulkEmbeddingResponse(jobId, itemsToProcess.size());
        response.setMessage("Backfill started for " + itemsToProcess.size() + " items");
        activeJobs.put(jobId, response);
        
        if (request.isAsync()) {
            // Process asynchronously
            processBackfillAsync(jobId, request, itemsToProcess)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    result -> logger.info("Backfill job {} completed successfully", jobId),
                    error -> {
                        logger.error("Backfill job {} failed", jobId, error);
                        response.markFailed("Backfill failed: " + error.getMessage());
                    }
                );
            
            return Mono.just(response);
        } else {
            // Process synchronously
            return processBackfillAsync(jobId, request, itemsToProcess);
        }
    }
    
    /**
     * Clear embeddings from vector database
     */
    public Mono<Integer> clearEmbeddings(String contentType) {
        logger.warn("Clearing embeddings for content type: {}", contentType);
        
        // TODO: Implement deleteByFilter in QdrantService
        logger.warn("Clear embeddings not yet implemented - deleteByFilter method needed in QdrantService");
        return Mono.just(0);
    }
    
    /**
     * Get embedding statistics
     */
    public Mono<Map<String, Object>> getEmbeddingStats() {
        logger.info("Retrieving embedding statistics");
        
        // TODO: Implement getCollectionInfo in QdrantService
        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "Collection info not yet implemented");
        stats.put("lastUpdated", LocalDateTime.now());
        
        // Add content type breakdown (placeholder)
        Map<String, Integer> contentTypeBreakdown = new HashMap<>();
        contentTypeBreakdown.put("stays", getContentTypeCount("stay"));
        contentTypeBreakdown.put("tours", getContentTypeCount("tour"));
        stats.put("contentTypeBreakdown", contentTypeBreakdown);
        
        return Mono.just(stats);
    }
    
    /**
     * Validate embedding consistency
     */
    public Mono<Map<String, Object>> validateEmbeddings(String contentType) {
        logger.info("Validating embeddings for content type: {}", contentType);
        
        return Mono.fromCallable(() -> {
            Map<String, Object> validation = new HashMap<>();
            
            // Get content from database (placeholder)
            int totalContentItems = getTotalItemsCount(contentType);
            
            // Get embeddings from vector database (placeholder)
            int totalEmbeddings = getEmbeddingCount(contentType);
            
            validation.put("totalContentItems", totalContentItems);
            validation.put("totalEmbeddings", totalEmbeddings);
            validation.put("missingEmbeddings", Math.max(0, totalContentItems - totalEmbeddings));
            validation.put("orphanedEmbeddings", Math.max(0, totalEmbeddings - totalContentItems));
            validation.put("consistencyPercentage", 
                totalContentItems > 0 ? (double) Math.min(totalContentItems, totalEmbeddings) / totalContentItems * 100 : 0);
            validation.put("validationTime", LocalDateTime.now());
            
            return validation;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            logger.error("Error during embedding validation for content type: {}", contentType, error);
            Map<String, Object> errorValidation = new HashMap<>();
            errorValidation.put("error", "Validation failed: " + error.getMessage());
            return Mono.just(errorValidation);
        });
    }
    
    /**
     * Get job status by ID
     */
    public Mono<BulkEmbeddingResponse> getJobStatus(String jobId) {
        BulkEmbeddingResponse job = activeJobs.get(jobId);
        if (job == null) {
            return Mono.error(new IllegalArgumentException("Job not found: " + jobId));
        }
        return Mono.just(job);
    }
    
    // Private helper methods
    
    private Mono<BulkEmbeddingResponse> processReindexAsync(String jobId, BulkEmbeddingRequest request) {
        return Mono.fromCallable(() -> {
            BulkEmbeddingResponse job = activeJobs.get(jobId);
            job.setStatus("in_progress");
            
            // Simulate processing (replace with actual implementation)
            List<String> allItems = getAllContentIds(request.getContentType());
            AtomicInteger processed = new AtomicInteger(0);
            AtomicInteger successful = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);
            
            // Process in batches
            for (int i = 0; i < allItems.size(); i += request.getBatchSize()) {
                int endIndex = Math.min(i + request.getBatchSize(), allItems.size());
                List<String> batch = allItems.subList(i, endIndex);
                
                try {
                    // Process batch (placeholder - implement actual embedding generation)
                    processBatch(batch, request);
                    successful.addAndGet(batch.size());
                } catch (Exception e) {
                    logger.error("Error processing batch starting at index {}", i, e);
                    failed.addAndGet(batch.size());
                }
                
                processed.addAndGet(batch.size());
                job.setProcessedItems(processed.get());
                job.setSuccessfulItems(successful.get());
                job.setFailedItems(failed.get());
                
                // Small delay to prevent overwhelming the system
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            job.markCompleted();
            job.setMessage("Reindexing completed. Processed: " + processed.get() + 
                          ", Successful: " + successful.get() + ", Failed: " + failed.get());
            
            return job;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<BulkEmbeddingResponse> processBackfillAsync(String jobId, BulkEmbeddingRequest request, List<String> itemsToProcess) {
        return Mono.fromCallable(() -> {
            BulkEmbeddingResponse job = activeJobs.get(jobId);
            job.setStatus("in_progress");
            
            AtomicInteger processed = new AtomicInteger(0);
            AtomicInteger successful = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);
            
            // Process items that need embeddings
            for (int i = 0; i < itemsToProcess.size(); i += request.getBatchSize()) {
                int endIndex = Math.min(i + request.getBatchSize(), itemsToProcess.size());
                List<String> batch = itemsToProcess.subList(i, endIndex);
                
                try {
                    // Process batch (placeholder - implement actual embedding generation)
                    processBatch(batch, request);
                    successful.addAndGet(batch.size());
                } catch (Exception e) {
                    logger.error("Error processing backfill batch starting at index {}", i, e);
                    failed.addAndGet(batch.size());
                }
                
                processed.addAndGet(batch.size());
                job.setProcessedItems(processed.get());
                job.setSuccessfulItems(successful.get());
                job.setFailedItems(failed.get());
                
                // Small delay to prevent overwhelming the system
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            job.markCompleted();
            job.setMessage("Backfill completed. Processed: " + processed.get() + 
                          ", Successful: " + successful.get() + ", Failed: " + failed.get());
            
            return job;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    // Placeholder methods (implement based on your data access layer)
    
    private int getTotalItemsCount(String contentType) {
        // Placeholder - query your content database
        return switch (contentType) {
            case "stay" -> 1000;
            case "tour" -> 500;
            case "all" -> 1500;
            default -> 0;
        };
    }
    
    private int getContentTypeCount(String contentType) {
        // Placeholder - query Qdrant for content type count
        return switch (contentType) {
            case "stay" -> 950;
            case "tour" -> 480;
            default -> 0;
        };
    }
    
    private int getEmbeddingCount(String contentType) {
        // Placeholder - query Qdrant for embedding count
        return getContentTypeCount(contentType);
    }
    
    private List<String> getAllContentIds(String contentType) {
        // Placeholder - query your content database
        List<String> ids = new ArrayList<>();
        int count = getTotalItemsCount(contentType);
        for (int i = 1; i <= count; i++) {
            ids.add(contentType + "_" + i);
        }
        return ids;
    }
    
    private List<String> getItemsNeedingEmbeddings(BulkEmbeddingRequest request) {
        // Placeholder - find items without embeddings
        if (request.getContentIds() != null) {
            return new ArrayList<>(request.getContentIds());
        }
        
        // Return items that don't have embeddings yet
        List<String> allIds = getAllContentIds(request.getContentType());
        return allIds.subList(0, Math.min(50, allIds.size())); // Placeholder
    }
    
    private void processBatch(List<String> batch, BulkEmbeddingRequest request) {
        // Placeholder - implement actual embedding generation and storage
        logger.debug("Processing batch of {} items with model: {}", batch.size(), request.getModel());
        
        // In real implementation:
        // 1. Fetch content for each ID
        // 2. Generate embeddings using embeddingService
        // 3. Store embeddings in Qdrant using qdrantService
    }
    
    private Map<String, Object> createContentTypeFilter(String contentType) {
        // Placeholder - create Qdrant filter for content type
        Map<String, Object> filter = new HashMap<>();
        if (!"all".equals(contentType)) {
            filter.put("content_type", contentType);
        }
        return filter;
    }
}