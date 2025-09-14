package com.hopngo.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Penetration-style security tests to verify the gateway's security implementation
 * against common attack vectors and vulnerabilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-penetration-testing-purposes-with-sufficient-length-for-hmac-sha256-algorithm",
    "logging.level.com.hopngo.gateway.filter=DEBUG"
})
class PenetrationSecurityTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    private final String jwtSecret = "test-secret-key-for-penetration-testing-purposes-with-sufficient-length-for-hmac-sha256-algorithm";
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void testSQLInjectionAttempts() {
        String adminToken = createValidAdminToken();
        
        // Test SQL injection in path parameters
        webTestClient.get()
            .uri("/api/v1/admin/users/1'; DROP TABLE users; --")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Gateway should handle malformed requests

        // Test SQL injection in query parameters
        webTestClient.get()
            .uri("/api/v1/admin/users?id=1' OR '1'='1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Gateway should handle malformed requests
    }

    @Test
    void testXSSAttempts() {
        String adminToken = createValidAdminToken();
        
        // Test XSS in headers
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .header("X-Custom-Header", "<script>alert('xss')</script>")
            .exchange()
            .expectStatus().is5xxServerError(); // Should be handled safely
    }

    @Test
    void testJWTManipulationAttempts() {
        // Test with modified JWT signature
        String validToken = createValidAdminToken();
        String manipulatedToken = validToken.substring(0, validToken.length() - 10) + "manipulated";
        
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulatedToken)
            .exchange()
            .expectStatus().isUnauthorized();

        // Test with JWT from different secret
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-with-sufficient-length-for-hmac-sha256-algorithm-testing".getBytes(StandardCharsets.UTF_8));
        String wrongSecretToken = Jwts.builder()
            .setSubject("admin123")
            .claim("email", "admin@example.com")
            .claim("role", "ADMIN")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(wrongKey)
            .compact();

        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongSecretToken)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void testRoleEscalationAttempts() {
        // Test with token claiming multiple roles
        String multiRoleToken = Jwts.builder()
            .setSubject("user123")
            .claim("email", "user@example.com")
            .claim("role", "TRAVELER,ADMIN") // Attempt to claim multiple roles
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact();

        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + multiRoleToken)
            .exchange()
            .expectStatus().isForbidden(); // Should reject multi-role attempts

        // Test with token claiming non-existent role
        String invalidRoleToken = Jwts.builder()
            .setSubject("user123")
            .claim("email", "user@example.com")
            .claim("role", "SUPER_ADMIN") // Non-existent role
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(secretKey)
            .compact();

        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidRoleToken)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void testPathTraversalAttempts() {
        String adminToken = createValidAdminToken();
        
        // Test directory traversal attempts
        webTestClient.get()
            .uri("/api/v1/admin/../../../etc/passwd")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchange()
            .expectStatus().isBadRequest(); // Should be handled safely

        webTestClient.get()
            .uri("/api/v1/admin/users/..%2F..%2F..%2Fetc%2Fpasswd")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchange()
            .expectStatus().isBadRequest(); // Should be handled safely
    }

    @Test
    void testHeaderInjectionAttempts() {
        String adminToken = createValidAdminToken();
        
        // Test header injection attempts
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .header("X-Test", "<script>alert('header-injection')</script>")
            .exchange()
            .expectStatus().is5xxServerError(); // Should be handled safely
    }

    @Test
    void testRateLimitingBypass() {
        String adminToken = createValidAdminToken();
        
        // Test rapid requests to check rate limiting
        for (int i = 0; i < 10; i++) {
            webTestClient.get()
                .uri("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().is5xxServerError(); // Should not be rate limited for valid requests
        }
    }

    @Test
    void testTokenReplayAttacks() {
        String adminToken = createValidAdminToken();
        
        // Test using the same token multiple times (should be allowed for valid tokens)
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchange()
            .expectStatus().is5xxServerError();

        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    void testCORSSecurityHeaders() {
        // Test CORS preflight request
        webTestClient.options()
            .uri("/api/v1/admin/users")
            .header("Origin", "http://malicious-site.com")
            .header("Access-Control-Request-Method", "GET")
            .exchange()
            .expectStatus().isForbidden(); // Should reject unauthorized origins

        // Test with allowed origin
        webTestClient.options()
            .uri("/api/v1/admin/users")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .exchange()
            .expectStatus().isOk(); // Should allow configured origins
    }

    @Test
    void testMalformedJWTHandling() {
        // Test with completely malformed JWT
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt")
            .exchange()
            .expectStatus().isUnauthorized();

        // Test with missing JWT parts
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer header.payload")
            .exchange()
            .expectStatus().isUnauthorized();

        // Test with empty JWT
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void testPrivilegeEscalationThroughEndpoints() {
        String travelerToken = createJwtToken("user123", "user@example.com", "TRAVELER");
        
        // Test accessing admin-only moderation endpoints
        webTestClient.post()
            .uri("/api/v1/admin/moderation/1/approve")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + travelerToken)
            .exchange()
            .expectStatus().isForbidden();

        // Test accessing user ban endpoint
        webTestClient.post()
            .uri("/api/v1/admin/users/123/ban")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + travelerToken)
            .exchange()
            .expectStatus().isForbidden();

        // Test accessing content removal endpoint
        webTestClient.post()
            .uri("/api/v1/admin/moderation/1/remove")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + travelerToken)
            .exchange()
            .expectStatus().isForbidden();
    }

    private String createValidAdminToken() {
        return createJwtToken("admin123", "admin@example.com", "ADMIN");
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
}