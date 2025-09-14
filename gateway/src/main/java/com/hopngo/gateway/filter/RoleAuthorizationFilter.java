package com.hopngo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleAuthorizationFilter extends AbstractGatewayFilterFactory<RoleAuthorizationFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleAuthorizationFilter.class);
    
    public RoleAuthorizationFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            String method = request.getMethod().name();
            
            // Check if this endpoint requires specific roles
            List<String> requiredRoles = getRequiredRoles(path, method);
            
            if (requiredRoles.isEmpty()) {
                // No specific role required, continue
                return chain.filter(exchange);
            }
            
            // Get user role from headers (set by JwtAuthenticationFilter)
            String userRole = request.getHeaders().getFirst("X-User-Role");
            
            if (userRole == null) {
                logger.warn("No user role found in headers for path: {}", path);
                return handleForbidden(exchange, "No user role found");
            }
            
            // Check if user has required role
            if (!requiredRoles.contains(userRole)) {
                logger.warn("User with role '{}' attempted to access '{}' which requires roles: {}", 
                    userRole, path, requiredRoles);
                return handleForbidden(exchange, "Insufficient privileges");
            }
            
            logger.debug("Role authorization passed for user with role '{}' accessing '{}'", userRole, path);
            return chain.filter(exchange);
        };
    }
    
    private List<String> getRequiredRoles(String path, String method) {
        // Admin-only endpoints
        if (path.startsWith("/api/v1/admin/")) {
            return Arrays.asList("ADMIN");
        }
        
        // Specific admin operations across services
        if (path.matches(".*/users/.*/ban") || 
            path.matches(".*/moderation/.*/remove") ||
            path.matches(".*/admin/.*")) {
            return Arrays.asList("ADMIN");
        }
        
        // Service provider specific endpoints
        if (path.startsWith("/market/provider/") && 
            ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) {
            return Arrays.asList("SERVICE_PROVIDER", "ADMIN");
        }
        
        // Traveler booking endpoints (POST/PUT/DELETE)
        if (path.startsWith("/bookings/") && 
            ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) {
            return Arrays.asList("TRAVELER", "SERVICE_PROVIDER", "ADMIN");
        }
        
        // No specific role required
        return Arrays.asList();
    }
    
    private Mono<Void> handleForbidden(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\": \"Access denied\", \"message\": \"%s\"}", reason);
        return response.writeWith(reactor.core.publisher.Mono.just(
            response.bufferFactory().wrap(body.getBytes())
        ));
    }
    
    public static class Config {
        private List<String> requiredRoles;
        
        public List<String> getRequiredRoles() {
            return requiredRoles;
        }
        
        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }
}