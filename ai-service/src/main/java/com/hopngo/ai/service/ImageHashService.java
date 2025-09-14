package com.hopngo.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class ImageHashService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageHashService.class);
    private static final String HASH_KEY_PREFIX = "image_hash:";
    private static final String URL_TO_HASH_PREFIX = "url_to_hash:";
    private static final long CACHE_TTL_HOURS = 24 * 7; // 1 week
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * Check for duplicate images and return list of duplicate URLs
     * @param mediaUrls List of image URLs to check
     * @return List of URLs that are duplicates of existing images
     */
    public List<String> checkForDuplicates(List<String> mediaUrls) {
        List<String> duplicates = new ArrayList<>();
        
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return duplicates;
        }
        
        for (String url : mediaUrls) {
            try {
                String hash = getOrComputeImageHash(url);
                if (hash != null && isDuplicate(hash, url)) {
                    duplicates.add(url);
                    logger.info("Duplicate image detected: {} with hash: {}", url, hash);
                }
            } catch (Exception e) {
                logger.warn("Failed to process image URL: {} - {}", url, e.getMessage());
            }
        }
        
        return duplicates;
    }
    
    /**
     * Get existing hash from cache or compute new hash for image URL
     */
    private String getOrComputeImageHash(String imageUrl) {
        // Check if we already have hash for this URL
        String cachedHash = redisTemplate.opsForValue().get(URL_TO_HASH_PREFIX + imageUrl);
        if (cachedHash != null) {
            return cachedHash;
        }
        
        // Compute new hash
        String hash = computeImageHash(imageUrl);
        if (hash != null) {
            // Cache URL -> hash mapping
            redisTemplate.opsForValue().set(URL_TO_HASH_PREFIX + imageUrl, hash, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
        
        return hash;
    }
    
    /**
     * Compute SHA-256 hash of image content
     */
    private String computeImageHash(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            
            try (InputStream inputStream = url.openStream()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                
                byte[] hashBytes = digest.digest();
                StringBuilder hexString = new StringBuilder();
                
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                
                return hexString.toString();
                
            } catch (IOException e) {
                logger.warn("Failed to read image from URL: {} - {}", imageUrl, e.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error computing image hash for URL: {} - {}", imageUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if hash already exists (indicating duplicate) and store new hash
     */
    private boolean isDuplicate(String hash, String currentUrl) {
        String hashKey = HASH_KEY_PREFIX + hash;
        
        // Check if this hash already exists
        Set<String> existingUrls = redisTemplate.opsForSet().members(hashKey);
        
        if (existingUrls != null && !existingUrls.isEmpty()) {
            // Check if any existing URL is different from current URL
            for (String existingUrl : existingUrls) {
                if (!existingUrl.equals(currentUrl)) {
                    // This is a duplicate - add current URL to the set and return true
                    redisTemplate.opsForSet().add(hashKey, currentUrl);
                    redisTemplate.expire(hashKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
                    return true;
                }
            }
        }
        
        // Not a duplicate - store this hash with current URL
        redisTemplate.opsForSet().add(hashKey, currentUrl);
        redisTemplate.expire(hashKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return false;
    }
    
    /**
     * Get duplicate count for monitoring/statistics
     */
    public long getDuplicateCount(String hash) {
        String hashKey = HASH_KEY_PREFIX + hash;
        Long count = redisTemplate.opsForSet().size(hashKey);
        return count != null ? count : 0;
    }
    
    /**
     * Clear hash cache for testing purposes
     */
    public void clearHashCache() {
        Set<String> hashKeys = redisTemplate.keys(HASH_KEY_PREFIX + "*");
        Set<String> urlKeys = redisTemplate.keys(URL_TO_HASH_PREFIX + "*");
        
        if (hashKeys != null && !hashKeys.isEmpty()) {
            redisTemplate.delete(hashKeys);
        }
        if (urlKeys != null && !urlKeys.isEmpty()) {
            redisTemplate.delete(urlKeys);
        }
        
        logger.info("Cleared image hash cache");
    }
}