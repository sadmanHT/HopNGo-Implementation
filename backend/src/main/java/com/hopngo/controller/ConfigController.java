package com.hopngo.controller;

import com.hopngo.entity.FeatureFlag;
import com.hopngo.service.FeatureFlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/config")
@CrossOrigin(origins = "*")
public class ConfigController {
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    @GetMapping("/feature-flags")
    public ResponseEntity<List<FeatureFlag>> getFeatureFlags() {
        List<FeatureFlag> flags = featureFlagService.getAllFlags();
        return ResponseEntity.ok(flags);
    }
    
    @GetMapping("/feature-flags/enabled")
    public ResponseEntity<List<FeatureFlag>> getEnabledFeatureFlags() {
        List<FeatureFlag> flags = featureFlagService.getEnabledFlags();
        return ResponseEntity.ok(flags);
    }
    
    @GetMapping("/feature-flags/{key}")
    public ResponseEntity<FeatureFlag> getFeatureFlag(@PathVariable String key) {
        Optional<FeatureFlag> flag = featureFlagService.getFeatureFlag(key);
        return flag.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/feature-flags/{key}/enabled")
    public ResponseEntity<Map<String, Boolean>> isFeatureEnabled(@PathVariable String key) {
        boolean enabled = featureFlagService.isFeatureEnabled(key);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }
    
    // Admin endpoints for managing flags (could be secured with admin role)
    @PostMapping("/feature-flags")
    public ResponseEntity<FeatureFlag> createOrUpdateFeatureFlag(
            @RequestBody CreateFeatureFlagRequest request) {
        FeatureFlag flag = featureFlagService.createOrUpdateFlag(
            request.getKey(),
            request.getDescription(),
            request.getEnabled(),
            request.getPayload()
        );
        return ResponseEntity.ok(flag);
    }
    
    // DTO for creating/updating feature flags
    public static class CreateFeatureFlagRequest {
        private String key;
        private String description;
        private Boolean enabled;
        private String payload;
        
        // Constructors
        public CreateFeatureFlagRequest() {}
        
        public CreateFeatureFlagRequest(String key, String description, Boolean enabled, String payload) {
            this.key = key;
            this.description = description;
            this.enabled = enabled;
            this.payload = payload;
        }
        
        // Getters and Setters
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getPayload() {
            return payload;
        }
        
        public void setPayload(String payload) {
            this.payload = payload;
        }
    }
}