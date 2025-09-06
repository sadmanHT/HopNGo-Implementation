package com.hopngo.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageHashServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private SetOperations<String, String> setOperations;
    
    @InjectMocks
    private ImageHashService imageHashService;
    
    @BeforeEach
    void setUp() {
        // Setup will be done in individual tests as needed
    }
    
    @Test
    void testCheckForDuplicates_EmptyList() {
        List<String> mediaUrls = Collections.emptyList();
        List<String> duplicates = imageHashService.checkForDuplicates(mediaUrls);
        
        assertTrue(duplicates.isEmpty());
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    void testCheckForDuplicates_NullList() {
        List<String> duplicates = imageHashService.checkForDuplicates(null);
        
        assertTrue(duplicates.isEmpty());
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    void testCheckForDuplicates_NoDuplicates() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        List<String> mediaUrls = Arrays.asList(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        );
        
        // Mock no cached hashes
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // Mock empty sets for hash keys (no existing duplicates)
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());
        
        List<String> duplicates = imageHashService.checkForDuplicates(mediaUrls);
        
        assertTrue(duplicates.isEmpty());
        
        // Verify Redis operations were called
        verify(valueOperations, times(2)).get(startsWith("url_to_hash:"));
        verify(setOperations, times(2)).add(startsWith("image_hash:"), anyString());
        verify(redisTemplate, times(2)).expire(startsWith("image_hash:"), eq(168L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testCheckForDuplicates_WithDuplicates() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        List<String> mediaUrls = Arrays.asList(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        );
        
        String mockHash1 = "hash123";
        String mockHash2 = "hash456";
        
        // Mock cached hashes
        when(valueOperations.get("url_to_hash:https://example.com/image1.jpg")).thenReturn(mockHash1);
        when(valueOperations.get("url_to_hash:https://example.com/image2.jpg")).thenReturn(mockHash2);
        
        // Mock existing URLs for hash1 (indicating duplicate)
        Set<String> existingUrls1 = new HashSet<>(Arrays.asList("https://other.com/duplicate.jpg"));
        when(setOperations.members("image_hash:" + mockHash1)).thenReturn(existingUrls1);
        
        // Mock empty set for hash2 (no duplicate)
        when(setOperations.members("image_hash:" + mockHash2)).thenReturn(Collections.emptySet());
        
        List<String> duplicates = imageHashService.checkForDuplicates(mediaUrls);
        
        assertEquals(1, duplicates.size());
        assertTrue(duplicates.contains("https://example.com/image1.jpg"));
        
        // Verify Redis operations
        verify(setOperations).add("image_hash:" + mockHash1, "https://example.com/image1.jpg");
        verify(setOperations).add("image_hash:" + mockHash2, "https://example.com/image2.jpg");
    }
    
    @Test
    void testCheckForDuplicates_SameUrlNotDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        List<String> mediaUrls = Arrays.asList("https://example.com/image1.jpg");
        String mockHash = "hash123";
        
        // Mock cached hash
        when(valueOperations.get("url_to_hash:https://example.com/image1.jpg")).thenReturn(mockHash);
        
        // Mock existing URL set containing the same URL
        Set<String> existingUrls = new HashSet<>(Arrays.asList("https://example.com/image1.jpg"));
        when(setOperations.members("image_hash:" + mockHash)).thenReturn(existingUrls);
        
        List<String> duplicates = imageHashService.checkForDuplicates(mediaUrls);
        
        assertTrue(duplicates.isEmpty(), "Same URL should not be considered a duplicate");
        
        // Verify the URL was still added to the set
        verify(setOperations).add("image_hash:" + mockHash, "https://example.com/image1.jpg");
    }
    
    @Test
    void testGetDuplicateCount() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        String hash = "testHash123";
        Long expectedCount = 3L;
        
        when(setOperations.size("image_hash:" + hash)).thenReturn(expectedCount);
        
        long actualCount = imageHashService.getDuplicateCount(hash);
        
        assertEquals(expectedCount, actualCount);
        verify(setOperations).size("image_hash:" + hash);
    }
    
    @Test
    void testGetDuplicateCount_NullResult() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        String hash = "testHash123";
        
        when(setOperations.size("image_hash:" + hash)).thenReturn(null);
        
        long actualCount = imageHashService.getDuplicateCount(hash);
        
        assertEquals(0, actualCount);
    }
    
    @Test
    void testClearHashCache() {
        Set<String> hashKeys = Set.of("image_hash:hash1", "image_hash:hash2");
        Set<String> urlKeys = Set.of("url_to_hash:url1", "url_to_hash:url2");
        
        when(redisTemplate.keys("image_hash:*")).thenReturn(hashKeys);
        when(redisTemplate.keys("url_to_hash:*")).thenReturn(urlKeys);
        
        imageHashService.clearHashCache();
        
        verify(redisTemplate).delete(hashKeys);
        verify(redisTemplate).delete(urlKeys);
    }
    
    @Test
    void testClearHashCache_EmptyKeys() {
        when(redisTemplate.keys("image_hash:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("url_to_hash:*")).thenReturn(Collections.emptySet());
        
        imageHashService.clearHashCache();
        
        verify(redisTemplate, never()).delete(any(Collection.class));
    }
    
    @Test
    void testCheckForDuplicates_InvalidUrl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        List<String> mediaUrls = Arrays.asList("invalid-url", "https://example.com/valid.jpg");
        
        // Mock no cached hashes
        when(valueOperations.get(anyString())).thenReturn(null);
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());
        
        List<String> duplicates = imageHashService.checkForDuplicates(mediaUrls);
        
        // Should handle invalid URL gracefully and continue with valid ones
        assertTrue(duplicates.isEmpty());
        
        // Should still process the valid URL
        verify(setOperations, atLeastOnce()).add(startsWith("image_hash:"), anyString());
    }
}