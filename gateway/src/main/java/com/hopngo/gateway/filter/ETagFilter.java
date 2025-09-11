package com.hopngo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class ETagFilter extends AbstractGatewayFilterFactory<ETagFilter.Config> {

    public ETagFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                var response = exchange.getResponse();
                var headers = response.getHeaders();
                
                // Only add ETag for GET requests and successful responses
                if ("GET".equals(exchange.getRequest().getMethod().name()) && 
                    response.getStatusCode() != null && 
                    response.getStatusCode().is2xxSuccessful()) {
                    
                    // Generate ETag based on response content (simplified approach)
                    String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
                    if (contentType != null && 
                        (contentType.contains("application/json") || 
                         contentType.contains("text/") || 
                         contentType.contains("application/xml"))) {
                        
                        // Generate a simple ETag based on URL and timestamp
                        String path = exchange.getRequest().getPath().value();
                        String etag = DigestUtils.md5DigestAsHex(
                            (path + System.currentTimeMillis() / 60000).getBytes(StandardCharsets.UTF_8)
                        );
                        
                        headers.add(HttpHeaders.ETAG, "\"" + etag + "\"");
                        
                        // Check if client sent If-None-Match header
                        String ifNoneMatch = exchange.getRequest().getHeaders().getFirst(HttpHeaders.IF_NONE_MATCH);
                        if (ifNoneMatch != null && ifNoneMatch.contains(etag)) {
                            response.setStatusCode(HttpStatus.NOT_MODIFIED);
                        }
                    }
                }
            }));
        };
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}