package com.hopngo.config;

import com.hopngo.service.FeatureFlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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
    
    // Temporarily disabled to resolve startup issues
    // @Bean
    // @DependsOn("entityManagerFactory")
    // public ApplicationRunner initializeFeatureFlags() {
    //     return args -> {
    //         try {
    //             // Initialize default feature flags on application startup
    //             featureFlagService.initializeDefaultFlags();
    //         } catch (Exception e) {
    //             // Log the error but don't fail the application startup
    //             System.err.println("Warning: Could not initialize default feature flags: " + e.getMessage());
    //             // The flags will be created when first accessed
    //         }
    //     };
    // }
}