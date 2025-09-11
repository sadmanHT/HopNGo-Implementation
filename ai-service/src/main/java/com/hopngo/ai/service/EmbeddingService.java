package com.hopngo.ai.service;

import com.hopngo.ai.dto.EmbeddingResponse;
import com.hopngo.ai.dto.BatchEmbeddingRequest;
import com.hopngo.ai.dto.BatchEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Service
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Value("${ai.embedding.dimensions:768}")
    private int embeddingDimensions;
    
    @Value("${ai.embedding.use-cloud:false}")
    private boolean useCloudEmbeddings;
    
    @Value("${ai.embedding.cloud-api-key:}")
    private String cloudApiKey;
    
    /**
     * Generate text embedding using deterministic approach
     */
    public Mono<EmbeddingResponse> generateTextEmbedding(String text, String model) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            float[] embedding;
            if (useCloudEmbeddings && !cloudApiKey.isEmpty()) {
                // TODO: Implement cloud API call (OpenAI, Cohere, etc.)
                embedding = generateCloudTextEmbedding(text, model);
            } else {
                // Use deterministic local embedding
                embedding = generateDeterministicTextEmbedding(text);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            EmbeddingResponse response = new EmbeddingResponse(embedding, model, "text");
            response.setProcessingTimeMs(processingTime);
            
            return response;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(result -> logger.debug("Generated text embedding with {} dimensions in {}ms", 
            result.getDimensions(), result.getProcessingTimeMs()))
        .doOnError(error -> logger.error("Error generating text embedding", error));
    }
    
    /**
     * Generate image embedding from uploaded file
     */
    public Mono<EmbeddingResponse> generateImageEmbedding(MultipartFile imageFile, String model) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            // Validate image file
            if (imageFile.isEmpty()) {
                throw new IllegalArgumentException("Image file is empty");
            }
            
            // Read and process image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
            if (image == null) {
                throw new IllegalArgumentException("Invalid image format");
            }
            
            float[] embedding;
            if (useCloudEmbeddings && !cloudApiKey.isEmpty()) {
                // TODO: Implement cloud API call (CLIP, etc.)
                embedding = generateCloudImageEmbedding(imageFile.getBytes(), model);
            } else {
                // Use deterministic local embedding based on image properties
                embedding = generateDeterministicImageEmbedding(image, imageFile.getOriginalFilename());
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            EmbeddingResponse response = new EmbeddingResponse(embedding, model, "image");
            response.setProcessingTimeMs(processingTime);
            
            return response;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(result -> logger.debug("Generated image embedding with {} dimensions in {}ms", 
            result.getDimensions(), result.getProcessingTimeMs()))
        .doOnError(error -> logger.error("Error generating image embedding", error));
    }
    
    /**
     * Generate image embedding from URL
     */
    public Mono<EmbeddingResponse> generateImageEmbeddingFromUrl(String imageUrl, String model) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            // Download and process image from URL
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            if (image == null) {
                throw new IllegalArgumentException("Cannot read image from URL: " + imageUrl);
            }
            
            float[] embedding;
            if (useCloudEmbeddings && !cloudApiKey.isEmpty()) {
                // TODO: Implement cloud API call
                embedding = generateCloudImageEmbeddingFromUrl(imageUrl, model);
            } else {
                // Use deterministic local embedding
                embedding = generateDeterministicImageEmbedding(image, imageUrl);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            EmbeddingResponse response = new EmbeddingResponse(embedding, model, "image");
            response.setProcessingTimeMs(processingTime);
            
            return response;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(result -> logger.debug("Generated image embedding from URL with {} dimensions in {}ms", 
            result.getDimensions(), result.getProcessingTimeMs()))
        .doOnError(error -> logger.error("Error generating image embedding from URL", error));
    }
    
    /**
     * Generate batch text embeddings
     */
    public Mono<BatchEmbeddingResponse> generateBatchTextEmbeddings(BatchEmbeddingRequest request) {
        return Flux.fromIterable(request.getTexts())
            .flatMap(text -> generateTextEmbedding(text, request.getModel()))
            .collectList()
            .map(embeddings -> new BatchEmbeddingResponse(embeddings, request.getModel()))
            .doOnSuccess(result -> logger.info("Generated {} batch text embeddings", result.getTotalCount()))
            .doOnError(error -> logger.error("Error generating batch text embeddings", error));
    }
    
    /**
     * Generate deterministic text embedding based on text content
     */
    private float[] generateDeterministicTextEmbedding(String text) {
        try {
            // Create hash-based deterministic embedding
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            
            float[] embedding = new float[embeddingDimensions];
            
            // Use text properties to create meaningful dimensions
            int textLength = text.length();
            int wordCount = text.split("\\s+").length;
            int uniqueChars = (int) text.chars().distinct().count();
            
            // Fill embedding with deterministic values based on content
            for (int i = 0; i < embeddingDimensions; i++) {
                int hashIndex = i % hash.length;
                float baseValue = (hash[hashIndex] & 0xFF) / 255.0f;
                
                // Add content-based variations
                if (i < 10) {
                    // First 10 dimensions encode basic text properties
                    switch (i) {
                        case 0: embedding[i] = Math.min(textLength / 1000.0f, 1.0f); break;
                        case 1: embedding[i] = Math.min(wordCount / 100.0f, 1.0f); break;
                        case 2: embedding[i] = Math.min(uniqueChars / 50.0f, 1.0f); break;
                        default: embedding[i] = baseValue;
                    }
                } else {
                    // Remaining dimensions use hash-based values with text influence
                    embedding[i] = baseValue * (1.0f + (textLength % 100) / 1000.0f);
                }
            }
            
            // Normalize the embedding vector
            return normalizeVector(embedding);
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error creating text embedding hash", e);
            return createRandomEmbedding();
        }
    }
    
    /**
     * Generate deterministic image embedding based on image properties
     */
    private float[] generateDeterministicImageEmbedding(BufferedImage image, String identifier) {
        try {
            // Extract image properties
            int width = image.getWidth();
            int height = image.getHeight();
            int type = image.getType();
            
            // Create hash from identifier
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(identifier.getBytes(StandardCharsets.UTF_8));
            
            float[] embedding = new float[embeddingDimensions];
            
            // Sample pixels for color information
            int[] rgbSamples = new int[Math.min(100, width * height)];
            int sampleIndex = 0;
            int stepX = Math.max(1, width / 10);
            int stepY = Math.max(1, height / 10);
            
            for (int y = 0; y < height && sampleIndex < rgbSamples.length; y += stepY) {
                for (int x = 0; x < width && sampleIndex < rgbSamples.length; x += stepX) {
                    rgbSamples[sampleIndex++] = image.getRGB(x, y);
                }
            }
            
            // Fill embedding with image-based values
            for (int i = 0; i < embeddingDimensions; i++) {
                int hashIndex = i % hash.length;
                float baseValue = (hash[hashIndex] & 0xFF) / 255.0f;
                
                if (i < 20) {
                    // First 20 dimensions encode image properties
                    switch (i % 5) {
                        case 0: embedding[i] = Math.min(width / 2000.0f, 1.0f); break;
                        case 1: embedding[i] = Math.min(height / 2000.0f, 1.0f); break;
                        case 2: embedding[i] = type / 10.0f; break;
                        case 3: 
                            if (sampleIndex > 0) {
                                int avgRed = Arrays.stream(rgbSamples, 0, sampleIndex)
                                    .map(rgb -> (rgb >> 16) & 0xFF)
                                    .sum() / sampleIndex;
                                embedding[i] = avgRed / 255.0f;
                            } else {
                                embedding[i] = baseValue;
                            }
                            break;
                        case 4:
                            if (sampleIndex > 0) {
                                int avgBlue = Arrays.stream(rgbSamples, 0, sampleIndex)
                                    .map(rgb -> rgb & 0xFF)
                                    .sum() / sampleIndex;
                                embedding[i] = avgBlue / 255.0f;
                            } else {
                                embedding[i] = baseValue;
                            }
                            break;
                    }
                } else {
                    // Remaining dimensions use hash with image influence
                    embedding[i] = baseValue * (1.0f + (width + height) % 100 / 1000.0f);
                }
            }
            
            return normalizeVector(embedding);
            
        } catch (Exception e) {
            logger.error("Error creating image embedding", e);
            return createRandomEmbedding();
        }
    }
    
    /**
     * Normalize vector to unit length
     */
    private float[] normalizeVector(float[] vector) {
        double sumOfSquares = 0.0;
        for (float v : vector) {
            sumOfSquares += v * v;
        }
        double magnitude = Math.sqrt(sumOfSquares);
        
        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / magnitude);
            }
        }
        
        return vector;
    }
    
    /**
     * Create random embedding as fallback
     */
    private float[] createRandomEmbedding() {
        float[] embedding = new float[embeddingDimensions];
        for (int i = 0; i < embeddingDimensions; i++) {
            embedding[i] = (float) (Math.random() * 2.0 - 1.0); // Range [-1, 1]
        }
        return normalizeVector(embedding);
    }
    
    // Placeholder methods for cloud embeddings (to be implemented later)
    private float[] generateCloudTextEmbedding(String text, String model) {
        // TODO: Implement OpenAI/Cohere API call
        logger.warn("Cloud text embedding not implemented, using deterministic fallback");
        return generateDeterministicTextEmbedding(text);
    }
    
    private float[] generateCloudImageEmbedding(byte[] imageBytes, String model) {
        // TODO: Implement CLIP API call
        logger.warn("Cloud image embedding not implemented, using deterministic fallback");
        return createRandomEmbedding();
    }
    
    private float[] generateCloudImageEmbeddingFromUrl(String imageUrl, String model) {
        // TODO: Implement CLIP API call
        logger.warn("Cloud image embedding from URL not implemented, using deterministic fallback");
        return createRandomEmbedding();
    }
}