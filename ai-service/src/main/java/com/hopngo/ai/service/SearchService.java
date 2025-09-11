package com.hopngo.ai.service;

import com.hopngo.ai.dto.SearchRequest;
import com.hopngo.ai.dto.SearchResponse;
import com.hopngo.ai.dto.ImageSearchRequest;
import com.hopngo.ai.dto.EmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    
    public SearchService(EmbeddingService embeddingService, QdrantService qdrantService) {
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }
    
    /**
     * Performs hybrid search combining text matching and semantic similarity
     */
    public Mono<SearchResponse> hybridSearch(SearchRequest request) {
        Instant startTime = Instant.now();
        
        logger.info("Starting hybrid search for query: {}", request.getQuery());
        
        return embeddingService.generateTextEmbedding(request.getQuery(), "default")
            .flatMap(embeddingResponse -> {
                // Perform semantic search using embeddings
                return performSemanticSearch(embeddingResponse.getEmbedding(), request)
                    .map(semanticResults -> {
                        // Combine with text-based search results (placeholder for now)
                        List<SearchResponse.RankedResult> textResults = performTextSearch(request);
                        
                        // Merge and rank results based on semantic weight (using default weight of 0.7)
                        List<SearchResponse.RankedResult> finalResults = mergeSearchResults(
                            semanticResults, textResults, 0.7);
                        
                        // Apply filters and limits
                        finalResults = applyFiltersAndLimits(finalResults, request);
                        
                        Duration processingTime = Duration.between(startTime, Instant.now());
                        
                        SearchResponse response = new SearchResponse();
                        response.setResults(finalResults);
                        response.setTotalResults(finalResults.size());
                        response.setQuery(request.getQuery());
                        response.setProcessingTime(processingTime.toMillis() + "ms");
                        
                        return response;
                    });
            })
            .doOnSuccess(result -> logger.info("Hybrid search completed for query: {} with {} results", 
                request.getQuery(), result.getResults().size()))
            .doOnError(error -> logger.error("Error in hybrid search for query: {}", request.getQuery(), error));
    }
    
    /**
     * Performs visual similarity search using image embeddings
     */
    public Mono<SearchResponse> imageSearch(ImageSearchRequest request) {
        Instant startTime = Instant.now();
        
        logger.info("Starting image search for file: {}", 
            request.getImageFile() != null ? request.getImageFile().getOriginalFilename() : "null");
        
        return embeddingService.generateImageEmbedding(request.getImageFile(), "default")
            .flatMap(embeddingResponse -> {
                return performImageSimilaritySearch(embeddingResponse.getEmbedding(), request)
                    .map(results -> {
                        Duration processingTime = Duration.between(startTime, Instant.now());
                        
                        SearchResponse response = new SearchResponse();
                        response.setResults(results);
                        response.setTotalResults(results.size());
                        response.setQuery("Image Search");
                        response.setProcessingTime(processingTime.toMillis() + "ms");
                        
                        return response;
                    });
            })
            .doOnSuccess(result -> logger.info("Image search completed with {} results", 
                result.getResults().size()))
            .doOnError(error -> logger.error("Error in image search", error));
    }
    
    /**
     * Performs visual similarity search using image URL
     */
    public Mono<SearchResponse> imageSearchByUrl(ImageSearchRequest request) {
        Instant startTime = Instant.now();
        
        logger.info("Starting image search for URL: {}", request.getImageUrl());
        
        return embeddingService.generateImageEmbeddingFromUrl(request.getImageUrl(), "default")
            .flatMap(embeddingResponse -> {
                return performImageSimilaritySearch(embeddingResponse.getEmbedding(), request)
                    .map(results -> {
                        Duration processingTime = Duration.between(startTime, Instant.now());
                        
                        SearchResponse response = new SearchResponse();
                        response.setResults(results);
                        response.setTotalResults(results.size());
                        response.setQuery("Image Search by URL");
                        response.setProcessingTime(processingTime.toMillis() + "ms");
                        
                        return response;
                    });
            })
            .doOnSuccess(result -> logger.info("Image search by URL completed with {} results", 
                result.getResults().size()))
            .doOnError(error -> logger.error("Error in image search by URL: {}", request.getImageUrl(), error));
    }
    
    /**
     * Performs semantic search using Qdrant vector database
     */
    private Mono<List<SearchResponse.RankedResult>> performSemanticSearch(float[] embedding, SearchRequest request) {
        Map<String, Object> filter = new HashMap<>();
        if (request.getCategory() != null) {
            filter.put("category", request.getCategory());
        }
        
        return qdrantService.searchSimilar("posts_vec", embedding, request.getMaxResults(), filter)
            .map(scoredPoints -> 
                scoredPoints.stream()
                    .filter(point -> point.getScore() >= 0.7) // Minimum similarity threshold
                    .map(point -> {
                        SearchResponse.RankedResult result = new SearchResponse.RankedResult();
                        result.setId(point.getId().toString());
                        result.setRelevanceScore(point.getScore());
                        // Extract metadata from payload if available
                        if (point.getPayload() != null) {
                            result.setTitle((String) point.getPayload().get("title"));
                            result.setDescription((String) point.getPayload().get("description"));
                            result.setCategory((String) point.getPayload().get("category"));
                            result.setImageUrl((String) point.getPayload().get("imageUrl"));
                            result.setLocation((String) point.getPayload().get("location"));
                        }
                        return result;
                    })
                    .collect(Collectors.toList())
            );
    }
    
    /**
     * Performs image similarity search using Qdrant vector database
     */
    private Mono<List<SearchResponse.RankedResult>> performImageSimilaritySearch(float[] embedding, ImageSearchRequest request) {
        return qdrantService.searchSimilar("listings_vec", embedding, request.getLimit(), new HashMap<>())
            .map(scoredPoints -> 
                scoredPoints.stream()
                    .filter(point -> point.getScore() >= request.getThreshold())
                    .filter(point -> filterByContentType(point, request.getContentType()))
                    .map(point -> {
                        SearchResponse.RankedResult result = new SearchResponse.RankedResult();
                        result.setId(point.getId().toString());
                        result.setRelevanceScore(point.getScore());
                        // Extract metadata from payload if available
                        if (point.getPayload() != null) {
                            result.setTitle((String) point.getPayload().get("title"));
                            result.setDescription((String) point.getPayload().get("description"));
                            result.setCategory((String) point.getPayload().get("category"));
                            result.setImageUrl((String) point.getPayload().get("imageUrl"));
                            result.setLocation((String) point.getPayload().get("location"));
                        }
                        return result;
                    })
                    .collect(Collectors.toList())
            );
    }
    
    /**
     * Placeholder for text-based search (to be implemented with Elasticsearch or similar)
     */
    private List<SearchResponse.RankedResult> performTextSearch(SearchRequest request) {
        // Placeholder implementation - in real scenario, this would query Elasticsearch
        logger.info("Performing text search for query: {}", request.getQuery());
        return new ArrayList<>(); // Return empty list for now
    }
    
    /**
     * Merges semantic and text search results based on weights
     */
    private List<SearchResponse.RankedResult> mergeSearchResults(
            List<SearchResponse.RankedResult> semanticResults,
            List<SearchResponse.RankedResult> textResults,
            double semanticWeight) {
        
        // For now, just return semantic results with adjusted scores
        // In a full implementation, this would merge and re-rank results
        return semanticResults.stream()
            .peek(result -> result.setRelevanceScore(result.getRelevanceScore() * semanticWeight))
            .collect(Collectors.toList());
    }
    
    /**
     * Applies filters and limits to search results
     */
    private List<SearchResponse.RankedResult> applyFiltersAndLimits(
            List<SearchResponse.RankedResult> results, SearchRequest request) {
        
        return results.stream()
            .filter(result -> filterByContentType(result, request.getCategory()))
             .limit(request.getMaxResults())
            .collect(Collectors.toList());
    }
    
    /**
     * Filters results by content type
     */
    private boolean filterByContentType(Object item, String contentType) {
        if (contentType == null || "all".equals(contentType)) {
            return true;
        }
        
        if (item instanceof SearchResponse.RankedResult) {
            SearchResponse.RankedResult result = (SearchResponse.RankedResult) item;
            return contentType.equals(result.getCategory());
        }
        
        // For Qdrant ScoredPoint, check payload
        return true; // Placeholder - implement based on your data structure
    }
    
    /**
     * Checks if result should be excluded
     */

}