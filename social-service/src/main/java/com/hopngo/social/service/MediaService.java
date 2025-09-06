package com.hopngo.social.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hopngo.social.dto.SignedUploadRequest;
import com.hopngo.social.dto.SignedUploadResponse;
import com.hopngo.social.entity.MediaMeta;
import com.hopngo.social.repository.MediaMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MediaService {
    
    private static final long DAILY_QUOTA_BYTES = 200L * 1024 * 1024; // 200MB
    private static final String QUOTA_KEY_PREFIX = "media_quota:";
    
    @Autowired
    private MediaMetaRepository mediaMetaRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private Cloudinary cloudinary;
    
    @Value("${cloudinary.upload-preset:hopngo_media}")
    private String uploadPreset;
    
    public SignedUploadResponse generateSignedUpload(String userId, SignedUploadRequest request) throws Exception {
        // Check daily quota
        if (!checkAndUpdateQuota(userId, request.getFileSize())) {
            throw new RuntimeException("Daily upload quota exceeded (200MB/day)");
        }
        
        // Generate timestamp
        long timestamp = System.currentTimeMillis() / 1000;
        
        // Prepare upload parameters
        Map<String, Object> params = new HashMap<>();
        params.put("timestamp", timestamp);
        params.put("upload_preset", uploadPreset);
        params.put("resource_type", request.getResourceType());
        
        if (request.getFolder() != null && !request.getFolder().isEmpty()) {
            params.put("folder", request.getFolder());
        }
        
        // Add user-specific folder
        params.put("folder", "users/" + userId);
        
        // Generate signature
        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);
        
        // Prepare response
        SignedUploadResponse response = new SignedUploadResponse();
        response.setSignature(signature);
        response.setTimestamp(String.valueOf(timestamp));
        response.setApiKey(cloudinary.config.apiKey);
        response.setCloudName(cloudinary.config.cloudName);
        response.setUploadUrl(cloudinary.url().resourceType(request.getResourceType()).generate(""));
        response.setUploadPreset(uploadPreset);
        response.setParams(params);
        
        return response;
    }
    
    public MediaMeta saveMediaMeta(String userId, Map<String, Object> uploadResult) {
        MediaMeta mediaMeta = new MediaMeta();
        mediaMeta.setUserId(userId);
        mediaMeta.setPublicId((String) uploadResult.get("public_id"));
        mediaMeta.setUrl((String) uploadResult.get("url"));
        mediaMeta.setSecureUrl((String) uploadResult.get("secure_url"));
        mediaMeta.setResourceType((String) uploadResult.get("resource_type"));
        mediaMeta.setFormat((String) uploadResult.get("format"));
        
        // Set dimensions and size
        if (uploadResult.get("width") != null) {
            mediaMeta.setWidth((Integer) uploadResult.get("width"));
        }
        if (uploadResult.get("height") != null) {
            mediaMeta.setHeight((Integer) uploadResult.get("height"));
        }
        if (uploadResult.get("bytes") != null) {
            mediaMeta.setBytes(((Number) uploadResult.get("bytes")).longValue());
        }
        if (uploadResult.get("duration") != null) {
            mediaMeta.setDuration(((Number) uploadResult.get("duration")).doubleValue());
        }
        
        return mediaMetaRepository.save(mediaMeta);
    }
    
    public boolean checkAndUpdateQuota(String userId, long fileSize) {
        String quotaKey = QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now().toString();
        
        // Get current usage
        String currentUsageStr = redisTemplate.opsForValue().get(quotaKey);
        long currentUsage = currentUsageStr != null ? Long.parseLong(currentUsageStr) : 0;
        
        // Check if adding this file would exceed quota
        if (currentUsage + fileSize > DAILY_QUOTA_BYTES) {
            return false;
        }
        
        // Update usage
        redisTemplate.opsForValue().increment(quotaKey, fileSize);
        
        // Set expiration to end of day if this is the first upload today
        if (currentUsage == 0) {
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
            LocalDateTime now = LocalDateTime.now();
            long secondsUntilEndOfDay = java.time.Duration.between(now, endOfDay).getSeconds();
            redisTemplate.expire(quotaKey, secondsUntilEndOfDay, TimeUnit.SECONDS);
        }
        
        return true;
    }
    
    public long getRemainingQuota(String userId) {
        String quotaKey = QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now().toString();
        String currentUsageStr = redisTemplate.opsForValue().get(quotaKey);
        long currentUsage = currentUsageStr != null ? Long.parseLong(currentUsageStr) : 0;
        return Math.max(0, DAILY_QUOTA_BYTES - currentUsage);
    }
    
    public List<MediaMeta> getUserMedia(String userId) {
        return mediaMetaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public void deleteMedia(String publicId) throws Exception {
        // Delete from Cloudinary
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        
        // Delete metadata
        mediaMetaRepository.deleteByPublicId(publicId);
    }
    
    public String getTransformedUrl(String publicId, String resourceType, Map<String, Object> transformations) {
        com.cloudinary.Transformation transformation = new com.cloudinary.Transformation();
        
        // Apply common transformations
        if (transformations.containsKey("width")) {
            transformation.width(transformations.get("width"));
        }
        if (transformations.containsKey("height")) {
            transformation.height(transformations.get("height"));
        }
        if (transformations.containsKey("crop")) {
            transformation.crop((String) transformations.get("crop"));
        }
        if (transformations.containsKey("quality")) {
            transformation.quality(transformations.get("quality"));
        }
        if (transformations.containsKey("format")) {
            transformation.fetchFormat((String) transformations.get("format"));
        }
        
        return cloudinary.url()
                .resourceType(resourceType)
                .transformation(transformation)
                .generate(publicId);
    }
}