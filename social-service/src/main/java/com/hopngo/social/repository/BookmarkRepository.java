package com.hopngo.social.repository;

import com.hopngo.social.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends MongoRepository<Bookmark, String> {
    
    // Find bookmark by user and post
    Optional<Bookmark> findByUserIdAndPostId(String userId, String postId);
    
    // Find all bookmarks by user
    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Get post IDs bookmarked by user
    List<Bookmark> findByUserId(String userId);
    
    // Check if post is bookmarked by user
    boolean existsByUserIdAndPostId(String userId, String postId);
    
    // Delete bookmark by user and post
    void deleteByUserIdAndPostId(String userId, String postId);
    
    // Count bookmarks by user
    long countByUserId(String userId);
    
    // Delete all bookmarks for a post
    void deleteByPostId(String postId);
}