package com.hopngo.social.service;

import com.hopngo.social.dto.*;
import com.hopngo.social.entity.Post;
import com.hopngo.social.repository.PostRepository;
import com.hopngo.social.repository.BookmarkRepository;
import com.hopngo.social.repository.CommentRepository;
import com.hopngo.social.service.EventPublisher;
import com.hopngo.social.service.AiModerationService;
import com.hopngo.social.service.AiModerationService.ModerationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private BookmarkRepository bookmarkRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private AiModerationService aiModerationService;
    
    // SearchIndexingService removed due to missing dependencies
    
    @CacheEvict(value = {"socialFeed", "userPosts"}, allEntries = true)
    public PostResponse createPost(CreatePostRequest request, String userId) {
        Post.Location location = null;
        if (request.getLocation() != null) {
            location = new Post.Location(
                request.getLocation().getLat(),
                request.getLocation().getLng(),
                request.getLocation().getPlace()
            );
        }
        
        Post post = new Post(
            userId,
            request.getText(),
            request.getMediaUrls(),
            request.getTags(),
            location
        );
        
        // AI moderation check
        ModerationResult moderationResult = aiModerationService.moderateContent(
            request.getText(), request.getMediaUrls()
        );
        
        if (aiModerationService.shouldFlag(moderationResult)) {
            post.setVisibility(Post.Visibility.PENDING_REVIEW);
            
            // Save post first to get ID
            Post savedPost = postRepository.save(post);
            
            // Emit content flagged event for AI moderation
            eventPublisher.publishContentFlaggedEvent(
                "POST", savedPost.getId(), "SYSTEM", 
                "AI moderation: " + String.join(", ", moderationResult.getReasons())
            );
            
            // Don't index flagged posts in search
            
            return convertToPostResponse(savedPost, userId);
        } else {
            // Content is clean, set as public
            post.setVisibility(Post.Visibility.PUBLIC);
        }
        
        Post savedPost = postRepository.save(post);
        
        // Search indexing removed due to missing dependencies
        
        return convertToPostResponse(savedPost, userId);
    }
    
    public Optional<PostResponse> getPostById(String postId, String currentUserId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            // Check if user can view this post
            if (canViewPost(post, currentUserId)) {
                return Optional.of(convertToPostResponse(post, currentUserId));
            }
        }
        return Optional.empty();
    }
    
    @Cacheable(value = "socialFeed", key = "'feed:' + #page + ':' + #size + ':' + #currentUserId")
    public Page<PostResponse> getFeed(int page, int size, String currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        return posts.map(post -> convertToPostResponse(post, currentUserId));
    }
    
    public boolean toggleLike(String postId, String userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            boolean liked = post.toggleLike(userId);
            postRepository.save(post);
            return liked;
        }
        throw new RuntimeException("Post not found");
    }
    
    @CacheEvict(value = "bookmarks", allEntries = true)
    public boolean toggleBookmark(String postId, String userId) {
        // Check if post exists
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }
        
        // Check if bookmark exists
        if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
            bookmarkRepository.deleteByUserIdAndPostId(userId, postId);
            return false; // unbookmarked
        } else {
            bookmarkRepository.save(new com.hopngo.social.entity.Bookmark(userId, postId));
            return true; // bookmarked
        }
    }
    
    @Cacheable(value = "userPosts", key = "'user:' + #userId + ':' + #page + ':' + #size + ':' + #currentUserId")
    public Page<PostResponse> getUserPosts(String userId, int page, int size, String currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;
        
        // If viewing own posts or admin, show all posts; otherwise only public posts
        if (userId.equals(currentUserId) || isAdmin(currentUserId)) {
            posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            posts = postRepository.findByUserIdAndVisibilityPublicOrderByCreatedAtDesc(userId, pageable);
        }
        
        return posts.map(post -> convertToPostResponse(post, currentUserId));
    }
    
    @Cacheable(value = "bookmarks", key = "'user:' + #userId + ':' + #page + ':' + #size")
    public Page<PostResponse> getBookmarkedPosts(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.hopngo.social.entity.Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        List<String> postIds = bookmarks.getContent().stream()
            .map(com.hopngo.social.entity.Bookmark::getPostId)
            .collect(Collectors.toList());
        
        List<Post> posts = postRepository.findAllById(postIds);
        List<PostResponse> postResponses = posts.stream()
            .map(post -> convertToPostResponse(post, userId))
            .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(
            postResponses, pageable, bookmarks.getTotalElements()
        );
    }
    
    private boolean canViewPost(Post post, String currentUserId) {
        // Public posts can be viewed by anyone
        if (post.getVisibility() == Post.Visibility.PUBLIC) {
            return true;
        }
        
        // Owner can always view their own posts
        if (post.getUserId().equals(currentUserId)) {
            return true;
        }
        
        // Admin can view all posts
        if (isAdmin(currentUserId)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isAdmin(String userId) {
        // TODO: Implement admin check - for now return false
        // This should check user roles from auth service or JWT claims
        return false;
    }
    
    public void flagPost(String postId, String reporterId, String reason) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            // Emit content.flagged event
            eventPublisher.publishContentFlaggedEvent("POST", postId, reporterId, reason);
        } else {
            throw new RuntimeException("Post not found");
        }
    }
    
    /**
     * Update post visibility (used by admin service)
     */
    @CacheEvict(value = {"socialFeed", "userPosts"}, allEntries = true)
    public void updatePostVisibility(String postId, Post.Visibility visibility) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            Post.Visibility oldVisibility = post.getVisibility();
            post.setVisibility(visibility);
            
            Post savedPost = postRepository.save(post);
            
            // Search indexing removed due to missing dependencies
        } else {
            throw new RuntimeException("Post not found");
        }
    }
    
    private PostResponse convertToPostResponse(Post post, String currentUserId) {
        LocationDto locationDto = null;
        if (post.getLocation() != null) {
            locationDto = new LocationDto(
                post.getLocation().getLat(),
                post.getLocation().getLng(),
                post.getLocation().getPlace()
            );
        }
        
        boolean isLiked = currentUserId != null && post.getLikedBy().contains(currentUserId);
        boolean isBookmarked = currentUserId != null && 
            bookmarkRepository.existsByUserIdAndPostId(currentUserId, post.getId());
        
        return new PostResponse(
            post.getId(),
            post.getUserId(),
            post.getText(),
            post.getMediaUrls(),
            post.getTags(),
            locationDto,
            post.getLikeCount(),
            post.getCommentCount(),
            isLiked,
            isBookmarked,
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}