package com.hopngo.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Security Headers Configuration for HopNGo Gateway
 * Implements comprehensive HTTP security headers including:
 * - Content Security Policy (CSP)
 * - HTTP Strict Transport Security (HSTS)
 * - X-Content-Type-Options
 * - X-Frame-Options
 * - Referrer-Policy
 * - Permissions-Policy
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Security headers filter that adds comprehensive security headers to all responses
     */
    @Bean
    public WebFilter securityHeadersFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            var response = exchange.getResponse();
            var headers = response.getHeaders();
            
            // Content Security Policy - Strict policy
            headers.add("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https: blob:; " +
                "connect-src 'self' https://api.hopngo.com wss://api.hopngo.com; " +
                "media-src 'self'; " +
                "object-src 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'; " +
                "frame-ancestors 'none'; " +
                "upgrade-insecure-requests; " +
                "block-all-mixed-content");
            
            // HTTP Strict Transport Security with preload
            headers.add("Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload");
            
            // Prevent MIME type sniffing
            headers.add("X-Content-Type-Options", "nosniff");
            
            // Prevent clickjacking
            headers.add("X-Frame-Options", "DENY");
            
            // Strict referrer policy
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Permissions Policy - Restrict dangerous features
            headers.add("Permissions-Policy", 
                "geolocation=(self), " +
                "microphone=(), " +
                "camera=(), " +
                "payment=(self), " +
                "usb=(), " +
                "magnetometer=(), " +
                "gyroscope=(), " +
                "speaker=(), " +
                "vibrate=(), " +
                "fullscreen=(self), " +
                "sync-xhr=()");
            
            // X-XSS-Protection (legacy but still useful)
            headers.add("X-XSS-Protection", "1; mode=block");
            
            // Cache control for sensitive pages
            if (exchange.getRequest().getPath().value().contains("/admin") ||
                exchange.getRequest().getPath().value().contains("/profile")) {
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");
            }
            
            // Remove server information
            headers.remove("Server");
            headers.add("Server", "HopNGo");
            
            return chain.filter(exchange);
        };
    }

    /**
     * CORS configuration with security-focused settings
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Allow specific origins only (update with actual domains)
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
            "https://hopngo.com",
            "https://*.hopngo.com",
            "http://localhost:3000", // Development only
            "http://localhost:8080"  // Development only
        ));
        
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-CSRF-Token"
        ));
        
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L); // 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}