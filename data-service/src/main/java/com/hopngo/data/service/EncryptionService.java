package com.hopngo.data.service;

import com.hopngo.data.model.EncryptionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Encryption Service for HopNGo Platform
 * Handles data encryption, key management, and secure operations
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 256;
    
    private static final String KEY_PREFIX = "enc_key:";
    private static final String METADATA_PREFIX = "enc_meta:";
    
    @Value("${encryption.master-key:}")
    private String masterKeyBase64;
    
    @Value("${encryption.key-rotation-enabled:true}")
    private boolean keyRotationEnabled;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom;
    private final Map<String, SecretKey> keyCache;
    
    public EncryptionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
        this.keyCache = new HashMap<>();
    }

    /**
     * Encrypt data with specified algorithm
     */
    public EncryptionMetadata encryptData(byte[] data, String algorithm) {
        try {
            if (!AES_GCM_ALGORITHM.equals(getJavaAlgorithm(algorithm))) {
                throw new IllegalArgumentException("Unsupported encryption algorithm: " + algorithm);
            }

            // Generate or retrieve encryption key
            String keyId = generateKeyId();
            SecretKey secretKey = generateDataEncryptionKey();
            
            // Store the key securely
            storeEncryptionKey(keyId, secretKey);
            
            // Generate IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Encrypt the data
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] encryptedData = cipher.doFinal(data);
            
            // Combine IV and encrypted data
            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
            
            EncryptionMetadata metadata = new EncryptionMetadata();
            metadata.setKeyId(keyId);
            metadata.setAlgorithm(algorithm);
            metadata.setEncryptedData(result);
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setDataSize(data.length);
            metadata.setEncryptedSize(result.length);
            
            // Store metadata
            storeEncryptionMetadata(keyId, metadata);
            
            logger.debug("Data encrypted successfully with key: {}", keyId);
            return metadata;
            
        } catch (Exception e) {
            logger.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data using stored key
     */
    public byte[] decryptData(byte[] encryptedData, String keyId, String algorithm) {
        try {
            if (!AES_GCM_ALGORITHM.equals(getJavaAlgorithm(algorithm))) {
                throw new IllegalArgumentException("Unsupported encryption algorithm: " + algorithm);
            }

            // Retrieve the encryption key
            SecretKey secretKey = retrieveEncryptionKey(keyId);
            if (secretKey == null) {
                throw new IllegalArgumentException("Encryption key not found: " + keyId);
            }
            
            // Extract IV and encrypted data
            if (encryptedData.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
            
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            
            // Decrypt the data
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] decryptedData = cipher.doFinal(cipherText);
            
            logger.debug("Data decrypted successfully with key: {}", keyId);
            return decryptedData;
            
        } catch (Exception e) {
            logger.error("Failed to decrypt data with key: {}", keyId, e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate a new data encryption key
     */
    public String generateNewKey(String algorithm) {
        try {
            String keyId = generateKeyId();
            SecretKey secretKey = generateDataEncryptionKey();
            
            storeEncryptionKey(keyId, secretKey);
            
            EncryptionMetadata metadata = new EncryptionMetadata();
            metadata.setKeyId(keyId);
            metadata.setAlgorithm(algorithm);
            metadata.setCreatedAt(LocalDateTime.now());
            
            storeEncryptionMetadata(keyId, metadata);
            
            logger.info("Generated new encryption key: {}", keyId);
            return keyId;
            
        } catch (Exception e) {
            logger.error("Failed to generate new encryption key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Rotate encryption key
     */
    public String rotateKey(String oldKeyId, String algorithm) {
        if (!keyRotationEnabled) {
            logger.warn("Key rotation is disabled");
            return oldKeyId;
        }
        
        try {
            // Generate new key
            String newKeyId = generateNewKey(algorithm);
            
            // Mark old key for deprecation (don't delete immediately for existing data)
            markKeyAsDeprecated(oldKeyId);
            
            logger.info("Rotated encryption key from {} to {}", oldKeyId, newKeyId);
            return newKeyId;
            
        } catch (Exception e) {
            logger.error("Failed to rotate encryption key: {}", oldKeyId, e);
            throw new RuntimeException("Key rotation failed", e);
        }
    }

    /**
     * Securely delete encryption key
     */
    public void secureDeleteKey(String keyId) {
        try {
            // Remove from cache
            keyCache.remove(keyId);
            
            // Remove from Redis
            redisTemplate.delete(KEY_PREFIX + keyId);
            redisTemplate.delete(METADATA_PREFIX + keyId);
            
            logger.info("Securely deleted encryption key: {}", keyId);
            
        } catch (Exception e) {
            logger.error("Failed to securely delete key: {}", keyId, e);
        }
    }

    /**
     * Get key metadata
     */
    public EncryptionMetadata getKeyMetadata(String keyId) {
        try {
            return (EncryptionMetadata) redisTemplate.opsForValue().get(METADATA_PREFIX + keyId);
        } catch (Exception e) {
            logger.error("Failed to retrieve key metadata: {}", keyId, e);
            return null;
        }
    }

    /**
     * List all active encryption keys
     */
    public List<String> listActiveKeys() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            List<String> keyIds = new ArrayList<>();
            
            if (keys != null) {
                for (String key : keys) {
                    String keyId = key.substring(KEY_PREFIX.length());
                    EncryptionMetadata metadata = getKeyMetadata(keyId);
                    
                    if (metadata != null && !metadata.isDeprecated()) {
                        keyIds.add(keyId);
                    }
                }
            }
            
            return keyIds;
            
        } catch (Exception e) {
            logger.error("Failed to list active keys", e);
            return new ArrayList<>();
        }
    }

    /**
     * Cleanup deprecated keys
     */
    public int cleanupDeprecatedKeys(int olderThanDays) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
            Set<String> keys = redisTemplate.keys(METADATA_PREFIX + "*");
            int deletedCount = 0;
            
            if (keys != null) {
                for (String metaKey : keys) {
                    EncryptionMetadata metadata = (EncryptionMetadata) redisTemplate.opsForValue().get(metaKey);
                    
                    if (metadata != null && metadata.isDeprecated() && 
                        metadata.getDeprecatedAt() != null && 
                        metadata.getDeprecatedAt().isBefore(cutoffDate)) {
                        
                        String keyId = metaKey.substring(METADATA_PREFIX.length());
                        secureDeleteKey(keyId);
                        deletedCount++;
                    }
                }
            }
            
            logger.info("Cleaned up {} deprecated encryption keys", deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Failed to cleanup deprecated keys", e);
            return 0;
        }
    }

    /**
     * Get encryption statistics
     */
    public Map<String, Object> getEncryptionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Set<String> allKeys = redisTemplate.keys(KEY_PREFIX + "*");
            int totalKeys = allKeys != null ? allKeys.size() : 0;
            
            List<String> activeKeys = listActiveKeys();
            int deprecatedKeys = totalKeys - activeKeys.size();
            
            stats.put("total_keys", totalKeys);
            stats.put("active_keys", activeKeys.size());
            stats.put("deprecated_keys", deprecatedKeys);
            stats.put("key_rotation_enabled", keyRotationEnabled);
            
        } catch (Exception e) {
            logger.error("Failed to get encryption statistics", e);
        }
        
        return stats;
    }

    // Private helper methods

    private String generateKeyId() {
        return "key_" + UUID.randomUUID().toString().replace("-", "");
    }

    private SecretKey generateDataEncryptionKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_LENGTH);
        return keyGenerator.generateKey();
    }

    private void storeEncryptionKey(String keyId, SecretKey secretKey) {
        try {
            // Encrypt the key with master key before storing
            byte[] encryptedKey = encryptKeyWithMasterKey(secretKey.getEncoded());
            
            // Store in Redis with expiration (optional)
            redisTemplate.opsForValue().set(KEY_PREFIX + keyId, encryptedKey, 365, TimeUnit.DAYS);
            
            // Cache in memory for performance
            keyCache.put(keyId, secretKey);
            
        } catch (Exception e) {
            logger.error("Failed to store encryption key: {}", keyId, e);
            throw new RuntimeException("Key storage failed", e);
        }
    }

    private SecretKey retrieveEncryptionKey(String keyId) {
        try {
            // Check cache first
            if (keyCache.containsKey(keyId)) {
                return keyCache.get(keyId);
            }
            
            // Retrieve from Redis
            byte[] encryptedKey = (byte[]) redisTemplate.opsForValue().get(KEY_PREFIX + keyId);
            if (encryptedKey == null) {
                return null;
            }
            
            // Decrypt with master key
            byte[] keyBytes = decryptKeyWithMasterKey(encryptedKey);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            
            // Cache for future use
            keyCache.put(keyId, secretKey);
            
            return secretKey;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve encryption key: {}", keyId, e);
            return null;
        }
    }

    private void storeEncryptionMetadata(String keyId, EncryptionMetadata metadata) {
        redisTemplate.opsForValue().set(METADATA_PREFIX + keyId, metadata, 365, TimeUnit.DAYS);
    }

    private void markKeyAsDeprecated(String keyId) {
        EncryptionMetadata metadata = getKeyMetadata(keyId);
        if (metadata != null) {
            metadata.setDeprecated(true);
            metadata.setDeprecatedAt(LocalDateTime.now());
            storeEncryptionMetadata(keyId, metadata);
        }
    }

    private String getJavaAlgorithm(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "AES-256-GCM":
                return AES_GCM_ALGORITHM;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private byte[] encryptKeyWithMasterKey(byte[] keyData) throws Exception {
        // Simplified implementation - in production, use proper key derivation
        if (masterKeyBase64 == null || masterKeyBase64.isEmpty()) {
            // For demo purposes, return as-is (NOT SECURE)
            logger.warn("Master key not configured - keys stored unencrypted!");
            return keyData;
        }
        
        byte[] masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);
        SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "AES");
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmSpec);
        
        byte[] encryptedData = cipher.doFinal(keyData);
        
        // Combine IV and encrypted data
        byte[] result = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
        
        return result;
    }

    private byte[] decryptKeyWithMasterKey(byte[] encryptedKeyData) throws Exception {
        if (masterKeyBase64 == null || masterKeyBase64.isEmpty()) {
            // For demo purposes, return as-is (NOT SECURE)
            return encryptedKeyData;
        }
        
        byte[] masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);
        SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "AES");
        
        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] cipherText = new byte[encryptedKeyData.length - GCM_IV_LENGTH];
        
        System.arraycopy(encryptedKeyData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedKeyData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
        
        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmSpec);
        
        return cipher.doFinal(cipherText);
    }
}