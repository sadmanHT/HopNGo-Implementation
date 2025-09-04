package com.hopngo.social.repository;

import com.hopngo.social.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    // Find comments by post ID
    Page<Comment> findByPostIdOrderByCreatedAtAsc(String postId, Pageable pageable);
    
    // Find comments by user ID
    Page<Comment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Count comments by post ID
    long countByPostId(String postId);
    
    // Delete all comments for a post
    void deleteByPostId(String postId);
    
    // Find all comments by post ID (for batch operations)
    List<Comment> findByPostId(String postId);
}