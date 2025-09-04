package com.hopngo.social.repository;

import com.hopngo.social.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends MongoRepository<Follow, String> {
    
    // Find follow relationship
    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);
    
    // Check if user is following another user
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);
    
    // Get users that a user is following
    Page<Follow> findByFollowerIdOrderByCreatedAtDesc(String followerId, Pageable pageable);
    
    // Get followers of a user
    Page<Follow> findByFollowingIdOrderByCreatedAtDesc(String followingId, Pageable pageable);
    
    // Get list of user IDs that a user is following (for feed generation)
    List<Follow> findByFollowerId(String followerId);
    
    // Count following
    long countByFollowerId(String followerId);
    
    // Count followers
    long countByFollowingId(String followingId);
    
    // Delete follow relationship
    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);
}