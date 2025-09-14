package com.hopngo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${rate-limiting.requests-per-minute:60}")
    private int requestsPerMinute;
    
    @Value("${rate-limiting.requests-per-hour:1000}")
    private int requestsPerHour;
    
    @Value("${rate-limiting.burst-capacity:10}")
    private int burstCapacity;
    
    @Value("${rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;
    
    @Value("${rate-limiting.whitelist-ips:127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
    private String whitelistIps;
    
    @Value("${rate-limiting.enable-ip-blocking:true}")
    private boolean enableIpBlocking;
    
    @Value("${rate-limiting.block-threshold:100}")
    private int blockThreshold;
    
    // Critical endpoints that need stricter rate limiting
    private static final String[] CRITICAL_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/verify-2fa",
        "/api/v1/auth/refresh",
        "/api/v1/admin",
        "/actuator"
    };
    
    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitingEnabled) {
            return chain.filter(exchange);
        }
        
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIpAddress(request);
        String path = request.getPath().value();
        
        // Skip rate limiting for whitelisted IPs
        if (isWhitelistedIp(clientIp)) {
            return chain.filter(exchange);
        }
        
        // Check if IP is blocked
        if (enableIpBlocking && isIpBlocked(clientIp)) {
            log.warn("Blocked IP {} attempted to access {}", clientIp, path);
            return handleIpBlocked(exchange);
        }
        
        // Check rate limits
        if (isRateLimited(clientIp, path)) {
            // Increment violation count for potential IP blocking
            if (enableIpBlocking) {
                incrementViolationCount(clientIp);
            }
            return handleRateLimitExceeded(exchange);
        }
        
        // Record the request
        recordRequest(clientIp, path);
        
        return chain.filter(exchange);
    }
    
    private boolean isRateLimited(String clientIp, String path) {
        boolean isCriticalEndpoint = isCriticalEndpoint(path);
        
        // Check per-minute rate limit
        String minuteKey = "rate_limit:minute:" + clientIp;
        int minuteLimit = isCriticalEndpoint ? requestsPerMinute / 4 : requestsPerMinute;
        
        if (exceedsLimit(minuteKey, minuteLimit, 60)) {
            log.warn("Rate limit exceeded for IP {} on path {} (per-minute limit: {})", 
                    clientIp, path, minuteLimit);
            return true;
        }
        
        // Check per-hour rate limit
        String hourKey = "rate_limit:hour:" + clientIp;
        int hourLimit = isCriticalEndpoint ? requestsPerHour / 4 : requestsPerHour;
        
        if (exceedsLimit(hourKey, hourLimit, 3600)) {
            log.warn("Rate limit exceeded for IP {} on path {} (per-hour limit: {})", 
                    clientIp, path, hourLimit);
            return true;
        }
        
        // Check burst capacity for critical endpoints
        if (isCriticalEndpoint) {
            String burstKey = "rate_limit:burst:" + clientIp + ":" + path;
            if (exceedsLimit(burstKey, burstCapacity, 10)) {
                log.warn("Burst limit exceeded for IP {} on critical path {} (burst limit: {})", 
                        clientIp, path, burstCapacity);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean exceedsLimit(String key, int limit, int windowSeconds) {
        try {
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            return currentCount >= limit;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}, Redis may be unavailable", key, e);
            // For critical endpoints, fail closed when Redis is unavailable
            // For non-critical endpoints, fail open
            return key.contains("critical") || key.contains("auth") || key.contains("admin");
        }
    }
    
    private void recordRequest(String clientIp, String path) {
        try {
            long currentTime = Instant.now().getEpochSecond();
            
            // Record per-minute count
            String minuteKey = "rate_limit:minute:" + clientIp;
            incrementCounter(minuteKey, 60);
            
            // Record per-hour count
            String hourKey = "rate_limit:hour:" + clientIp;
            incrementCounter(hourKey, 3600);
            
            // Record burst count for critical endpoints
            if (isCriticalEndpoint(path)) {
                String burstKey = "rate_limit:burst:" + clientIp + ":" + path;
                incrementCounter(burstKey, 10);
            }
            
            // Record request timestamp for analytics
            String timestampKey = "rate_limit:timestamp:" + clientIp;
            redisTemplate.opsForValue().set(timestampKey, String.valueOf(currentTime), 
                    Duration.ofHours(1));
            
        } catch (Exception e) {
            log.error("Error recording request for IP: {}", clientIp, e);
        }
    }
    
    private void incrementCounter(String key, int ttlSeconds) {
        try {
            String currentValue = redisTemplate.opsForValue().get(key);
            if (currentValue == null) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
            } else {
                redisTemplate.opsForValue().increment(key);
            }
        } catch (Exception e) {
            log.error("Error incrementing counter for key: {}", key, e);
        }
    }
    
    private boolean isCriticalEndpoint(String path) {
        for (String criticalPath : CRITICAL_ENDPOINTS) {
            if (path.startsWith(criticalPath)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isWhitelistedIp(String clientIp) {
        if (whitelistIps == null || whitelistIps.trim().isEmpty()) {
            return false;
        }
        
        String[] ips = whitelistIps.split(",");
        for (String ip : ips) {
            if (clientIp.equals(ip.trim())) {
                return true;
            }
        }
        return false;
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
    
    private boolean isIpBlocked(String clientIp) {
        try {
            String blockKey = "ip_blocked:" + clientIp;
            return redisTemplate.hasKey(blockKey);
        } catch (Exception e) {
            log.error("Error checking IP block status for {}", clientIp, e);
            return false;
        }
    }
    
    private void incrementViolationCount(String clientIp) {
        try {
            String violationKey = "violations:" + clientIp;
            String currentCountStr = redisTemplate.opsForValue().get(violationKey);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            currentCount++;
            redisTemplate.opsForValue().set(violationKey, String.valueOf(currentCount), Duration.ofHours(1));
            
            // Block IP if threshold exceeded
            if (currentCount >= blockThreshold) {
                blockIp(clientIp);
                log.warn("IP {} blocked due to {} violations", clientIp, currentCount);
            }
        } catch (Exception e) {
            log.error("Error incrementing violation count for {}", clientIp, e);
        }
    }
    
    private void blockIp(String clientIp) {
        try {
            String blockKey = "ip_blocked:" + clientIp;
            redisTemplate.opsForValue().set(blockKey, "blocked", Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Error blocking IP {}", clientIp, e);
        }
    }
    
    private Mono<Void> handleIpBlocked(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorMessage = "{\"error\":\"IP Blocked\",\"message\":\"Your IP address has been temporarily blocked due to suspicious activity.\"}";
        DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        
        // Add rate limit headers
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()));
        response.getHeaders().add("Retry-After", "60");
        
        String errorMessage = "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}";
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorMessage.getBytes())));
    }
    
    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}