package com.hopngo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Custom security filter that adds comprehensive security headers
 * and validates CORS requests with strict origin checking
 */
@Component
public class SecurityHeadersFilter extends AbstractGatewayFilterFactory<SecurityHeadersFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);
    
    @Value("${security.allowed-origins:http://localhost:3000,https://hopngo.com}")
    private String allowedOrigins;
    
    @Value("${security.strict-origin-validation:true}")
    private boolean strictOriginValidation;
    
    @Value("${security.enable-request-logging:false}")
    private boolean enableRequestLogging;
    
    @Value("${security.max-request-size:10MB}")
    private String maxRequestSize;
    
    // Patterns for suspicious requests
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\.\\..*", Pattern.CASE_INSENSITIVE), // Path traversal
        Pattern.compile(".*(script|javascript|vbscript).*", Pattern.CASE_INSENSITIVE), // Script injection
        Pattern.compile(".*(union|select|insert|delete|drop|create|alter).*", Pattern.CASE_INSENSITIVE), // SQL injection
        Pattern.compile(".*(<|%3C)(script|iframe|object|embed).*", Pattern.CASE_INSENSITIVE) // XSS attempts
    );
    
    public SecurityHeadersFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            
            // Log security-relevant request details
            logSecurityInfo(request);
            
            // Validate request for suspicious patterns
            if (containsSuspiciousContent(request)) {
                logger.warn("Suspicious request detected from IP: {} - Path: {} - User-Agent: {}", 
                    getClientIp(request), request.getPath(), request.getHeaders().getFirst("User-Agent"));
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return response.setComplete();
            }
            
            // Strict origin validation for CORS requests
            if (strictOriginValidation && !isValidOrigin(request)) {
                logger.warn("Invalid origin detected: {} from IP: {}", 
                    request.getHeaders().getFirst("Origin"), getClientIp(request));
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }
            
            // Add comprehensive security headers
            addSecurityHeaders(response, request);
            
            return chain.filter(exchange);
        };
    }
    
    private void logSecurityInfo(ServerHttpRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String origin = request.getHeaders().getFirst("Origin");
        String referer = request.getHeaders().getFirst("Referer");
        
        logger.debug("Security check - IP: {}, Origin: {}, Referer: {}, User-Agent: {}, Path: {}", 
            clientIp, origin, referer, userAgent, request.getPath());
    }
    
    private boolean containsSuspiciousContent(ServerHttpRequest request) {
        String path = request.getPath().value();
        String queryString = request.getURI().getQuery();
        
        // Check path for suspicious patterns
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
            if (queryString != null && pattern.matcher(queryString).matches()) {
                return true;
            }
        }
        
        // Check headers for suspicious content
        HttpHeaders headers = request.getHeaders();
        String userAgent = headers.getFirst("User-Agent");
        if (userAgent != null && (userAgent.length() > 1000 || containsScriptTags(userAgent))) {
            return true;
        }
        
        return false;
    }
    
    private boolean containsScriptTags(String input) {
        return input.toLowerCase().contains("<script") || 
               input.toLowerCase().contains("javascript:") ||
               input.toLowerCase().contains("vbscript:");
    }
    
    private boolean isValidOrigin(ServerHttpRequest request) {
        String origin = request.getHeaders().getFirst("Origin");
        
        // Allow requests without Origin header (non-CORS requests)
        if (origin == null) {
            return true;
        }
        
        // Check against allowed origins
        List<String> allowedOriginsList = Arrays.asList(allowedOrigins.split(","));
        for (String allowedOrigin : allowedOriginsList) {
            if (origin.equals(allowedOrigin.trim())) {
                return true;
            }
        }
        
        return false;
    }
    
    private void addSecurityHeaders(ServerHttpResponse response, ServerHttpRequest request) {
        HttpHeaders headers = response.getHeaders();
        
        // Core security headers (if not already set)
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("X-XSS-Protection", "1; mode=block");
        headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // HSTS for HTTPS requests
        if (request.getURI().getScheme().equals("https")) {
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }
        
        // Permissions Policy
        headers.add("Permissions-Policy", 
            "camera=(), microphone=(), geolocation=(self), payment=(), usb=(), magnetometer=(), gyroscope=()");
        
        // Content Security Policy with environment-aware settings
        String csp = buildContentSecurityPolicy(request);
        headers.add("Content-Security-Policy", csp);
        
        // Cache control for sensitive endpoints
        if (isSensitiveEndpoint(request.getPath().value())) {
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate, private");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
        } else {
            headers.add("Cache-Control", "public, max-age=300");
        }
        
        // Custom security headers
        headers.add("X-Robots-Tag", "noindex, nofollow");
        headers.add("X-Permitted-Cross-Domain-Policies", "none");
    }
    
    private String buildContentSecurityPolicy(ServerHttpRequest request) {
        StringBuilder csp = new StringBuilder();
        
        // Base policy
        csp.append("default-src 'self'; ");
        
        // Script sources - restrictive for production
        if (isProductionEnvironment()) {
            csp.append("script-src 'self'; ");
        } else {
            csp.append("script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://unpkg.com; ");
        }
        
        // Style sources
        csp.append("style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; ");
        
        // Font sources
        csp.append("font-src 'self' https://fonts.gstatic.com; ");
        
        // Image sources
        csp.append("img-src 'self' data: https:; ");
        
        // Connect sources - include allowed origins
        csp.append("connect-src 'self' ").append(allowedOrigins.replace(",", " ")).append(" wss: ws:; ");
        
        // Frame and form policies
        csp.append("frame-ancestors 'none'; ");
        csp.append("base-uri 'self'; ");
        csp.append("form-action 'self'; ");
        
        // Object and media restrictions
        csp.append("object-src 'none'; ");
        csp.append("media-src 'self';");
        
        return csp.toString();
    }
    
    private boolean isSensitiveEndpoint(String path) {
        return path.contains("/auth/") || 
               path.contains("/admin/") || 
               path.contains("/api/v1/auth/") ||
               path.contains("/api/v1/admin/") ||
               path.contains("/actuator/");
    }
    
    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "prod".equals(profile) || "production".equals(profile);
    }
    
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
    
    public static class Config {
        // Configuration properties can be added here if needed
    }
}