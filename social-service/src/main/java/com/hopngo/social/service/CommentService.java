package com.hopngo.social.service;

import com.hopngo.social.dto.CommentResponse;
import com.hopngo.social.dto.CreateCommentRequest;
import com.hopngo.social.entity.Comment;
import com.hopngo.social.entity.Post;
import com.hopngo.social.repository.CommentRepository;
import com.hopngo.social.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @CacheEvict(value = "comments", allEntries = true)
    public CommentResponse createComment(CreateCommentRequest request, String userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(request.getPostId());
        if (postOpt.isEmpty()) {
            throw new RuntimeException("Post not found");
        }
        
        // Create comment
        Comment comment = new Comment(
            request.getPostId(),
            userId,
            request.getText()
        );
        
        Comment savedComment = commentRepository.save(comment);
        
        // Update post comment count
        Post post = postOpt.get();
        post.incrementCommentCount();
        postRepository.save(post);
        
        return convertToCommentResponse(savedComment);
    }
    
    @Cacheable(value = "comments", key = "'post:' + #postId + ':' + #page + ':' + #size")
    public Page<CommentResponse> getCommentsByPostId(String postId, int page, int size) {
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        return comments.map(this::convertToCommentResponse);
    }
    
    public Page<CommentResponse> getCommentsByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return comments.map(this::convertToCommentResponse);
    }
    
    @CacheEvict(value = "comments", allEntries = true)
    public boolean deleteComment(String commentId, String userId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return false;
        }
        
        Comment comment = commentOpt.get();
        
        // Check if user owns the comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }
        
        // Delete comment
        commentRepository.delete(comment);
        
        // Update post comment count
        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.decrementCommentCount();
            postRepository.save(post);
        }
        
        return true;
    }
    
    public Optional<CommentResponse> getCommentById(String commentId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        return commentOpt.map(this::convertToCommentResponse);
    }
    
    private CommentResponse convertToCommentResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getPostId(),
            comment.getUserId(),
            comment.getText(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}