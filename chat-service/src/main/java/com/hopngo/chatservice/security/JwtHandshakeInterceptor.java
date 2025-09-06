package com.hopngo.chatservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtHandshakeInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    
    @Value("${app.jwt.public-key}")
    private String publicKeyString;
    
    // Note: WebSocket handshake interception will be implemented once Spring WebSocket dependencies are properly resolved
    // For now, this is a placeholder to allow compilation
    
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warn("JWT token is null or empty");
                return false;
            }
            
            // Remove Bearer prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Validate JWT token
            PublicKey publicKey = getPublicKey();
            Claims claims = Jwts.parser()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Extract user information from claims
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            
            logger.info("JWT validation successful for user: {} ({})", username, userId);
            return true;
            
        } catch (Exception e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    private PublicKey getPublicKey() throws Exception {
        // Remove PEM headers and decode
        String publicKeyPEM = publicKeyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}