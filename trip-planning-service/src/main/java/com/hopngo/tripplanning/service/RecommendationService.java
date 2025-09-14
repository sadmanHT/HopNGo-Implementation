package com.hopngo.tripplanning.service;

import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.entity.*;
import com.hopngo.tripplanning.mapper.ItineraryMapper;
import com.hopngo.tripplanning.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    
    private static final BigDecimal MIN_SIMILARITY_THRESHOLD = new BigDecimal("0.3");
    private static final BigDecimal MIN_RECOMMENDATION_SCORE = new BigDecimal("0.5");
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int SIMILAR_USERS_LIMIT = 20;

    private final ItineraryRepository itineraryRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserSimilarityRepository userSimilarityRepository;
    private final ItineraryRatingRepository itineraryRatingRepository;
    private final ItineraryRecommendationRepository recommendationRepository;
    private final ItineraryMapper itineraryMapper;

    public RecommendationService(ItineraryRepository itineraryRepository,
                               UserPreferenceRepository userPreferenceRepository,
                               UserSimilarityRepository userSimilarityRepository,
                               ItineraryRatingRepository itineraryRatingRepository,
                               ItineraryRecommendationRepository recommendationRepository,
                               ItineraryMapper itineraryMapper) {
        this.itineraryRepository = itineraryRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userSimilarityRepository = userSimilarityRepository;
        this.itineraryRatingRepository = itineraryRatingRepository;
        this.recommendationRepository = recommendationRepository;
        this.itineraryMapper = itineraryMapper;
    }

    /**
     * Get personalized itinerary recommendations for a user
     */
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getRecommendationsForUser(String userId, int limit) {
        logger.info("Getting recommendations for user: {}", userId);
        
        // First, try to get cached recommendations
        List<ItineraryRecommendation> cachedRecommendations = recommendationRepository
                .findTopActiveRecommendationsForUser(userId, PageRequest.of(0, limit));
        
        if (!cachedRecommendations.isEmpty()) {
            logger.debug("Found {} cached recommendations for user: {}", cachedRecommendations.size(), userId);
            return cachedRecommendations.stream()
                    .map(rec -> itineraryMapper.toResponse(rec.getRecommendedItinerary()))
                    .collect(Collectors.toList());
        }
        
        // Generate new recommendations
        List<ItineraryRecommendation> newRecommendations = generateRecommendations(userId, limit);
        
        // Cache the recommendations
        recommendationRepository.saveAll(newRecommendations);
        
        return newRecommendations.stream()
                .map(rec -> itineraryMapper.toResponse(rec.getRecommendedItinerary()))
                .collect(Collectors.toList());
    }

    /**
     * Generate new recommendations using collaborative filtering
     */
    private List<ItineraryRecommendation> generateRecommendations(String userId, int limit) {
        logger.info("Generating new recommendations for user: {}", userId);
        
        // Step 1: Find similar users
        List<String> similarUsers = findSimilarUsers(userId);
        logger.debug("Found {} similar users for user: {}", similarUsers.size(), userId);
        
        if (similarUsers.isEmpty()) {
            // Fallback to popular itineraries if no similar users found
            return generatePopularityBasedRecommendations(userId, limit);
        }
        
        // Step 2: Get highly rated itineraries from similar users
        Map<UUID, BigDecimal> candidateItineraries = getCandidateItineraries(similarUsers, userId);
        
        // Step 3: Calculate recommendation scores
        List<ItineraryRecommendation> recommendations = new ArrayList<>();
        
        candidateItineraries.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> {
                    UUID itineraryId = entry.getKey();
                    BigDecimal score = entry.getValue();
                    
                    if (score.compareTo(MIN_RECOMMENDATION_SCORE) >= 0) {
                        Optional<Itinerary> itinerary = itineraryRepository.findById(itineraryId);
                        if (itinerary.isPresent()) {
                            String reason = generateRecommendationReason(userId, itinerary.get(), similarUsers);
                            recommendations.add(new ItineraryRecommendation(
                                    userId, itinerary.get(), score, reason));
                        }
                    }
                });
        
        logger.info("Generated {} recommendations for user: {}", recommendations.size(), userId);
        return recommendations;
    }

    /**
     * Find users similar to the given user
     */
    private List<String> findSimilarUsers(String userId) {
        // First, try to get from cached similarities
        List<String> similarUserIds = userSimilarityRepository
                .findSimilarUserIds(userId, MIN_SIMILARITY_THRESHOLD);
        
        if (!similarUserIds.isEmpty()) {
            return similarUserIds.stream().limit(SIMILAR_USERS_LIMIT).collect(Collectors.toList());
        }
        
        // If no cached similarities, calculate them
        calculateUserSimilarities(userId);
        
        // Try again after calculation
        return userSimilarityRepository
                .findSimilarUserIds(userId, MIN_SIMILARITY_THRESHOLD)
                .stream().limit(SIMILAR_USERS_LIMIT).collect(Collectors.toList());
    }

    /**
     * Calculate user similarities based on preferences and ratings
     */
    public void calculateUserSimilarities(String userId) {
        logger.info("Calculating similarities for user: {}", userId);
        
        List<UserPreference> userPreferences = userPreferenceRepository.findByUserId(userId);
        if (userPreferences.isEmpty()) {
            logger.warn("No preferences found for user: {}", userId);
            return;
        }
        
        // Find users with similar preferences
        List<String> usersWithSimilarPrefs = userPreferenceRepository
                .findUsersWithSimilarPreferences(userId);
        
        for (String otherUserId : usersWithSimilarPrefs) {
            if (!userSimilarityRepository.existsSimilarityBetweenUsers(userId, otherUserId)) {
                BigDecimal similarity = calculateSimilarityScore(userId, otherUserId);
                if (similarity.compareTo(MIN_SIMILARITY_THRESHOLD) >= 0) {
                    UserSimilarity userSimilarity = new UserSimilarity(userId, otherUserId, similarity);
                    userSimilarityRepository.save(userSimilarity);
                }
            }
        }
    }

    /**
     * Calculate similarity score between two users
     */
    private BigDecimal calculateSimilarityScore(String userId1, String userId2) {
        // Get preferences for both users
        List<UserPreference> prefs1 = userPreferenceRepository.findByUserId(userId1);
        List<UserPreference> prefs2 = userPreferenceRepository.findByUserId(userId2);
        
        // Get ratings for both users
        List<ItineraryRating> ratings1 = itineraryRatingRepository.findByUserIdAndInteractionType(userId1, "rated");
        List<ItineraryRating> ratings2 = itineraryRatingRepository.findByUserIdAndInteractionType(userId2, "rated");
        
        // Calculate preference similarity (Jaccard similarity)
        BigDecimal prefSimilarity = calculatePreferenceSimilarity(prefs1, prefs2);
        
        // Calculate rating similarity (Cosine similarity)
        BigDecimal ratingSimilarity = calculateRatingSimilarity(ratings1, ratings2);
        
        // Weighted combination (70% preferences, 30% ratings)
        BigDecimal weightedSimilarity = prefSimilarity.multiply(new BigDecimal("0.7"))
                .add(ratingSimilarity.multiply(new BigDecimal("0.3")));
        
        return weightedSimilarity.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate preference similarity using Jaccard index
     */
    private BigDecimal calculatePreferenceSimilarity(List<UserPreference> prefs1, List<UserPreference> prefs2) {
        Set<String> prefSet1 = prefs1.stream()
                .map(p -> p.getPreferenceType() + ":" + p.getPreferenceValue())
                .collect(Collectors.toSet());
        
        Set<String> prefSet2 = prefs2.stream()
                .map(p -> p.getPreferenceType() + ":" + p.getPreferenceValue())
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(prefSet1);
        intersection.retainAll(prefSet2);
        
        Set<String> union = new HashSet<>(prefSet1);
        union.addAll(prefSet2);
        
        if (union.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return new BigDecimal(intersection.size())
                .divide(new BigDecimal(union.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate rating similarity using Cosine similarity
     */
    private BigDecimal calculateRatingSimilarity(List<ItineraryRating> ratings1, List<ItineraryRating> ratings2) {
        Map<UUID, Integer> ratingMap1 = ratings1.stream()
                .collect(Collectors.toMap(
                        r -> r.getItinerary().getId(),
                        ItineraryRating::getRating,
                        (existing, replacement) -> existing));
        
        Map<UUID, Integer> ratingMap2 = ratings2.stream()
                .collect(Collectors.toMap(
                        r -> r.getItinerary().getId(),
                        ItineraryRating::getRating,
                        (existing, replacement) -> existing));
        
        Set<UUID> commonItineraries = new HashSet<>(ratingMap1.keySet());
        commonItineraries.retainAll(ratingMap2.keySet());
        
        if (commonItineraries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (UUID itineraryId : commonItineraries) {
            int rating1 = ratingMap1.get(itineraryId);
            int rating2 = ratingMap2.get(itineraryId);
            
            dotProduct += rating1 * rating2;
            norm1 += rating1 * rating1;
            norm2 += rating2 * rating2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return BigDecimal.ZERO;
        }
        
        double cosineSimilarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return new BigDecimal(cosineSimilarity).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Get candidate itineraries from similar users
     */
    private Map<UUID, BigDecimal> getCandidateItineraries(List<String> similarUsers, String userId) {
        Map<UUID, BigDecimal> candidates = new HashMap<>();
        
        // Get user's own itineraries to exclude them
        Set<UUID> userItineraries = itineraryRepository.findByUserIdOrderByCreatedAtDesc(
                userId, Pageable.unpaged()).getContent().stream()
                .map(Itinerary::getId)
                .collect(Collectors.toSet());
        
        for (String similarUserId : similarUsers) {
            // Get highly rated itineraries from similar user
            List<UUID> highlyRatedItineraries = itineraryRatingRepository
                    .findHighlyRatedItinerariesByUser(similarUserId);
            
            // Get similarity score with this user
            Optional<UserSimilarity> similarity = userSimilarityRepository
                    .findSimilarityBetweenUsers(userId, similarUserId);
            
            BigDecimal userSimilarityScore = similarity
                    .map(UserSimilarity::getSimilarityScore)
                    .orElse(BigDecimal.ZERO);
            
            for (UUID itineraryId : highlyRatedItineraries) {
                // Skip user's own itineraries
                if (userItineraries.contains(itineraryId)) {
                    continue;
                }
                
                // Calculate weighted score based on user similarity
                BigDecimal currentScore = candidates.getOrDefault(itineraryId, BigDecimal.ZERO);
                BigDecimal newScore = currentScore.add(userSimilarityScore);
                candidates.put(itineraryId, newScore);
            }
        }
        
        return candidates;
    }

    /**
     * Generate popularity-based recommendations as fallback
     */
    private List<ItineraryRecommendation> generatePopularityBasedRecommendations(String userId, int limit) {
        logger.info("Generating popularity-based recommendations for user: {}", userId);
        
        List<Object[]> popularItineraries = itineraryRatingRepository
                .findMostPopularItineraries(PageRequest.of(0, limit * 2));
        
        List<ItineraryRecommendation> recommendations = new ArrayList<>();
        
        for (Object[] result : popularItineraries) {
            UUID itineraryId = (UUID) result[0];
            Long interactionCount = (Long) result[1];
            
            Optional<Itinerary> itinerary = itineraryRepository.findById(itineraryId);
            if (itinerary.isPresent() && !itinerary.get().getUserId().equals(userId)) {
                BigDecimal score = new BigDecimal(Math.min(1.0, interactionCount / 100.0))
                        .setScale(4, RoundingMode.HALF_UP);
                
                if (score.compareTo(MIN_RECOMMENDATION_SCORE) >= 0) {
                    recommendations.add(new ItineraryRecommendation(
                            userId, itinerary.get(), score, "Popular among other travelers"));
                }
            }
            
            if (recommendations.size() >= limit) {
                break;
            }
        }
        
        return recommendations;
    }

    /**
     * Generate recommendation reason
     */
    private String generateRecommendationReason(String userId, Itinerary itinerary, List<String> similarUsers) {
        return String.format("Recommended based on %d similar travelers who enjoyed this itinerary", 
                similarUsers.size());
    }

    /**
     * Record user interaction with an itinerary
     */
    public void recordInteraction(String userId, UUID itineraryId, String interactionType, Integer rating) {
        logger.debug("Recording interaction: user={}, itinerary={}, type={}, rating={}", 
                userId, itineraryId, interactionType, rating);
        
        Optional<Itinerary> itinerary = itineraryRepository.findById(itineraryId);
        if (itinerary.isPresent()) {
            ItineraryRating itineraryRating = new ItineraryRating(
                    userId, itinerary.get(), rating != null ? rating : 3, interactionType);
            itineraryRatingRepository.save(itineraryRating);
            
            // Invalidate cached recommendations for this user
            recommendationRepository.deleteByUserId(userId);
        }
    }

    /**
     * Update user preferences based on itinerary interaction
     */
    public void updateUserPreferences(String userId, Itinerary itinerary, String interactionType) {
        logger.debug("Updating preferences for user: {} based on itinerary: {}", userId, itinerary.getId());
        
        // Extract preferences from itinerary (budget range, trip duration, etc.)
        String budgetRange = getBudgetRange(itinerary.getBudget());
        String durationRange = getDurationRange(itinerary.getDays());
        
        // Update or create preferences
        updateOrCreatePreference(userId, "budget_range", budgetRange, getInteractionWeight(interactionType));
        updateOrCreatePreference(userId, "trip_duration", durationRange, getInteractionWeight(interactionType));
    }

    private void updateOrCreatePreference(String userId, String preferenceType, String preferenceValue, BigDecimal weight) {
        Optional<UserPreference> existing = userPreferenceRepository
                .findByUserIdAndPreferenceTypeAndPreferenceValue(userId, preferenceType, preferenceValue);
        
        if (existing.isPresent()) {
            UserPreference pref = existing.get();
            BigDecimal newWeight = pref.getWeight().add(weight);
            if (newWeight.compareTo(BigDecimal.ONE) > 0) {
                newWeight = BigDecimal.ONE;
            }
            pref.setWeight(newWeight);
            userPreferenceRepository.save(pref);
        } else {
            UserPreference newPref = new UserPreference(userId, preferenceType, preferenceValue, weight);
            userPreferenceRepository.save(newPref);
        }
    }

    private String getBudgetRange(Integer budget) {
        if (budget < 500) return "budget";
        if (budget < 1500) return "mid-range";
        return "luxury";
    }

    private String getDurationRange(Integer days) {
        if (days <= 3) return "short";
        if (days <= 7) return "week";
        if (days <= 14) return "extended";
        return "long";
    }

    private BigDecimal getInteractionWeight(String interactionType) {
        return switch (interactionType.toLowerCase()) {
            case "created" -> new BigDecimal("0.8");
            case "rated" -> new BigDecimal("0.6");
            case "saved" -> new BigDecimal("0.4");
            case "shared" -> new BigDecimal("0.3");
            case "viewed" -> new BigDecimal("0.1");
            default -> new BigDecimal("0.1");
        };
    }

    /**
     * Clean up expired recommendations
     */
    @Transactional
    public void cleanupExpiredRecommendations() {
        int deletedCount = recommendationRepository.deleteExpiredRecommendations();
        logger.info("Cleaned up {} expired recommendations", deletedCount);
    }
}