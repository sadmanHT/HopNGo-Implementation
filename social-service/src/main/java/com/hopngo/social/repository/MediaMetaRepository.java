package com.hopngo.social.repository;

import com.hopngo.social.entity.MediaMeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaMetaRepository extends MongoRepository<MediaMeta, String> {
    
    List<MediaMeta> findByUserId(String userId);
    
    List<MediaMeta> findByUserIdOrderByCreatedAtDesc(String userId);
    
    @Query("{'userId': ?0, 'createdAt': {'$gte': ?1, '$lt': ?2}}")
    List<MediaMeta> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    @Query(value = "{'userId': ?0, 'createdAt': {'$gte': ?1, '$lt': ?2}}", fields = "{'bytes': 1}")
    List<MediaMeta> findBytesByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    void deleteByPublicId(String publicId);
    
    MediaMeta findByPublicId(String publicId);
}