package com.hopngo.social.controller;

import com.hopngo.social.config.AuthFilter;
import com.hopngo.social.dto.*;
import com.hopngo.social.service.PostService;
import com.hopngo.social.service.CommentService;
import com.hopngo.social.service.HeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/social")
@Tag(name = "Social Media", description = "Social media endpoints for posts, comments, likes, and bookmarks")
public class SocialController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private HeatmapService heatmapService;
    
    @PostMapping("/posts")
    @Operation(summary = "Create a new post", description = "Create a new social media post with text, media, tags, and location")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        PostResponse post = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }
    
    @GetMapping("/posts/{id}")
    @Operation(summary = "Get post by ID", description = "Retrieve a specific post by its ID")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        Optional<PostResponse> post = postService.getPostById(id, userId);
        
        if (post.isPresent()) {
            return ResponseEntity.ok(post.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/feed")
    @Operation(summary = "Get social media feed", description = "Get paginated feed of recent posts")
    public ResponseEntity<Page<PostResponse>> getFeed(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        Page<PostResponse> feed = postService.getFeed(page, size, userId);
        return ResponseEntity.ok(feed);
    }
    
    @PostMapping("/posts/{id}/like")
    @Operation(summary = "Toggle like on post", description = "Like or unlike a post (toggle behavior)")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        
        try {
            boolean liked = postService.toggleLike(id, userId);
            return ResponseEntity.ok(Map.of(
                "liked", liked,
                "message", liked ? "Post liked" : "Post unliked"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/posts/{id}/bookmark")
    @Operation(summary = "Toggle bookmark on post", description = "Bookmark or unbookmark a post (toggle behavior)")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        
        try {
            boolean bookmarked = postService.toggleBookmark(id, userId);
            return ResponseEntity.ok(Map.of(
                "bookmarked", bookmarked,
                "message", bookmarked ? "Post bookmarked" : "Post unbookmarked"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/comments")
    @Operation(summary = "Create a comment", description = "Add a comment to a post")
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        
        try {
            CommentResponse comment = commentService.createComment(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments for a post", description = "Retrieve paginated comments for a specific post")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable String postId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<CommentResponse> comments = commentService.getCommentsByPostId(postId, page, size);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/heatmap")
    @Operation(summary = "Get heatmap data", description = "Get clustered post counts for heatmap visualization")
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @Parameter(description = "Bounding box: minLat,minLng,maxLat,maxLng") 
            @RequestParam String bbox) {
        
        try {
            String[] coords = bbox.split(",");
            if (coords.length != 4) {
                return ResponseEntity.badRequest().build();
            }
            
            double minLat = Double.parseDouble(coords[0]);
            double minLng = Double.parseDouble(coords[1]);
            double maxLat = Double.parseDouble(coords[2]);
            double maxLng = Double.parseDouble(coords[3]);
            
            HeatmapResponse heatmap = heatmapService.getHeatmap(minLat, maxLat, minLng, maxLng);
            return ResponseEntity.ok(heatmap);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/users/{userId}/posts")
    @Operation(summary = "Get user posts", description = "Get paginated posts by a specific user")
    public ResponseEntity<Page<PostResponse>> getUserPosts(
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        
        String currentUserId = AuthFilter.getCurrentUserId(httpRequest);
        Page<PostResponse> posts = postService.getUserPosts(userId, page, size, currentUserId);
        return ResponseEntity.ok(posts);
    }
    
    @GetMapping("/bookmarks")
    @Operation(summary = "Get bookmarked posts", description = "Get user's bookmarked posts")
    public ResponseEntity<Page<PostResponse>> getBookmarkedPosts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        Page<PostResponse> posts = postService.getBookmarkedPosts(userId, page, size);
        return ResponseEntity.ok(posts);
    }
}