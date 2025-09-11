package com.hopngo.ai.controller;

import com.hopngo.ai.dto.SearchRequest;
import com.hopngo.ai.dto.SearchResponse;
import com.hopngo.ai.dto.ImageSearchRequest;
import com.hopngo.ai.service.SearchService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Search", description = "AI-powered search services")
@CrossOrigin(origins = "*")
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    private final SearchService searchService;
    
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    @PostMapping("/hybrid-search")
    @Operation(
        summary = "Hybrid text and semantic search",
        description = "Performs hybrid search combining text matching and semantic similarity"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully performed search"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SearchResponse>> hybridSearch(
            @Parameter(description = "Search request with query and filters")
            @Valid @RequestBody SearchRequest request) {
        
        logger.info("Performing hybrid search for query: {} with category: {}", 
            request.getQuery(), request.getCategory());
        
        return searchService.hybridSearch(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully found {} results for query: {}", 
                result.getBody().getResults().size(), request.getQuery()))
            .doOnError(error -> logger.error("Error performing hybrid search for query: {}", 
                request.getQuery(), error));
    }
    
    @PostMapping(value = "/image-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Visual similarity search",
        description = "Finds visually similar content using image embeddings"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully performed image search"),
        @ApiResponse(responseCode = "400", description = "Invalid image file or parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SearchResponse>> imageSearch(
            @Parameter(description = "Image file for visual search")
            @RequestParam("image") MultipartFile imageFile,
            
            @Parameter(description = "Maximum number of results to return", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            
            @Parameter(description = "Minimum similarity threshold (0.0-1.0)", example = "0.7")
            @RequestParam(defaultValue = "0.7") @Min(0) @Max(1) double threshold,
            
            @Parameter(description = "Content type filter (stay, tour, all)", example = "all")
            @RequestParam(defaultValue = "all") String contentType,
            
            @Parameter(description = "User ID for personalization")
            @RequestParam(required = false) String userId) {
        
        logger.info("Performing image search for file: {} (size: {} bytes) with limit: {}", 
            imageFile.getOriginalFilename(), imageFile.getSize(), limit);
        
        ImageSearchRequest request = new ImageSearchRequest();
        request.setImageFile(imageFile);
        request.setLimit(limit);
        request.setThreshold(threshold);
        request.setContentType(contentType);
        request.setUserId(userId);
        
        return searchService.imageSearch(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully found {} visually similar results for image: {}", 
                result.getBody().getResults().size(), imageFile.getOriginalFilename()))
            .doOnError(error -> logger.error("Error performing image search for file: {}", 
                imageFile.getOriginalFilename(), error));
    }
    
    @PostMapping("/image-search/url")
    @Operation(
        summary = "Visual similarity search by URL",
        description = "Finds visually similar content using image URL"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully performed image search"),
        @ApiResponse(responseCode = "400", description = "Invalid image URL or parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SearchResponse>> imageSearchByUrl(
            @Parameter(description = "Image URL for visual search")
            @RequestParam @NotBlank String imageUrl,
            
            @Parameter(description = "Maximum number of results to return", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            
            @Parameter(description = "Minimum similarity threshold (0.0-1.0)", example = "0.7")
            @RequestParam(defaultValue = "0.7") @Min(0) @Max(1) double threshold,
            
            @Parameter(description = "Content type filter (stay, tour, all)", example = "all")
            @RequestParam(defaultValue = "all") String contentType,
            
            @Parameter(description = "User ID for personalization")
            @RequestParam(required = false) String userId) {
        
        logger.info("Performing image search for URL: {} with limit: {}", imageUrl, limit);
        
        ImageSearchRequest request = new ImageSearchRequest();
        request.setImageUrl(imageUrl);
        request.setLimit(limit);
        request.setThreshold(threshold);
        request.setContentType(contentType);
        request.setUserId(userId);
        
        return searchService.imageSearchByUrl(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully found {} visually similar results for URL: {}", 
                result.getBody().getResults().size(), imageUrl))
            .doOnError(error -> logger.error("Error performing image search for URL: {}", imageUrl, error));
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Check search service health",
        description = "Returns the health status of the search service"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Search service is healthy");
    }
}