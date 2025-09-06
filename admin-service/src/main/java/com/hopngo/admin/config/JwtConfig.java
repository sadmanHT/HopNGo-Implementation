package com.hopngo.admin.config;

import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);
    
    @Value("${jwt.public-key-path}")
    private String publicKeyPath;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    private final ResourceLoader resourceLoader;
    
    private RSAPublicKey publicKey;
    
    public JwtConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @PostConstruct
    public void init() {
        try {
            loadPublicKey();
        } catch (Exception e) {
            logger.error("Failed to load RSA public key", e);
            throw new RuntimeException("Failed to initialize JWT config", e);
        }
    }
    
    private void loadPublicKey() throws Exception {
        // Load public key
        Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
        if (!publicKeyResource.exists()) {
            logger.warn("Public key file not found: {}. Using temporary key for development.", publicKeyPath);
            // Generate a temporary key pair for development
            var keyPair = Keys.keyPairFor(io.jsonwebtoken.SignatureAlgorithm.RS256);
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            return;
        }
        
        String publicKeyContent = new String(publicKeyResource.getInputStream().readAllBytes())
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("# TODO: Replace with actual RSA public key", "")
                .replace("# This is a placeholder - in production, use K8s Secrets", "")
                .replace("# Generate with: openssl rsa -in private_key.pem -pubout -out public_key.pem", "")
                .replace("PLACEHOLDER_RSA_PUBLIC_KEY_CONTENT", "")
                .replaceAll("\\s", "");
        
        // For now, create a temporary key since we have placeholders
        if (publicKeyContent.isEmpty() || publicKeyContent.equals("PLACEHOLDER_RSA_PUBLIC_KEY_CONTENT")) {
            logger.warn("Using temporary RSA key pair for development. Replace with actual keys in production!");
            // Generate a temporary key pair for development
            var keyPair = Keys.keyPairFor(io.jsonwebtoken.SignatureAlgorithm.RS256);
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            return;
        }
        
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        
        logger.info("Successfully loaded RSA public key for JWT validation");
    }
    
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }
    
    public String getIssuer() {
        return issuer;
    }
}