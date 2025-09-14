package com.hopngo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RequestValidationFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(RequestValidationFilter.class);
    
    @Value("${security.max-request-size:10MB}")
    private String maxRequestSize;
    
    @Value("${security.enable-path-traversal-protection:true}")
    private boolean enablePathTraversalProtection;
    
    @Value("${security.enable-request-size-validation:true}")
    private boolean enableRequestSizeValidation;
    
    @Value("${security.blocked-file-extensions:.exe,.bat,.cmd,.sh,.ps1,.vbs,.jar,.war}")
    private String blockedFileExtensions;
    
    // Path traversal patterns
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\.\\.[\\/\\\\].*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*[\\/\\\\]\\.\\.[\\/\\\\].*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%2e%2e[\\/\\\\].*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%252e%252e[\\/\\\\].*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.\\.%2f.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.\\.%5c.*", Pattern.CASE_INSENSITIVE)
    );
    
    // Suspicious path patterns
    private static final List<Pattern> SUSPICIOUS_PATH_PATTERNS = Arrays.asList(
        Pattern.compile(".*/etc/passwd.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/etc/shadow.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/windows/system32.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/proc/.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/dev/.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\\\.*", Pattern.CASE_INSENSITIVE), // UNC paths
        Pattern.compile(".*file:.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*jar:.*", Pattern.CASE_INSENSITIVE)
    );
    
    // SQL injection patterns
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile(".*('|(\\x27)|(\\x2D)|(\\x2d)).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(union|select|insert|update|delete|drop|create|alter|exec|execute).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(-|\\x2D|\\x2d){2,}.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(;|\\x3B|\\x3b).*", Pattern.CASE_INSENSITIVE)
    );
    
    // XSS patterns
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
        Pattern.compile(".*<script.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*javascript:.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*on(load|error|click|mouseover|focus|blur).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*<iframe.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*<object.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*<embed.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*<svg.*onload.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*data:text/html.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*vbscript:.*", Pattern.CASE_INSENSITIVE)
    );
    
    // Additional security patterns for advanced threats
    private static final List<Pattern> ADVANCED_THREAT_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\\\x[0-9a-f]{2}.*", Pattern.CASE_INSENSITIVE), // Hex encoding
        Pattern.compile(".*%u[0-9a-f]{4}.*", Pattern.CASE_INSENSITIVE), // Unicode encoding
        Pattern.compile(".*\\\\u[0-9a-f]{4}.*", Pattern.CASE_INSENSITIVE), // Unicode escape
        Pattern.compile(".*eval\\s*\\(.*", Pattern.CASE_INSENSITIVE), // JavaScript eval
        Pattern.compile(".*expression\\s*\\(.*", Pattern.CASE_INSENSITIVE), // CSS expression
        Pattern.compile(".*@import.*", Pattern.CASE_INSENSITIVE) // CSS import
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Validate request size
        if (enableRequestSizeValidation && exceedsMaxSize(request)) {
            return handleRequestTooLarge(exchange);
        }
        
        // Validate path for traversal attacks
        if (enablePathTraversalProtection) {
            String path = request.getPath().value();
            String decodedPath = urlDecode(path);
            
            if (containsPathTraversal(path) || containsPathTraversal(decodedPath)) {
                log.warn("Path traversal attempt detected: {} from IP: {}", 
                        path, getClientIpAddress(request));
                return handleSecurityViolation(exchange, "Path traversal detected");
            }
            
            if (containsSuspiciousPath(path) || containsSuspiciousPath(decodedPath)) {
                log.warn("Suspicious path access attempt: {} from IP: {}", 
                        path, getClientIpAddress(request));
                return handleSecurityViolation(exchange, "Suspicious path detected");
            }
            
            if (containsBlockedFileExtension(path)) {
                log.warn("Blocked file extension access attempt: {} from IP: {}", 
                        path, getClientIpAddress(request));
                return handleSecurityViolation(exchange, "Blocked file type");
            }
        }
        
        // Validate query parameters
        String queryString = request.getURI().getQuery();
        if (queryString != null) {
            String decodedQuery = urlDecode(queryString);
            
            if (containsSqlInjection(queryString) || containsSqlInjection(decodedQuery)) {
                log.warn("SQL injection attempt detected in query: {} from IP: {}", 
                        queryString, getClientIpAddress(request));
                return handleSecurityViolation(exchange, "SQL injection detected");
            }
            
            if (containsXss(queryString) || containsXss(decodedQuery)) {
                log.warn("XSS attempt detected in query: {} from IP: {}", 
                        queryString, getClientIpAddress(request));
                return handleSecurityViolation(exchange, "XSS detected");
            }
        }
        
        // Validate headers
        request.getHeaders().forEach((name, values) -> {
            for (String value : values) {
                if (containsXss(value) || containsSqlInjection(value) || containsAdvancedThreats(value)) {
                    log.warn("Malicious content detected in header {}: {} from IP: {}", 
                            name, value, getClientIpAddress(request));
                }
            }
        });
        
        // Additional validation for User-Agent header
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (userAgent != null && isSuspiciousUserAgent(userAgent)) {
            log.warn("Suspicious User-Agent detected: {} from IP: {}", 
                    userAgent, getClientIpAddress(request));
        }
        
        return chain.filter(exchange);
    }
    
    private boolean exceedsMaxSize(ServerHttpRequest request) {
        try {
            String contentLengthHeader = request.getHeaders().getFirst("Content-Length");
            if (contentLengthHeader != null) {
                long contentLength = Long.parseLong(contentLengthHeader);
                long maxSizeBytes = DataSize.parse(maxRequestSize).toBytes();
                return contentLength > maxSizeBytes;
            }
        } catch (Exception e) {
            log.warn("Error parsing content length: {}", e.getMessage());
        }
        return false;
    }
    
    private boolean containsPathTraversal(String input) {
        if (input == null) return false;
        return PATH_TRAVERSAL_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }
    
    private boolean containsSuspiciousPath(String input) {
        if (input == null) return false;
        return SUSPICIOUS_PATH_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }
    
    private boolean containsSqlInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }
    
    private boolean containsXss(String input) {
        if (input == null) return false;
        return XSS_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }
    
    private boolean containsBlockedFileExtension(String path) {
        if (path == null || blockedFileExtensions == null) return false;
        
        String[] extensions = blockedFileExtensions.split(",");
        String lowerPath = path.toLowerCase();
        
        return Arrays.stream(extensions)
                .map(String::trim)
                .anyMatch(lowerPath::endsWith);
    }
    
    private boolean containsAdvancedThreats(String input) {
        if (input == null) return false;
        return ADVANCED_THREAT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }
    
    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null) return false;
        
        String lowerUserAgent = userAgent.toLowerCase();
        
        // Check for common attack tools and suspicious patterns
        return lowerUserAgent.contains("sqlmap") ||
               lowerUserAgent.contains("nikto") ||
               lowerUserAgent.contains("nessus") ||
               lowerUserAgent.contains("burp") ||
               lowerUserAgent.contains("owasp") ||
               lowerUserAgent.contains("scanner") ||
               lowerUserAgent.length() > 500 || // Unusually long user agent
               lowerUserAgent.isEmpty(); // Empty user agent
    }
    
    private String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return input;
        }
    }
    
    private String getClientIpAddress(ServerHttpRequest request) {
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
    
    private Mono<Void> handleRequestTooLarge(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        
        String errorMessage = "{\"error\":\"Request too large\",\"message\":\"Request size exceeds maximum allowed limit.\"}";
        DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    private Mono<Void> handleSecurityViolation(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        
        String errorMessage = String.format(
                "{\"error\":\"Security violation\",\"message\":\"%s\"}", reason);
        DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -2; // Execute before rate limiting and other filters
    }
}