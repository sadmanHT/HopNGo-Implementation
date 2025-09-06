package com.hopngo.social.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class HeatmapCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(HeatmapCacheService.class);
    private static final String CACHE_PREFIX = "heatmap:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(45); // 45 seconds TTL
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private final ObjectMapper objectMapper;
    
    public HeatmapCacheService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public List<HeatmapService.HeatmapCell> getCachedHeatmap(String bbox, int precision, int sinceHours, String tag) {
        String cacheKey = generateCacheKey(bbox, precision, sinceHours, tag);
        
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                logger.debug("Cache hit for heatmap key: {}", cacheKey);
                return objectMapper.readValue(cachedValue, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HeatmapService.HeatmapCell.class));
            }
        } catch (Exception e) {
            logger.warn("Error reading from cache for key {}: {}", cacheKey, e.getMessage());
        }
        
        logger.debug("Cache miss for heatmap key: {}", cacheKey);
        return null;
    }
    
    public void cacheHeatmap(String bbox, int precision, int sinceHours, String tag, List<HeatmapService.HeatmapCell> cells) {
        String cacheKey = generateCacheKey(bbox, precision, sinceHours, tag);
        
        try {
            String jsonValue = objectMapper.writeValueAsString(cells);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, CACHE_TTL);
            logger.debug("Cached heatmap for key: {} (TTL: {}s)", cacheKey, CACHE_TTL.getSeconds());
        } catch (JsonProcessingException e) {
            logger.error("Error caching heatmap for key {}: {}", cacheKey, e.getMessage());
        }
    }
    
    public void invalidateHeatmapCache() {
        try {
            // Delete all heatmap cache keys
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Invalidated {} heatmap cache entries", keys.size());
            }
        } catch (Exception e) {
            logger.error("Error invalidating heatmap cache: {}", e.getMessage());
        }
    }
    
    public void invalidateHeatmapCacheForBbox(String bbox) {
        try {
            String pattern = CACHE_PREFIX + bbox + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Invalidated {} heatmap cache entries for bbox: {}", keys.size(), bbox);
            }
        } catch (Exception e) {
            logger.error("Error invalidating heatmap cache for bbox {}: {}", bbox, e.getMessage());
        }
    }
    
    private String generateCacheKey(String bbox, int precision, int sinceHours, String tag) {
        StringBuilder keyBuilder = new StringBuilder(CACHE_PREFIX);
        keyBuilder.append(bbox != null ? bbox : "all");
        keyBuilder.append(":").append(precision);
        keyBuilder.append(":").append(sinceHours);
        keyBuilder.append(":").append(tag != null ? tag : "notag");
        return keyBuilder.toString();
    }
    
    public boolean isCacheAvailable() {
        try {
            redisTemplate.opsForValue().get("test");
            return true;
        } catch (Exception e) {
            logger.warn("Redis cache is not available: {}", e.getMessage());
            return false;
        }
    }
}