package com.hopngo.gateway.security;

import com.hopngo.gateway.filter.JwtAuthenticationFilter;
import com.hopngo.gateway.filter.RoleAuthorizationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

class SecurityIntegrationTest {

    private final String jwtSecret = "test-secret-key-for-security-testing-purposes";
    private SecretKey secretKey;
    private MockGatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        filterChain = new MockGatewayFilterChain();
    }

    @Test
    void testJwtAuthenticationFilterWithValidToken() {
        // Create valid JWT token
        String validToken = createJwtToken("user123", "user@example.com", "ADMIN");
        
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // Create filter with proper configuration
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter();
        jwtFilter.setJwtSecret(jwtSecret);
        
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());
        
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();
        
        // Verify that user headers were added
        assert "user123".equals(exchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assert "user@example.com".equals(exchange.getRequest().getHeaders().getFirst("X-User-Email"));
        assert "ADMIN".equals(exchange.getRequest().getHeaders().getFirst("X-User-Role"));
        assert filterChain.wasCalled();
    }

    @Test
    void testJwtAuthenticationFilterWithInvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter();
        jwtFilter.setJwtSecret(jwtSecret);
        
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());
        
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();
        
        // Verify that response status is UNAUTHORIZED
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        assert !filterChain.wasCalled();
    }

    @Test
    void testJwtAuthenticationFilterBypassesPublicEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/auth/login")
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter();
        jwtFilter.setJwtSecret(jwtSecret);
        
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());
        
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();
        
        // Verify that the filter chain was called (meaning authentication was bypassed)
        assert filterChain.wasCalled();
    }

    @Test
    void testRoleAuthorizationFilterAllowsAdminRole() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/admin/users")
            .header("X-User-Role", "ADMIN")
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        RoleAuthorizationFilter roleFilter = new RoleAuthorizationFilter();
        GatewayFilter filter = roleFilter.apply(new RoleAuthorizationFilter.Config());
        
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();
        
        // Verify that the request was allowed through
        assert filterChain.wasCalled();
    }

    @Test
    void testRoleAuthorizationFilterBlocksTravelerFromAdminEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/admin/users")
            .header("X-User-Role", "TRAVELER")
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        RoleAuthorizationFilter roleFilter = new RoleAuthorizationFilter();
        GatewayFilter filter = roleFilter.apply(new RoleAuthorizationFilter.Config());
        
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();
        
        // Verify that response status is FORBIDDEN
        assert exchange.getResponse().getStatusCode() == HttpStatus.FORBIDDEN;
        assert !filterChain.wasCalled();
    }

    @Test
    void testRoleAuthorizationFilterAllowsServiceProviderForProviderEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest
            .post("/market/provider/services")
            .header("X-User-Role", "SERVICE_PROVIDER")
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        RoleAuthorizationFilter roleFilter = new RoleAuthorizationFilter();
        GatewayFilter filter = roleFilter.apply(new RoleAuthorizationFilter.Config());
        
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();
        
        // Verify that the request was allowed through
        assert filterChain.wasCalled();
    }

    private String createJwtToken(String userId, String email, String role) {
        return Jwts.builder()
            .setSubject(userId)
            .claim("email", email)
            .claim("role", role)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact();
    }

    private static class MockGatewayFilterChain implements GatewayFilterChain {
        private boolean called = false;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            called = true;
            return Mono.empty();
        }

        public boolean wasCalled() {
            return called;
        }
    }
}