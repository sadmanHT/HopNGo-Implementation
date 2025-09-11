package com.hopngo.config;

import com.hopngo.service.FeatureFlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@Configuration
@EnableCaching
public class FeatureFlagConfig {
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("feature-flags");
    }
    
    @Bean
    public CommandLineRunner initializeFeatureFlags() {
        return args -> {
            // Initialize default feature flags on application startup
            featureFlagService.initializeDefaultFlags();
        };
    }
}