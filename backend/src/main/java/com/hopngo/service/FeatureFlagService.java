package com.hopngo.service;

import com.hopngo.entity.FeatureFlag;
import com.hopngo.repository.FeatureFlagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeatureFlagService {
    
    @Autowired
    private FeatureFlagRepository featureFlagRepository;
    
    @Cacheable(value = "feature-flags", key = "'all'")
    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAll();
    }
    
    @Cacheable(value = "feature-flags", key = "'enabled'")
    public List<FeatureFlag> getEnabledFlags() {
        return featureFlagRepository.findAllEnabled();
    }
    
    @Cacheable(value = "feature-flags", key = "#key")
    public boolean isFeatureEnabled(String key) {
        try {
            Optional<FeatureFlag> flag = featureFlagRepository.findByKey(key);
            return flag.map(FeatureFlag::getEnabled).orElse(false);
        } catch (Exception e) {
            // Return false if database is not ready or table doesn't exist
            return false;
        }
    }
    
    public Optional<FeatureFlag> getFeatureFlag(String key) {
        return featureFlagRepository.findByKey(key);
    }
    
    public FeatureFlag createOrUpdateFlag(String key, String description, Boolean enabled, String payload) {
        Optional<FeatureFlag> existingFlag = featureFlagRepository.findByKey(key);
        
        FeatureFlag flag;
        if (existingFlag.isPresent()) {
            flag = existingFlag.get();
            flag.setDescription(description);
            flag.setEnabled(enabled);
            flag.setPayload(payload);
        } else {
            flag = new FeatureFlag(key, description, enabled);
            flag.setPayload(payload);
        }
        
        return featureFlagRepository.save(flag);
    }
    
    public void initializeDefaultFlags() {
        try {
            // Initialize recs_v1 flag if it doesn't exist
            if (!featureFlagRepository.existsByKey("recs_v1")) {
                createOrUpdateFlag(
                    "recs_v1", 
                    "Enable recommendations system v1 features", 
                    false, // Start disabled for gradual rollout
                    "{\"rollout_percentage\": 0}"
                );
            }
            
            // Add other default flags as needed
            if (!featureFlagRepository.existsByKey("new_ui_design")) {
                createOrUpdateFlag(
                    "new_ui_design", 
                    "Enable new UI design components", 
                    false,
                    null
                );
            }
        } catch (Exception e) {
            // Re-throw to be handled by the caller
            throw new RuntimeException("Failed to initialize default feature flags", e);
        }
    }
}