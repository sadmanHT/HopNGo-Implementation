package com.hopngo.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Circuit Breaker Filter for AI Service with fallback responses
 */
@Component
public class CircuitBreakerFilter extends AbstractGatewayFilterFactory<CircuitBreakerFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    public CircuitBreakerFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String serviceName = config.getName();
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
            
            logger.debug("Applying circuit breaker for service: {}, state: {}", 
                serviceName, circuitBreaker.getState());
            
            return chain.filter(exchange)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    logger.warn("Circuit breaker triggered for service: {} - Error: {}", 
                        serviceName, throwable.getMessage());
                    return handleFallback(exchange, serviceName, throwable);
                });
        };
    }
    
    private Mono<Void> handleFallback(ServerWebExchange exchange, String serviceName, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Set appropriate status code based on error type
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            response.setStatusCode(HttpStatus.REQUEST_TIMEOUT);
        } else {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("X-Fallback-Reason", "circuit-breaker-open");
        response.getHeaders().add("X-Service-Name", serviceName);
        
        String fallbackResponse = createFallbackResponse(serviceName, throwable);
        DataBuffer buffer = response.bufferFactory().wrap(fallbackResponse.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
    
    private String createFallbackResponse(String serviceName, Throwable throwable) {
        if ("ai-service".equals(serviceName)) {
            return createAIServiceFallback(throwable);
        }
        
        // Generic fallback for other services
        return String.format("{"
            + "\"error\": \"Service Unavailable\","
            + "\"message\": \"The %s is currently unavailable. Please try again later.\","
            + "\"service\": \"%s\","
            + "\"timestamp\": \"%s\","
            + "\"fallback\": true"
            + "}", serviceName, serviceName, java.time.Instant.now().toString());
    }
    
    private String createAIServiceFallback(Throwable throwable) {
        String errorType = throwable instanceof java.util.concurrent.TimeoutException ? "timeout" : "circuit-breaker";
        
        return "{"
            + "\"error\": \"AI Service Unavailable\","
            + "\"message\": \"AI service is currently experiencing issues. Using fallback response.\","
            + "\"fallback\": true,"
            + "\"errorType\": \"" + errorType + "\","
            + "\"timestamp\": \"" + java.time.Instant.now().toString() + "\","
            + "\"suggestions\": ["
            + "\"Please try your request again in a few moments\","
            + "\"Consider simplifying your query if the issue persists\","
            + "\"Contact support if you continue experiencing problems\""
            + "],"
            + "\"alternativeEndpoints\": {"
            + "\"search\": \"/search/basic\","
            + "\"recommendations\": \"/trips/popular\""
            + "}"
            + "}";
    }
    
    public static class Config {
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}