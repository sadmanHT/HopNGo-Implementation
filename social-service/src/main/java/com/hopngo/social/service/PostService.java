package com.hopngo.social.service;

import com.hopngo.social.dto.*;
import com.hopngo.social.entity.Post;
import com.hopngo.social.repository.PostRepository;
import com.hopngo.social.repository.BookmarkRepository;
import com.hopngo.social.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        
        Post savedPost = postRepository.save(post);
        return convertToPostResponse(savedPost, userId);
    }
    
    public Optional<PostResponse> getPostById(String postId, String currentUserId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            return Optional.of(convertToPostResponse(postOpt.get(), currentUserId));
        }
        return Optional.empty();
    }
    
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
    
    public Page<PostResponse> getUserPosts(String userId, int page, int size, String currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return posts.map(post -> convertToPostResponse(post, currentUserId));
    }
    
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