package com.hopngo.config.service;

import com.hopngo.config.dto.FeatureFlagDto;
import com.hopngo.config.dto.FeatureFlagEvaluationRequest;
import com.hopngo.config.dto.FeatureFlagEvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeatureFlagEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagEvaluationService.class);
    
    private final FeatureFlagService featureFlagService;
    
    @Autowired
    public FeatureFlagEvaluationService(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }
    
    /**
     * Evaluate a single feature flag for a user
     */
    public boolean evaluateFlag(String flagKey, String userId) {
        return evaluateFlag(flagKey, userId, null);
    }
    
    /**
     * Evaluate a single feature flag for a user with additional context
     */
    public boolean evaluateFlag(String flagKey, String userId, Map<String, Object> context) {
        logger.debug("Evaluating feature flag '{}' for user '{}'", flagKey, userId);
        
        Optional<FeatureFlagDto> flagOpt = featureFlagService.getFlagByKey(flagKey);
        if (flagOpt.isEmpty()) {
            logger.debug("Feature flag '{}' not found, returning false", flagKey);
            return false;
        }
        
        FeatureFlagDto flag = flagOpt.get();
        
        // If flag is disabled, return false
        if (!flag.isEnabled()) {
            logger.debug("Feature flag '{}' is disabled", flagKey);
            return false;
        }
        
        // Check if user is in target users list (if specified)
        if (flag.getPayload() != null && flag.getPayload().has("targetUsers")) {
            List<String> targetUsers = flag.getPayload().get("targetUsers").findValuesAsText("userId");
            if (!targetUsers.isEmpty() && targetUsers.contains(userId)) {
                logger.debug("User '{}' is in target users for flag '{}'", userId, flagKey);
                return true;
            }
        }
        
        // Check rollout percentage
        int rolloutPercentage = 100; // Default to 100%
        if (flag.getPayload() != null && flag.getPayload().has("rolloutPercentage")) {
            rolloutPercentage = flag.getPayload().get("rolloutPercentage").asInt(100);
        }
        
        if (rolloutPercentage == 0) {
            logger.debug("Feature flag '{}' has 0% rollout", flagKey);
            return false;
        }
        
        if (rolloutPercentage == 100) {
            logger.debug("Feature flag '{}' has 100% rollout", flagKey);
            return true;
        }
        
        // Use consistent hashing for rollout percentage
        boolean inRollout = isUserInRollout(userId, flagKey, rolloutPercentage);
        logger.debug("User '{}' rollout evaluation for flag '{}': {}", userId, flagKey, inRollout);
        
        return inRollout;
    }
    
    /**
     * Evaluate multiple feature flags for a user
     */
    public FeatureFlagEvaluationResponse evaluateFlags(FeatureFlagEvaluationRequest request) {
        logger.debug("Evaluating {} feature flags for user '{}'", 
                    request.getFlagKeys().size(), request.getUserId());
        
        Map<String, Boolean> results = new HashMap<>();
        
        for (String flagKey : request.getFlagKeys()) {
            boolean result = evaluateFlag(flagKey, request.getUserId(), request.getContext());
            results.put(flagKey, result);
        }
        
        return new FeatureFlagEvaluationResponse(request.getUserId(), results);
    }
    
    /**
     * Get all enabled flags for a user (considering rollout and targeting)
     */
    public Map<String, Boolean> getEnabledFlagsForUser(String userId) {
        logger.debug("Getting all enabled flags for user '{}'", userId);
        
        List<FeatureFlagDto> allFlags = featureFlagService.getEnabledFlags();
        Map<String, Boolean> userFlags = new HashMap<>();
        
        for (FeatureFlagDto flag : allFlags) {
            boolean enabled = evaluateFlag(flag.getKey(), userId);
            userFlags.put(flag.getKey(), enabled);
        }
        
        return userFlags;
    }
    
    /**
     * Determine if a user is in the rollout percentage using consistent hashing
     */
    private boolean isUserInRollout(String userId, String flagKey, int rolloutPercentage) {
        // Create a consistent hash based on user ID and flag key
        String hashInput = userId + ":" + flagKey;
        int hash = Math.abs(hashInput.hashCode());
        int bucket = hash % 100;
        
        return bucket < rolloutPercentage;
    }
    
    /**
     * Check if user meets specific conditions (extensible for future use)
     */
    private boolean evaluateConditions(Map<String, Object> conditions, String userId, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        // Example condition evaluations (can be extended)
        if (conditions.containsKey("userSegment")) {
            String requiredSegment = (String) conditions.get("userSegment");
            String userSegment = context != null ? (String) context.get("userSegment") : null;
            if (!requiredSegment.equals(userSegment)) {
                return false;
            }
        }
        
        if (conditions.containsKey("minVersion")) {
            String minVersion = (String) conditions.get("minVersion");
            String userVersion = context != null ? (String) context.get("appVersion") : null;
            if (userVersion == null || !isVersionGreaterOrEqual(userVersion, minVersion)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Simple version comparison (assumes semantic versioning)
     */
    private boolean isVersionGreaterOrEqual(String version, String minVersion) {
        try {
            String[] versionParts = version.split("\\.");
            String[] minVersionParts = minVersion.split("\\.");
            
            for (int i = 0; i < Math.max(versionParts.length, minVersionParts.length); i++) {
                int v1 = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
                int v2 = i < minVersionParts.length ? Integer.parseInt(minVersionParts[i]) : 0;
                
                if (v1 > v2) return true;
                if (v1 < v2) return false;
            }
            
            return true; // Equal versions
        } catch (NumberFormatException e) {
            logger.warn("Invalid version format: {} or {}", version, minVersion);
            return false;
        }
    }
}