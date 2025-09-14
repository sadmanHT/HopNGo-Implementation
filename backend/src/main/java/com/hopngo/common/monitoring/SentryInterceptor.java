package com.hopngo.common.monitoring;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor to capture HTTP request context for Sentry error reporting
 */
@Component
public class SentryInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SentryInterceptor.class);
    
    private final SentryConfig sentryConfig;
    
    public SentryInterceptor(SentryConfig sentryConfig) {
        this.sentryConfig = sentryConfig;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Set user context if authenticated
            setUserContext();
            
            // Add request context
            addRequestContext(request);
            
            // Add breadcrumb for request
            addRequestBreadcrumb(request);
            
        } catch (Exception e) {
            logger.warn("Failed to set Sentry context for request", e);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        try {
            // Add response context
            addResponseContext(response);
            
            // Log slow requests
            logSlowRequest(request);
            
        } catch (Exception e) {
            logger.warn("Failed to complete Sentry context for request", e);
        } finally {
            // Clear context to prevent memory leaks
            Sentry.clearBreadcrumbs();
        }
    }
    
    private void setUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            
            User user = new User();
            
            // Hash user ID for privacy
            String userId = authentication.getName();
            if (userId != null) {
                user.setId(sentryConfig.hashUserId(userId));
            }
            
            // Add user roles (without sensitive data)
            if (authentication.getAuthorities() != null) {
                user.setData("roles", authentication.getAuthorities().toString());
            }
            
            Sentry.setUser(user);
        }
    }
    
    private void addRequestContext(HttpServletRequest request) {
        Sentry.setTag("http.method", request.getMethod());
        Sentry.setTag("http.url", sanitizeUrl(request.getRequestURL().toString()));
        Sentry.setTag("http.user_agent", request.getHeader("User-Agent"));
        Sentry.setTag("http.remote_addr", getClientIpAddress(request));
        
        // Add custom headers (excluding sensitive ones)
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // Skip sensitive headers
            if (isSafeHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        
        Sentry.setExtra("request.headers", headers);
        Sentry.setExtra("request.query_string", request.getQueryString());
    }
    
    private void addRequestBreadcrumb(HttpServletRequest request) {
        Sentry.addBreadcrumb(
            String.format("%s %s", request.getMethod(), sanitizeUrl(request.getRequestURI())),
            "http.request"
        );
    }
    
    private void addResponseContext(HttpServletResponse response) {
        Sentry.setTag("http.status_code", String.valueOf(response.getStatus()));
        
        // Categorize response status
        int status = response.getStatus();
        if (status >= 400 && status < 500) {
            Sentry.setTag("http.status_class", "4xx");
        } else if (status >= 500) {
            Sentry.setTag("http.status_class", "5xx");
        } else {
            Sentry.setTag("http.status_class", "success");
        }
    }
    
    private void logSlowRequest(HttpServletRequest request) {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > 5000) { // Log requests slower than 5 seconds
                Sentry.addBreadcrumb(
                    String.format("Slow request: %dms", duration),
                    "performance"
                );
                
                Sentry.captureMessage(
                    String.format("Slow request detected: %s %s took %dms", 
                        request.getMethod(), request.getRequestURI(), duration),
                    SentryLevel.WARNING
                );
            }
        }
    }
    
    private String sanitizeUrl(String url) {
        if (url == null) return null;
        
        // Remove sensitive parameters
        return url.replaceAll("([?&])(password|token|key|secret)=[^&]*", "$1$2=***");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean isSafeHeader(String headerName) {
        if (headerName == null) return false;
        
        String lowerName = headerName.toLowerCase();
        
        // Skip sensitive headers
        return !lowerName.contains("authorization") &&
               !lowerName.contains("cookie") &&
               !lowerName.contains("password") &&
               !lowerName.contains("token") &&
               !lowerName.contains("key") &&
               !lowerName.contains("secret");
    }
}