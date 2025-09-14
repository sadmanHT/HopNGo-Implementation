package com.hopngo.emergency.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class UserIdValidationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(UserIdValidationFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Skip validation for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String userId = request.getHeader(USER_ID_HEADER);
        
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("Missing or empty X-User-Id header for request: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing X-User-Id header\"}");
            response.setContentType("application/json");
            return;
        }
        
        // Validate userId format (basic validation)
        if (!isValidUserId(userId)) {
            logger.warn("Invalid X-User-Id format: {} for request: {}", userId, requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid X-User-Id format\"}");
            response.setContentType("application/json");
            return;
        }
        
        // Set authentication context
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        logger.debug("Authenticated user: {} for request: {}", userId, requestURI);
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/actuator/") ||
               requestURI.equals("/emergency/health") ||
               requestURI.startsWith("/v3/api-docs/") ||
               requestURI.startsWith("/swagger-ui/") ||
               requestURI.equals("/swagger-ui.html");
    }
    
    private boolean isValidUserId(String userId) {
        // Basic validation: non-empty, reasonable length, alphanumeric with hyphens
        return userId != null && 
               userId.length() >= 1 && 
               userId.length() <= 255 && 
               userId.matches("^[a-zA-Z0-9_-]+$");
    }
}