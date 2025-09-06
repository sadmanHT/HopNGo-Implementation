package com.hopngo.tripplanning.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserContextFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip validation for health checks and public endpoints
        if (isPublicEndpoint(requestURI, method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String userId = request.getHeader(USER_ID_HEADER);
        
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("Missing or empty X-User-Id header for request: {} {}", method, requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{");
            response.getWriter().write("\"code\": \"MISSING_USER_ID\",");
            response.getWriter().write("\"message\": \"X-User-Id header is required\",");
            response.getWriter().write("\"timestamp\": \"" + java.time.OffsetDateTime.now() + "\"");
            response.getWriter().write("}");
            return;
        }
        
        // Validate userId format (should be a valid UUID or user identifier)
        if (!isValidUserId(userId)) {
            logger.warn("Invalid X-User-Id format: {} for request: {} {}", userId, method, requestURI);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{");
            response.getWriter().write("\"code\": \"INVALID_USER_ID\",");
            response.getWriter().write("\"message\": \"X-User-Id header has invalid format\",");
            response.getWriter().write("\"timestamp\": \"" + java.time.OffsetDateTime.now() + "\"");
            response.getWriter().write("}");
            return;
        }
        
        // Set user context for the request
        UserContext.setUserId(userId);
        
        try {
            logger.debug("Processing request for user: {} - {} {}", userId, method, requestURI);
            filterChain.doFilter(request, response);
        } finally {
            // Clean up user context after request processing
            UserContext.clear();
        }
    }

    /**
     * Check if the endpoint is public and doesn't require authentication
     */
    private boolean isPublicEndpoint(String requestURI, String method) {
        // Health check endpoints
        if (requestURI.startsWith("/actuator/")) {
            return true;
        }
        
        // OpenAPI/Swagger documentation
        if (requestURI.startsWith("/v3/api-docs") || 
            requestURI.startsWith("/swagger-ui") ||
            requestURI.equals("/swagger-ui.html")) {
            return true;
        }
        
        // Root endpoint
        if (requestURI.equals("/") && "GET".equals(method)) {
            return true;
        }
        
        return false;
    }

    /**
     * Validate userId format
     * For now, we accept any non-empty string, but this can be enhanced
     * to validate UUID format or other specific patterns
     */
    private boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - can be enhanced based on requirements
        String trimmedUserId = userId.trim();
        
        // Check length (reasonable bounds)
        if (trimmedUserId.length() < 1 || trimmedUserId.length() > 100) {
            return false;
        }
        
        // Check for basic format (alphanumeric, hyphens, underscores)
        return trimmedUserId.matches("^[a-zA-Z0-9_-]+$");
    }
}