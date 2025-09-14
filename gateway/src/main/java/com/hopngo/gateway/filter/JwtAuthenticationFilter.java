package com.hopngo.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    
    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/health",
        "/actuator/health"
    );
    
    @Value("${jwt.secret:default-secret-key}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }
    
    // Setter for testing purposes
    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // Check if this is a public endpoint
            if (isPublicEndpoint(path)) {
                logger.debug("Public endpoint accessed: {}", path);
                return chain.filter(exchange);
            }
            
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("No Authorization header found or invalid format for path: {}", path);
                return unauthorizedResponse(exchange);
            }
            
            String token = authHeader.substring(BEARER_PREFIX.length());
            
            try {
                Claims claims = validateJwtToken(token);
                
                // Extract user information from JWT claims
                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                
                logger.debug("JWT validated for user: {} with role: {}", userId, role);
                
                // Add user information to headers for downstream services
                ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                        .build())
                    .build();
                
                return chain.filter(modifiedExchange);
                
            } catch (Exception e) {
                logger.error("JWT validation failed: {}", e.getMessage());
                return unauthorizedResponse(exchange);
            }
        };
    }
    
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
    
    private Claims validateJwtToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}