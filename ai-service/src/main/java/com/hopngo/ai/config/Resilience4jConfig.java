package com.hopngo.ai.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {
    
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }
    
    @Bean
    public RateLimiter aiEndpointsRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 requests
                .limitRefreshPeriod(Duration.ofSeconds(60)) // per 60 seconds
                .timeoutDuration(Duration.ofSeconds(5)) // timeout after 5 seconds
                .build();
        
        return registry.rateLimiter("ai-endpoints", config);
    }
    
    @Bean
    public RateLimiter userQuotaRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50) // 50 requests
                .limitRefreshPeriod(Duration.ofHours(1)) // per hour
                .timeoutDuration(Duration.ofSeconds(3)) // timeout after 3 seconds
                .build();
        
        return registry.rateLimiter("user-quota", config);
    }
}