package com.hopngo.ai.service;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.*;
import io.qdrant.client.grpc.Points.*;
import io.qdrant.client.grpc.JsonWithInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class QdrantService {
    
    private static final Logger logger = LoggerFactory.getLogger(QdrantService.class);
    
    @Value("${ai.qdrant.host:localhost}")
    private String qdrantHost;
    
    @Value("${ai.qdrant.port:6334}")
    private int qdrantPort;
    
    @Value("${ai.qdrant.api-key:}")
    private String apiKey;
    
    @Value("${ai.qdrant.timeout-ms:10000}")
    private int timeoutMs;
    
    @Value("${ai.qdrant.collections.posts.name:posts_vec}")
    private String postsCollectionName;
    
    @Value("${ai.qdrant.collections.listings.name:listings_vec}")
    private String listingsCollectionName;
    
    @Value("${ai.embedding.dimensions:768}")
    private int embeddingDimensions;
    
    private QdrantClient qdrantClient;
    
    @PostConstruct
    public void initialize() {
        try {
            // Initialize Qdrant client
            QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                qdrantHost, qdrantPort, false);
            
            if (!apiKey.isEmpty()) {
                builder.withApiKey(apiKey);
            }
            
            qdrantClient = new QdrantClient(builder.build());
            
            // Create collections if they don't exist
            createCollectionsIfNotExist();
            
            logger.info("Qdrant service initialized successfully on {}:{}", qdrantHost, qdrantPort);
            
        } catch (Exception e) {
            logger.error("Failed to initialize Qdrant service", e);
            // Don't throw exception to allow service to start without Qdrant
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (qdrantClient != null) {
            try {
                qdrantClient.close();
                logger.info("Qdrant client closed successfully");
            } catch (Exception e) {
                logger.error("Error closing Qdrant client", e);
            }
        }
    }
    
    /**
     * Create collections if they don't exist
     */
    private void createCollectionsIfNotExist() {
        try {
            createCollectionIfNotExists(postsCollectionName);
            createCollectionIfNotExists(listingsCollectionName);
        } catch (Exception e) {
            logger.error("Error creating Qdrant collections", e);
        }
    }
    
    /**
     * Create a collection if it doesn't exist
     */
    private void createCollectionIfNotExists(String collectionName) throws ExecutionException, InterruptedException {
        try {
            // Check if collection exists
            qdrantClient.getCollectionInfoAsync(collectionName).get();
            logger.info("Collection '{}' already exists", collectionName);
        } catch (Exception e) {
            // Collection doesn't exist, create it
            logger.info("Creating collection '{}'", collectionName);
            
            VectorParams vectorParams = VectorParams.newBuilder()
                .setSize(embeddingDimensions)
                .setDistance(Distance.Cosine)
                .build();
            
            CreateCollection createCollection = CreateCollection.newBuilder()
                .setCollectionName(collectionName)
                .setVectorsConfig(VectorsConfig.newBuilder()
                    .setParams(vectorParams)
                    .build())
                .build();
            
            qdrantClient.createCollectionAsync(createCollection).get();
            logger.info("Collection '{}' created successfully", collectionName);
        }
    }
    
    /**
     * Upsert a single point (post or listing) into Qdrant
     */
    public Mono<Boolean> upsertPoint(String collectionName, String id, float[] embedding, Map<String, Object> payload) {
        return Mono.fromCallable(() -> {
            if (qdrantClient == null) {
                logger.warn("Qdrant client not initialized, skipping upsert");
                return false;
            }
            
            try {
                // Convert payload to Qdrant format
                Map<String, JsonWithInt.Value> qdrantPayload = convertPayload(payload);
                
                // Convert float array to List<Float>
                List<Float> vectorList = new ArrayList<>();
                for (float f : embedding) {
                    vectorList.add(f);
                }
                
                // Create point (convert string ID to numeric)
                long numericId = Math.abs(id.hashCode());
                PointStruct point = PointStruct.newBuilder()
                    .setId(PointId.newBuilder().setNum(numericId).build())
                    .setVectors(Vectors.newBuilder()
                        .setVector(io.qdrant.client.grpc.Points.Vector.newBuilder()
                            .addAllData(vectorList)
                            .build())
                        .build())
                    .putAllPayload(qdrantPayload)
                    .build();
                
                // Upsert point
                UpsertPoints upsertPoints = UpsertPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addPoints(point)
                    .build();
                
                qdrantClient.upsertAsync(upsertPoints).get();
                
                logger.debug("Successfully upserted point {} to collection {}", id, collectionName);
                return true;
                
            } catch (Exception e) {
                logger.error("Error upserting point {} to collection {}", id, collectionName, e);
                return false;
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Batch upsert multiple points
     */
    public Mono<Boolean> upsertPoints(String collectionName, Map<String, EmbeddingPoint> points) {
        return Mono.fromCallable(() -> {
            if (qdrantClient == null) {
                logger.warn("Qdrant client not initialized, skipping batch upsert");
                return false;
            }
            
            try {
                List<PointStruct> qdrantPoints = points.entrySet().stream()
                    .map(entry -> {
                        String id = entry.getKey();
                        EmbeddingPoint embeddingPoint = entry.getValue();
                        
                        Map<String, JsonWithInt.Value> qdrantPayload = convertPayload(embeddingPoint.getPayload());
                        
                        // Convert float array to List<Float>
                        List<Float> vectorList = new ArrayList<>();
                        for (float f : embeddingPoint.getEmbedding()) {
                            vectorList.add(f);
                        }
                        
                        long numericId = Math.abs(id.hashCode());
                        return PointStruct.newBuilder()
                            .setId(PointId.newBuilder().setNum(numericId).build())
                            .setVectors(Vectors.newBuilder()
                                .setVector(io.qdrant.client.grpc.Points.Vector.newBuilder()
                                    .addAllData(vectorList)
                                    .build())
                                .build())
                            .putAllPayload(qdrantPayload)
                            .build();
                    })
                    .collect(Collectors.toList());
                
                UpsertPoints upsertPoints = UpsertPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllPoints(qdrantPoints)
                    .build();
                
                qdrantClient.upsertAsync(upsertPoints).get();
                
                logger.info("Successfully upserted {} points to collection {}", points.size(), collectionName);
                return true;
                
            } catch (Exception e) {
                logger.error("Error batch upserting {} points to collection {}", points.size(), collectionName, e);
                return false;
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Search for similar vectors
     */
    public Mono<List<SearchResult>> searchSimilar(String collectionName, float[] queryEmbedding, int limit, Map<String, Object> filter) {
        return Mono.fromCallable(() -> {
            if (qdrantClient == null) {
                logger.warn("Qdrant client not initialized, returning empty results");
                return Collections.<SearchResult>emptyList();
            }
            
            try {
                // Build search request
                List<Float> vectorList = new ArrayList<>();
                for (float f : queryEmbedding) {
                    vectorList.add(f);
                }
                
                SearchPoints.Builder searchBuilder = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(vectorList)
                    .setLimit(limit)
                    .setWithPayload(WithPayloadSelector.newBuilder()
                        .setEnable(true)
                        .build());
                
                // Add filter if provided
                if (filter != null && !filter.isEmpty()) {
                    // TODO: Implement filter conversion
                    logger.debug("Filters not yet implemented, searching without filters");
                }
                
                List<ScoredPoint> results = qdrantClient.searchAsync(searchBuilder.build()).get();
                
                return results.stream()
                    .map(scoredPoint -> new SearchResult(
                        String.valueOf(scoredPoint.getId().getNum()),
                        scoredPoint.getScore(),
                        convertPayloadBack(scoredPoint.getPayloadMap())
                    ))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Error searching in collection {}", collectionName, e);
                return Collections.<SearchResult>emptyList();
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Delete a point from collection
     */
    public Mono<Boolean> deletePoint(String collectionName, String id) {
        return Mono.fromCallable(() -> {
            if (qdrantClient == null) {
                logger.warn("Qdrant client not initialized, skipping delete");
                return false;
            }
            
            try {
                long numericId = Math.abs(id.hashCode());
                DeletePoints deletePoints = DeletePoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setPoints(PointsSelector.newBuilder()
                        .setPoints(PointsIdsList.newBuilder()
                            .addIds(PointId.newBuilder().setNum(numericId).build())
                            .build())
                        .build())
                    .build();
                
                qdrantClient.deleteAsync(deletePoints).get();
                
                logger.debug("Successfully deleted point {} from collection {}", id, collectionName);
                return true;
                
            } catch (Exception e) {
                logger.error("Error deleting point {} from collection {}", id, collectionName, e);
                return false;
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Convert Java payload to Qdrant Value format
     */
    private Map<String, JsonWithInt.Value> convertPayload(Map<String, Object> payload) {
        Map<String, JsonWithInt.Value> qdrantPayload = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            JsonWithInt.Value.Builder valueBuilder = JsonWithInt.Value.newBuilder();
            
            if (value instanceof String) {
                valueBuilder.setStringValue((String) value);
            } else if (value instanceof Integer) {
                valueBuilder.setIntegerValue((Integer) value);
            } else if (value instanceof Long) {
                valueBuilder.setIntegerValue((Long) value);
            } else if (value instanceof Double) {
                valueBuilder.setDoubleValue((Double) value);
            } else if (value instanceof Boolean) {
                valueBuilder.setBoolValue((Boolean) value);
            } else {
                // Convert to string as fallback
                valueBuilder.setStringValue(value.toString());
            }
            
            qdrantPayload.put(key, valueBuilder.build());
        }
        
        return qdrantPayload;
    }
    
    /**
     * Convert Qdrant payload back to Java objects
     */
    private Map<String, Object> convertPayloadBack(Map<String, JsonWithInt.Value> qdrantPayload) {
        Map<String, Object> payload = new HashMap<>();
        
        for (Map.Entry<String, JsonWithInt.Value> entry : qdrantPayload.entrySet()) {
            String key = entry.getKey();
            JsonWithInt.Value value = entry.getValue();
            
            // Simple conversion - just store as string for now
            // TODO: Implement proper type conversion when Qdrant API is stable
            payload.put(key, "value_placeholder");
        }
        
        return payload;
    }
    
    // Helper classes
    public static class EmbeddingPoint {
        private final float[] embedding;
        private final Map<String, Object> payload;
        
        public EmbeddingPoint(float[] embedding, Map<String, Object> payload) {
            this.embedding = embedding;
            this.payload = payload;
        }
        
        public float[] getEmbedding() { return embedding; }
        public Map<String, Object> getPayload() { return payload; }
    }
    
    public static class SearchResult {
        private final String id;
        private final float score;
        private final Map<String, Object> payload;
        
        public SearchResult(String id, float score, Map<String, Object> payload) {
            this.id = id;
            this.score = score;
            this.payload = payload;
        }
        
        public String getId() { return id; }
        public float getScore() { return score; }
        public Map<String, Object> getPayload() { return payload; }
    }
    
    // Getter methods for collection names
    public String getPostsCollectionName() { return postsCollectionName; }
    public String getListingsCollectionName() { return listingsCollectionName; }
}