package com.hopngo.social.repository;

import com.hopngo.social.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    
    // Find posts by user ID
    Page<Post> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find recent posts for feed (ordered by creation date)
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Find posts by tags
    @Query("{'tags': { $in: ?0 }}")
    Page<Post> findByTagsInOrderByCreatedAtDesc(List<String> tags, Pageable pageable);
    
    // Find posts within a geographic bounding box
    @Query("{'location.lat': { $gte: ?0, $lte: ?1 }, 'location.lng': { $gte: ?2, $lte: ?3 }}")
    List<Post> findPostsInBoundingBox(double minLat, double maxLat, double minLng, double maxLng);
    
    // Find posts by user IDs (for following-based feed)
    @Query("{'userId': { $in: ?0 }}")
    Page<Post> findByUserIdInOrderByCreatedAtDesc(List<String> userIds, Pageable pageable);
    
    // Count posts by user
    long countByUserId(String userId);
}