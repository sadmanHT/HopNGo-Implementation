package com.hopngo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                logger.info("JWT Token received: {}...", token.substring(0, Math.min(token.length(), 20)));
                logger.debug("Full JWT Token: {}", token);
                
                // TODO: Replace with actual RS256 JWT validation when AuthService is ready
                // For now, we just log and forward the request
                logger.info("JWT validation placeholder - forwarding request to downstream service");
                
                // Add custom headers for downstream services if needed
                ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Token", token)
                        .build())
                    .build();
                
                return chain.filter(modifiedExchange);
            } else {
                logger.warn("No Authorization header found or invalid format");
                // For now, allow requests without JWT - will be restricted later
                return chain.filter(exchange);
            }
        };
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}