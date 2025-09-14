package com.hopngo.config;

import com.hopngo.common.monitoring.SentryEventProcessor;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
// Removed EnableSentry import - not available in sentry-spring-boot-starter-jakarta 7.0.0
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Configuration class for Sentry error monitoring
 */
@Configuration
@ConditionalOnProperty(name = "hopngo.error-monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class SentryConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(SentryConfiguration.class);
    
    @Value("${sentry.dsn:}")
    private String sentryDsn;
    
    @Value("${sentry.environment:${spring.profiles.active:development}}")
    private String environment;
    
    @Value("${sentry.release:${app.version:1.0.0}}")
    private String release;
    
    @Value("${sentry.traces-sample-rate:0.1}")
    private Double tracesSampleRate;
    
    @Value("${sentry.profiles-sample-rate:0.1}")
    private Double profilesSampleRate;
    
    @Value("${sentry.debug:false}")
    private Boolean debug;
    
    private final SentryEventProcessor sentryEventProcessor;
    
    public SentryConfiguration(SentryEventProcessor sentryEventProcessor) {
        this.sentryEventProcessor = sentryEventProcessor;
    }
    
    @PostConstruct
    public void configureSentry() {
        if (sentryDsn == null || sentryDsn.trim().isEmpty()) {
            logger.warn("Sentry DSN not configured. Error monitoring will be disabled.");
            return;
        }
        
        try {
            Sentry.init(options -> {
                configureSentryOptions(options);
            });
            
            logger.info("Sentry initialized successfully for environment: {}", environment);
            
        } catch (Exception e) {
            logger.error("Failed to initialize Sentry", e);
        }
    }
    
    private void configureSentryOptions(SentryOptions options) {
        // Basic configuration
        options.setDsn(sentryDsn);
        options.setEnvironment(environment);
        options.setRelease(release);
        options.setDebug(debug);
        
        // Performance monitoring
        options.setTracesSampleRate(tracesSampleRate);
        options.setProfilesSampleRate(profilesSampleRate);
        options.setEnableTracing(true);
        
        // Privacy settings
        options.setSendDefaultPii(false);
        
        // Add custom event processor
        options.addEventProcessor(sentryEventProcessor);
        
        // Configure integrations
        // Note: SentrySpringIntegration will be auto-configured by Spring Boot
        
        // Set in-app packages
        options.addInAppInclude("com.hopngo");
        
        // Configure ignored exceptions
        options.addIgnoredExceptionForType(com.hopngo.common.exception.BusinessException.class);
        options.addIgnoredExceptionForType(com.hopngo.common.exception.ResourceNotFoundException.class);
        options.addIgnoredExceptionForType(org.springframework.security.access.AccessDeniedException.class);
        options.addIgnoredExceptionForType(org.springframework.web.bind.MethodArgumentNotValidException.class);
        
        // Set server name
        options.setServerName(System.getenv().getOrDefault("HOSTNAME", "hopngo-backend"));
        
        // Configure tags
        options.setTag("service", "backend");
        options.setTag("component", "api");
        
        // Configure breadcrumbs
        options.setMaxBreadcrumbs(100);
        
        // Configure before send callback
        options.setBeforeSend((event, hint) -> {
            // Additional filtering can be done here
            return event;
        });
        
        // Configure before breadcrumb callback
        options.setBeforeBreadcrumb((breadcrumb, hint) -> {
            // Filter sensitive breadcrumbs
            if (breadcrumb.getMessage() != null && 
                breadcrumb.getMessage().toLowerCase().contains("password")) {
                return null; // Drop sensitive breadcrumbs
            }
            return breadcrumb;
        });
    }
    
    // Sentry auto-configuration is handled by @EnableSentry annotation
    // No manual bean configuration needed for Spring Boot 3 with Jakarta EE
    
    /**
     * Bean for development environment to disable Sentry
     */
    @Bean
    @Profile("test")
    public SentryOptions.BeforeSendCallback testBeforeSendCallback() {
        return (event, hint) -> {
            logger.debug("Sentry event dropped in test environment: {}", event.getEventId());
            return null; // Drop all events in test environment
        };
    }
}