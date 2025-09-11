package com.hopngo.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRetry
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure retry policy
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RuntimeException.class, true);
        retryableExceptions.put(Exception.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // 1 second
        backOffPolicy.setMaxInterval(10000L);    // 10 seconds
        backOffPolicy.setMultiplier(2.0);        // Double each time
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
    
    @Bean
    public RetryTemplate notificationRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // More aggressive retry for critical notifications
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RuntimeException.class, true);
        retryableExceptions.put(Exception.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure backoff policy for notifications
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000L); // 2 seconds
        backOffPolicy.setMaxInterval(30000L);    // 30 seconds
        backOffPolicy.setMultiplier(1.5);        // 1.5x multiplier
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
    
    @Bean
    public RetryTemplate emergencyRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Most aggressive retry for emergency notifications
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RuntimeException.class, true);
        retryableExceptions.put(Exception.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(10, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure backoff policy for emergency notifications
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500L);  // 0.5 seconds
        backOffPolicy.setMaxInterval(5000L);     // 5 seconds
        backOffPolicy.setMultiplier(1.2);        // 1.2x multiplier
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}