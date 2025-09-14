package com.hopngo.config;

import com.hopngo.common.monitoring.SentryInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering interceptors and other web-related settings
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final SentryInterceptor sentryInterceptor;
    
    public WebConfig(SentryInterceptor sentryInterceptor) {
        this.sentryInterceptor = sentryInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register Sentry interceptor for all API endpoints
        registry.addInterceptor(sentryInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/health",
                    "/api/actuator/**",
                    "/api/swagger-ui/**",
                    "/api/v3/api-docs/**"
                );
    }
}