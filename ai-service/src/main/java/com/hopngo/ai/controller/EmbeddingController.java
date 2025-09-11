package com.hopngo.ai.controller;

import com.hopngo.ai.dto.EmbeddingResponse;
import com.hopngo.ai.dto.BatchEmbeddingRequest;
import com.hopngo.ai.dto.BatchEmbeddingResponse;
import com.hopngo.ai.service.EmbeddingService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/embeddings")
@Tag(name = "Embeddings", description = "AI embedding generation services")
@CrossOrigin(origins = "*")
public class EmbeddingController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingController.class);
    
    private final EmbeddingService embeddingService;
    
    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    @PostMapping("/text")
    @Operation(
        summary = "Generate text embeddings",
        description = "Generates vector embeddings for text content using AI models"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated text embeddings"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<EmbeddingResponse>> generateTextEmbedding(
            @Parameter(description = "Text content to generate embeddings for")
            @RequestParam @NotBlank @Size(max = 8000) String text,
            
            @Parameter(description = "Model to use for embedding generation", example = "text-embedding-ada-002")
            @RequestParam(required = false, defaultValue = "default") String model) {
        
        logger.info("Generating text embedding for text of length: {} using model: {}", text.length(), model);
        
        return embeddingService.generateTextEmbedding(text, model)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully generated text embedding with {} dimensions", 
                result.getBody().getEmbedding().length))
            .doOnError(error -> logger.error("Error generating text embedding", error));
    }
    
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Generate image embeddings",
        description = "Generates vector embeddings for image content using AI models"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated image embeddings"),
        @ApiResponse(responseCode = "400", description = "Invalid image file or parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<EmbeddingResponse>> generateImageEmbedding(
            @Parameter(description = "Image file to generate embeddings for")
            @RequestParam("image") MultipartFile imageFile,
            
            @Parameter(description = "Model to use for embedding generation", example = "clip-vit-base-patch32")
            @RequestParam(required = false, defaultValue = "default") String model) {
        
        logger.info("Generating image embedding for file: {} (size: {} bytes) using model: {}", 
            imageFile.getOriginalFilename(), imageFile.getSize(), model);
        
        return embeddingService.generateImageEmbedding(imageFile, model)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully generated image embedding with {} dimensions", 
                result.getBody().getEmbedding().length))
            .doOnError(error -> logger.error("Error generating image embedding for file: {}", 
                imageFile.getOriginalFilename(), error));
    }
    
    @PostMapping("/image/url")
    @Operation(
        summary = "Generate image embeddings from URL",
        description = "Generates vector embeddings for image from URL using AI models"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated image embeddings"),
        @ApiResponse(responseCode = "400", description = "Invalid image URL or parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<EmbeddingResponse>> generateImageEmbeddingFromUrl(
            @Parameter(description = "Image URL to generate embeddings for")
            @RequestParam @NotBlank String imageUrl,
            
            @Parameter(description = "Model to use for embedding generation", example = "clip-vit-base-patch32")
            @RequestParam(required = false, defaultValue = "default") String model) {
        
        logger.info("Generating image embedding for URL: {} using model: {}", imageUrl, model);
        
        return embeddingService.generateImageEmbeddingFromUrl(imageUrl, model)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully generated image embedding from URL with {} dimensions", 
                result.getBody().getEmbedding().length))
            .doOnError(error -> logger.error("Error generating image embedding from URL: {}", imageUrl, error));
    }
    
    @PostMapping("/batch/text")
    @Operation(
        summary = "Generate batch text embeddings",
        description = "Generates vector embeddings for multiple text inputs in a single request"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated batch text embeddings"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<BatchEmbeddingResponse>> generateBatchTextEmbeddings(
            @Parameter(description = "Batch text embedding request")
            @Valid @RequestBody BatchEmbeddingRequest request) {
        
        logger.info("Generating batch text embeddings for {} texts using model: {}", 
            request.getTexts().size(), request.getModel());
        
        return embeddingService.generateBatchTextEmbeddings(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(result -> logger.info("Successfully generated {} text embeddings", 
                result.getBody().getEmbeddings().size()))
            .doOnError(error -> logger.error("Error generating batch text embeddings", error));
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Check embedding service health",
        description = "Returns the health status of the embedding service"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Embedding service is healthy");
    }
}