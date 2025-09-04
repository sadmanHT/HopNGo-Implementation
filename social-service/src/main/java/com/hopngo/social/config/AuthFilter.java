package com.hopngo.social.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ID_ATTRIBUTE = "userId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip auth for actuator endpoints, swagger, and API docs
        if (requestPath.startsWith("/actuator") || 
            requestPath.startsWith("/swagger-ui") || 
            requestPath.startsWith("/api-docs") ||
            requestPath.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract user ID from header
        String userId = request.getHeader(USER_ID_HEADER);
        
        if (userId == null || userId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Unauthorized\", \"message\": \"Missing or invalid user authentication\"}"
            );
            return;
        }
        
        // Set user ID as request attribute for controllers to use
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        
        filterChain.doFilter(request, response);
    }
    
    public static String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute(USER_ID_ATTRIBUTE);
    }
}