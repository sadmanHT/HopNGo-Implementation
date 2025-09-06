package com.hopngo.social.controller;

import com.hopngo.social.config.AuthFilter;
import com.hopngo.social.dto.*;
import com.hopngo.social.service.PostService;
import com.hopngo.social.service.CommentService;
import com.hopngo.social.service.HeatmapService;
import com.hopngo.social.service.HeatmapCacheService;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    
    @Autowired
    private HeatmapCacheService heatmapCacheService;
    
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
    @Operation(summary = "Get heatmap data", description = "Get clustered post counts for heatmap visualization with geohash and time decay")
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @Parameter(description = "Bounding box: minLat,minLng,maxLat,maxLng") 
            @RequestParam String bbox,
            @Parameter(description = "Geohash precision (5-7)") 
            @RequestParam(defaultValue = "6") int precision,
            @Parameter(description = "Time window in hours") 
            @RequestParam(defaultValue = "72") int sinceHours,
            @Parameter(description = "Filter by tag (optional)") 
            @RequestParam(required = false) String tag) {
        
        try {
            // Validate bbox format
            String[] coords = bbox.split(",");
            if (coords.length != 4) {
                return ResponseEntity.badRequest().build();
            }
            
            double minLat = Double.parseDouble(coords[0]);
            double minLng = Double.parseDouble(coords[1]);
            double maxLat = Double.parseDouble(coords[2]);
            double maxLng = Double.parseDouble(coords[3]);
            
            // Validate precision range
            if (precision < 5 || precision > 7) {
                return ResponseEntity.badRequest().build();
            }
            
            // Check cache first
            List<HeatmapService.HeatmapCell> cachedCells = heatmapCacheService.getCachedHeatmap(bbox, precision, sinceHours, tag);
            if (cachedCells != null) {
                // Convert cached cells to response format
                List<HeatmapResponse.HeatmapPoint> cachedPoints = cachedCells.stream()
                    .map(cell -> new HeatmapResponse.HeatmapPoint(
                        cell.getGeohash(), cell.getLat(), cell.getLng(), cell.getWeight(), cell.getTagsTop()))
                    .collect(Collectors.toList());
                
                HeatmapResponse cachedResponse = new HeatmapResponse(cachedPoints);
                return ResponseEntity.ok(cachedResponse);
            }
            
            // Generate new heatmap
            HeatmapService.BoundingBoxFilter bboxFilter = new HeatmapService.BoundingBoxFilter(minLat, maxLat, minLng, maxLng);
            List<HeatmapService.HeatmapCell> cells = heatmapService.generateHeatmap(bboxFilter, precision, sinceHours, tag);
            
            // Convert to response format
            List<HeatmapResponse.HeatmapPoint> points = cells.stream()
                .map(cell -> new HeatmapResponse.HeatmapPoint(
                    cell.getGeohash(), cell.getLat(), cell.getLng(), cell.getWeight(), cell.getTagsTop()))
                .collect(java.util.stream.Collectors.toList());
            
            HeatmapResponse heatmap = new HeatmapResponse(points);
            
            // Cache the result
            heatmapCacheService.cacheHeatmap(bbox, precision, sinceHours, tag, cells);
            
            return ResponseEntity.ok(heatmap);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
        Page<PostResponse> bookmarkedPosts = postService.getBookmarkedPosts(userId, page, size);
        return ResponseEntity.ok(bookmarkedPosts);
    }
    
    @PostMapping("/posts/{id}/flag")
    @Operation(summary = "Flag a post", description = "Report a post for inappropriate content")
    public ResponseEntity<Map<String, String>> flagPost(
            @PathVariable String id,
            @Valid @RequestBody FlagContentRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        
        try {
            postService.flagPost(id, userId, request.getReason());
            return ResponseEntity.ok(Map.of(
                "message", "Post has been flagged for review",
                "postId", id
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/posts/{id}/visibility")
    @Operation(summary = "Update post visibility", description = "Update post visibility (admin only)")
    public ResponseEntity<Map<String, String>> updatePostVisibility(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String userId = AuthFilter.getCurrentUserId(httpRequest);
        
        // TODO: Add proper admin check
        // if (!isAdmin(userId)) {
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
        //         .body(Map.of("error", "Admin access required"));
        // }
        
        try {
            String visibilityStr = request.get("visibility");
            if (visibilityStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Visibility is required"));
            }
            
            com.hopngo.social.entity.Post.Visibility visibility;
            try {
                visibility = com.hopngo.social.entity.Post.Visibility.valueOf(visibilityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid visibility value"));
            }
            
            postService.updatePostVisibility(id, visibility);
            
            return ResponseEntity.ok(Map.of(
                "message", "Post visibility updated",
                "visibility", visibility.name()
            ));
            
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Post not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update post visibility"));
            }
        }
    }
}