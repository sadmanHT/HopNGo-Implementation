package com.hopngo.ai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

@Component
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class UserIdFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Rate limiting configuration
    private static final int HOURLY_QUOTA = 50;
    private static final int DAILY_QUOTA = 200;
    
    public UserIdFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip filter for public endpoints
        if (path.startsWith("/actuator") || path.startsWith("/v3/api-docs") || 
            path.startsWith("/swagger-ui") || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check for X-User-Id header on AI endpoints
        if (path.startsWith("/ai/")) {
            String userId = request.getHeader("X-User-Id");
            
            if (userId == null || userId.trim().isEmpty()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing X-User-Id header\"}");
                return;
            }
            
            // Check user quotas
            if (!checkUserQuota(userId)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }
            
            // Increment usage counters
            incrementUserUsage(userId);
            
            // Set authentication context
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean checkUserQuota(String userId) {
        try {
            String hourlyKey = "quota:hourly:" + userId;
            String dailyKey = "quota:daily:" + userId;
            
            String hourlyCount = redisTemplate.opsForValue().get(hourlyKey);
            String dailyCount = redisTemplate.opsForValue().get(dailyKey);
            
            int hourlyUsage = hourlyCount != null ? Integer.parseInt(hourlyCount) : 0;
            int dailyUsage = dailyCount != null ? Integer.parseInt(dailyCount) : 0;
            
            return hourlyUsage < HOURLY_QUOTA && dailyUsage < DAILY_QUOTA;
        } catch (Exception e) {
            // If Redis is unavailable, allow the request but log the error
            logger.warn("Failed to check user quota for user: " + userId, e);
            return true;
        }
    }
    
    private void incrementUserUsage(String userId) {
        try {
            String hourlyKey = "quota:hourly:" + userId;
            String dailyKey = "quota:daily:" + userId;
            
            // Increment hourly counter with 1-hour expiration
            redisTemplate.opsForValue().increment(hourlyKey);
            redisTemplate.expire(hourlyKey, Duration.ofHours(1));
            
            // Increment daily counter with 24-hour expiration
            redisTemplate.opsForValue().increment(dailyKey);
            redisTemplate.expire(dailyKey, Duration.ofDays(1));
        } catch (Exception e) {
            // Log error but don't fail the request
            logger.warn("Failed to increment user usage for user: " + userId, e);
        }
    }
}