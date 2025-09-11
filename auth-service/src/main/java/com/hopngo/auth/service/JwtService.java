package com.hopngo.auth.service;

import com.hopngo.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    @Value("${jwt.private-key-path:}")
    private String privateKeyPath;
    
    @Value("${jwt.public-key-path:}")
    private String publicKeyPath;
    
    @Value("${jwt.rsa.private-key:}")
    private String privateKeyEnv;
    
    @Value("${jwt.rsa.public-key:}")
    private String publicKeyEnv;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    private final ResourceLoader resourceLoader;
    
    private PrivateKey privateKey;
    private PublicKey publicKey;
    
    public JwtService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @PostConstruct
    public void init() {
        try {
            loadKeys();
        } catch (Exception e) {
            logger.error("Failed to load RSA keys", e);
            throw new RuntimeException("Failed to initialize JWT service", e);
        }
    }
    
    private void loadKeys() throws Exception {
        // Priority 1: Load from environment variables (production)
        if (privateKeyEnv != null && !privateKeyEnv.trim().isEmpty() && 
            publicKeyEnv != null && !publicKeyEnv.trim().isEmpty()) {
            
            logger.info("Loading RSA keys from environment variables");
            loadKeysFromEnvironment();
            return;
        }
        
        // Priority 2: Load from files (legacy support)
        if (privateKeyPath != null && !privateKeyPath.trim().isEmpty() && 
            publicKeyPath != null && !publicKeyPath.trim().isEmpty()) {
            
            logger.info("Loading RSA keys from files");
            loadKeysFromFiles();
            return;
        }
        
        // Priority 3: Generate temporary keys for development
        logger.warn("No RSA keys configured. Generating temporary keys for development. " +
                   "Set JWT_RSA_PRIVATE_KEY and JWT_RSA_PUBLIC_KEY environment variables in production!");
        generateTemporaryKeys();
    }
    
    private void loadKeysFromEnvironment() throws Exception {
        // Clean and decode private key
        String cleanPrivateKey = cleanKeyContent(privateKeyEnv);
        if (cleanPrivateKey.isEmpty()) {
            throw new RuntimeException("Private key environment variable is empty or invalid");
        }
        
        byte[] privateKeyBytes = Base64.getDecoder().decode(cleanPrivateKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(privateKeySpec);
        
        // Clean and decode public key
        String cleanPublicKey = cleanKeyContent(publicKeyEnv);
        if (cleanPublicKey.isEmpty()) {
            throw new RuntimeException("Public key environment variable is empty or invalid");
        }
        
        byte[] publicKeyBytes = Base64.getDecoder().decode(cleanPublicKey);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = keyFactory.generatePublic(publicKeySpec);
        
        logger.info("Successfully loaded RSA keys from environment variables");
    }
    
    private void loadKeysFromFiles() throws Exception {
        // Load private key from file
        Resource privateKeyResource = resourceLoader.getResource(privateKeyPath);
        if (!privateKeyResource.exists()) {
            throw new RuntimeException("Private key file not found: " + privateKeyPath);
        }
        
        String privateKeyContent = new String(privateKeyResource.getInputStream().readAllBytes());
        String cleanPrivateKey = cleanKeyContent(privateKeyContent);
        
        if (cleanPrivateKey.isEmpty() || isPlaceholder(cleanPrivateKey)) {
            generateTemporaryKeys();
            return;
        }
        
        byte[] privateKeyBytes = Base64.getDecoder().decode(cleanPrivateKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(privateKeySpec);
        
        // Load public key from file
        Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
        if (!publicKeyResource.exists()) {
            throw new RuntimeException("Public key file not found: " + publicKeyPath);
        }
        
        String publicKeyContent = new String(publicKeyResource.getInputStream().readAllBytes());
        String cleanPublicKey = cleanKeyContent(publicKeyContent);
        
        if (!cleanPublicKey.isEmpty() && !isPlaceholder(cleanPublicKey)) {
            byte[] publicKeyBytes = Base64.getDecoder().decode(cleanPublicKey);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            this.publicKey = keyFactory.generatePublic(publicKeySpec);
        }
        
        logger.info("Successfully loaded RSA keys from files");
    }
    
    private void generateTemporaryKeys() {
        logger.warn("Generating temporary RSA key pair for development. " +
                   "This should NOT be used in production!");
        var keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }
    
    private String cleanKeyContent(String keyContent) {
        return keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("# TODO: Replace with actual RSA private key", "")
                .replace("# TODO: Replace with actual RSA public key", "")
                .replace("# This is a placeholder - in production, use K8s Secrets", "")
                .replace("# Generate with: openssl genrsa -out private_key.pem 2048", "")
                .replace("# Extract from private key: openssl rsa -in private_key.pem -pubout -out public_key.pem", "")
                .replaceAll("\\s", "");
    }
    
    private boolean isPlaceholder(String content) {
        return content.contains("PLACEHOLDER") || content.isEmpty();
    }
    
    /**
     * Generate JWT token for user
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRole().name());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtExpiration, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().toString())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
    
    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return Long.parseLong(subject);
    }
    
    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    
    /**
     * Extract roles from token
     */
    public String extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", String.class));
    }
    
    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.resolve(claims);
    }
    
    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.error("Failed to parse JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Validate token
     */
    public boolean validateToken(String token, User user) {
        try {
            final Long userId = extractUserId(token);
            return (userId.equals(user.getId()) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }
    
    /**
     * Validate token without user context
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }
    
    /**
     * Get public key for external validation
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }
    
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}