package com.hopngo.ai.service;

import com.hopngo.ai.dto.SimilarityRequest;
import com.hopngo.ai.dto.SimilarityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimilarityService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimilarityService.class);
    
    private final WebClient webClient;
    
    // Mock data for demonstration - in production, this would come from ML models
    private static final List<String> SAMPLE_STAYS = Arrays.asList(
        "stay_1", "stay_2", "stay_3", "stay_4", "stay_5", "stay_6", "stay_7", "stay_8", "stay_9", "stay_10"
    );
    
    private static final List<String> SAMPLE_TOURS = Arrays.asList(
        "tour_1", "tour_2", "tour_3", "tour_4", "tour_5", "tour_6", "tour_7", "tour_8", "tour_9", "tour_10"
    );
    
    private static final List<String> SAMPLE_USERS = Arrays.asList(
        "user_1", "user_2", "user_3", "user_4", "user_5", "user_6", "user_7", "user_8", "user_9", "user_10"
    );
    
    public SimilarityService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    @Cacheable(value = "user-similarity", key = "#request.userId + '_' + #request.limit")
    public Mono<SimilarityResponse> findSimilarUsers(SimilarityRequest request) {
        logger.info("Finding similar users for user: {}", request.getUserId());
        
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            // Mock algorithm - in production, this would use collaborative filtering
            List<SimilarityResponse.SimilarItem> similarUsers = generateSimilarUsers(request);
            
            SimilarityResponse response = new SimilarityResponse(similarUsers, "collaborative_filtering");
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.setCacheStatus("miss");
            response.setMetadata(Map.of(
                "algorithm_version", "1.0",
                "user_profile_completeness", Math.random() * 100,
                "total_candidates", SAMPLE_USERS.size()
            ));
            
            return response;
        });
    }
    
    @Cacheable(value = "item-similarity", key = "#request.itemId + '_' + #request.itemType + '_' + #request.limit")
    public Mono<SimilarityResponse> findSimilarItems(SimilarityRequest request) {
        logger.info("Finding similar items for item: {} of type: {}", request.getItemId(), request.getItemType());
        
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            // Mock algorithm - in production, this would use content-based filtering
            List<SimilarityResponse.SimilarItem> similarItems = generateSimilarItems(request);
            
            SimilarityResponse response = new SimilarityResponse(similarItems, "content_based_filtering");
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.setCacheStatus("miss");
            response.setMetadata(Map.of(
                "algorithm_version", "1.0",
                "item_features_count", 15,
                "total_candidates", "stay".equals(request.getItemType()) ? SAMPLE_STAYS.size() : SAMPLE_TOURS.size()
            ));
            
            return response;
        });
    }
    
    @Cacheable(value = "home-recommendations", key = "#request.userId + '_' + #request.limit + '_' + (#request.location != null ? #request.location : 'no_location')")
    public Mono<SimilarityResponse> getHomeRecommendations(SimilarityRequest request) {
        logger.info("Getting home recommendations for user: {}", request.getUserId());
        
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            // Mock hybrid algorithm - combines collaborative and content-based filtering
            List<SimilarityResponse.SimilarItem> recommendations = generateHomeRecommendations(request);
            
            SimilarityResponse response = new SimilarityResponse(recommendations, "hybrid_recommendation");
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.setCacheStatus("miss");
            response.setMetadata(Map.of(
                "algorithm_version", "1.0",
                "personalization_score", Math.random() * 100,
                "location_boost", request.getLocation() != null,
                "total_candidates", SAMPLE_STAYS.size() + SAMPLE_TOURS.size()
            ));
            
            return response;
        });
    }
    
    private List<SimilarityResponse.SimilarItem> generateSimilarUsers(SimilarityRequest request) {
        List<String> candidates = new ArrayList<>(SAMPLE_USERS);
        candidates.remove(request.getUserId()); // Don't recommend the user to themselves
        
        if (request.getExcludeIds() != null) {
            candidates.removeAll(request.getExcludeIds());
        }
        
        Collections.shuffle(candidates);
        
        return candidates.stream()
            .limit(request.getLimit())
            .map(userId -> {
                SimilarityResponse.SimilarItem user = new SimilarityResponse.SimilarItem();
                user.setId(userId);
                user.setType("user");
                user.setTitle("Travel Buddy " + userId.substring(userId.length() - 1));
                user.setDescription("Loves adventure and exploring new places");
                user.setImageUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId);
                user.setLocation(generateRandomLocation());
                user.setSimilarityScore(0.6 + Math.random() * 0.4); // 0.6-1.0
                user.setReason("Similar travel preferences and destinations");
                user.setAttributes(Map.of(
                    "mutual_friends", (int)(Math.random() * 10),
                    "common_destinations", (int)(Math.random() * 5) + 1,
                    "travel_style_match", Math.random() * 100
                ));
                return user;
            })
            .collect(Collectors.toList());
    }
    
    private List<SimilarityResponse.SimilarItem> generateSimilarItems(SimilarityRequest request) {
        List<String> candidates = "stay".equals(request.getItemType()) ? 
            new ArrayList<>(SAMPLE_STAYS) : new ArrayList<>(SAMPLE_TOURS);
        
        candidates.remove(request.getItemId()); // Don't recommend the same item
        
        if (request.getExcludeIds() != null) {
            candidates.removeAll(request.getExcludeIds());
        }
        
        Collections.shuffle(candidates);
        
        return candidates.stream()
            .limit(request.getLimit())
            .map(itemId -> {
                SimilarityResponse.SimilarItem item = new SimilarityResponse.SimilarItem();
                item.setId(itemId);
                item.setType(request.getItemType());
                
                if ("stay".equals(request.getItemType())) {
                    item.setTitle("Cozy " + (Math.random() > 0.5 ? "Apartment" : "Villa") + " in " + generateRandomCity());
                    item.setDescription("Beautiful accommodation with modern amenities");
                    item.setPrice(50.0 + Math.random() * 200);
                } else {
                    item.setTitle(generateRandomTourTitle());
                    item.setDescription("Exciting tour experience with local guides");
                    item.setPrice(30.0 + Math.random() * 150);
                }
                
                item.setImageUrl("https://picsum.photos/400/300?random=" + itemId.hashCode());
                item.setLocation(generateRandomLocation());
                item.setRating(3.5 + Math.random() * 1.5);
                item.setReviewCount((int)(Math.random() * 100) + 10);
                item.setSimilarityScore(0.5 + Math.random() * 0.5); // 0.5-1.0
                item.setReason("Similar style and amenities");
                item.setAttributes(Map.of(
                    "feature_similarity", Math.random() * 100,
                    "price_range_match", Math.random() > 0.3,
                    "location_proximity", Math.random() * 10
                ));
                
                return item;
            })
            .collect(Collectors.toList());
    }
    
    private List<SimilarityResponse.SimilarItem> generateHomeRecommendations(SimilarityRequest request) {
        List<SimilarityResponse.SimilarItem> recommendations = new ArrayList<>();
        
        // Mix of stays and tours
        int stayCount = request.getLimit() / 2;
        int tourCount = request.getLimit() - stayCount;
        
        // Generate stay recommendations
        SimilarityRequest stayRequest = new SimilarityRequest(request.getUserId(), stayCount);
        stayRequest.setItemType("stay");
        stayRequest.setExcludeIds(request.getExcludeIds());
        recommendations.addAll(generateSimilarItems(stayRequest));
        
        // Generate tour recommendations
        SimilarityRequest tourRequest = new SimilarityRequest(request.getUserId(), tourCount);
        tourRequest.setItemType("tour");
        tourRequest.setExcludeIds(request.getExcludeIds());
        recommendations.addAll(generateSimilarItems(tourRequest));
        
        // Shuffle and apply location boost if provided
        Collections.shuffle(recommendations);
        
        if (request.getLocation() != null) {
            // Boost similarity scores for items near user's location
            recommendations.forEach(item -> {
                if (Math.random() > 0.7) { // 30% chance of location boost
                    item.setSimilarityScore(Math.min(item.getSimilarityScore() + 0.1, 1.0));
                    item.setReason("Popular in your area");
                }
            });
        }
        
        // Sort by similarity score
        recommendations.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        
        return recommendations;
    }
    
    private String generateRandomLocation() {
        String[] cities = {"Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Krabi", "Koh Samui"};
        return cities[(int)(Math.random() * cities.length)] + ", Thailand";
    }
    
    private String generateRandomCity() {
        String[] cities = {"Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Krabi", "Koh Samui"};
        return cities[(int)(Math.random() * cities.length)];
    }
    
    private String generateRandomTourTitle() {
        String[] activities = {"Temple", "Food", "Adventure", "Cultural", "Nature", "City"};
        String[] types = {"Tour", "Experience", "Journey", "Adventure", "Discovery"};
        return activities[(int)(Math.random() * activities.length)] + " " + 
               types[(int)(Math.random() * types.length)];
    }
}