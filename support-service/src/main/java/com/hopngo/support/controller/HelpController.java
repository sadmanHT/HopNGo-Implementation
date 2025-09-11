package com.hopngo.support.controller;

import com.hopngo.support.dto.HelpArticleResponse;
import com.hopngo.support.entity.HelpArticle;
import com.hopngo.support.service.HelpArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/support/help")
@Tag(name = "Help Center", description = "Public help center and knowledge base")
public class HelpController {

    private final HelpArticleService helpArticleService;

    @Autowired
    public HelpController(HelpArticleService helpArticleService) {
        this.helpArticleService = helpArticleService;
    }

    @GetMapping
    @Operation(summary = "Get help articles", description = "Get published help articles with optional filtering by tag and search query")
    public ResponseEntity<List<HelpArticleResponse>> getHelpArticles(
            @Parameter(description = "Filter by tag") @RequestParam(value = "tag", required = false) String tag,
            @Parameter(description = "Search query") @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Include article body") @RequestParam(value = "includeBody", defaultValue = "false") boolean includeBody) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<HelpArticle> articles;
        
        if (query != null && !query.trim().isEmpty()) {
            // Search articles
            articles = helpArticleService.searchPublishedArticles(query.trim(), pageable);
        } else if (tag != null && !tag.trim().isEmpty()) {
            // Filter by tag
            articles = helpArticleService.getPublishedArticlesByTag(tag.trim(), pageable);
        } else {
            // Get all published articles
            articles = helpArticleService.getPublishedArticles(pageable);
        }
        
        List<HelpArticleResponse> response = articles.getContent().stream()
                .map(article -> includeBody ? 
                    HelpArticleResponse.from(article) : 
                    HelpArticleResponse.fromWithoutBody(article))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get help article by slug", description = "Get a specific published help article by its slug")
    public ResponseEntity<HelpArticleResponse> getHelpArticle(
            @Parameter(description = "Article slug") @PathVariable String slug) {
        
        HelpArticle article = helpArticleService.getPublishedArticleBySlug(slug);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Increment view count
        helpArticleService.incrementViewCount(article.getId());
        
        return ResponseEntity.ok(HelpArticleResponse.from(article));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get help categories", description = "Get all available help article tags/categories")
    public ResponseEntity<List<String>> getHelpCategories() {
        List<String> categories = helpArticleService.getAllPublishedTags();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular help articles", description = "Get most viewed published help articles")
    public ResponseEntity<List<HelpArticleResponse>> getPopularArticles(
            @Parameter(description = "Number of articles to return") @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        Page<HelpArticle> articles = helpArticleService.getPopularArticles(pageable);
        
        List<HelpArticleResponse> response = articles.getContent().stream()
                .map(HelpArticleResponse::fromWithoutBody)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent help articles", description = "Get recently published help articles")
    public ResponseEntity<List<HelpArticleResponse>> getRecentArticles(
            @Parameter(description = "Number of articles to return") @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        Page<HelpArticle> articles = helpArticleService.getRecentArticles(pageable);
        
        List<HelpArticleResponse> response = articles.getContent().stream()
                .map(HelpArticleResponse::fromWithoutBody)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/related")
    @Operation(summary = "Get related help articles", description = "Get articles related to the specified article")
    public ResponseEntity<List<HelpArticleResponse>> getRelatedArticles(
            @Parameter(description = "Article slug") @PathVariable String slug,
            @Parameter(description = "Number of articles to return") @RequestParam(value = "limit", defaultValue = "5") int limit) {
        
        HelpArticle article = helpArticleService.getPublishedArticleBySlug(slug);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        Page<HelpArticle> relatedArticles = helpArticleService.getRelatedArticles(article.getId(), pageable);
        
        List<HelpArticleResponse> response = relatedArticles.getContent().stream()
                .map(HelpArticleResponse::fromWithoutBody)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get search suggestions based on query")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Number of suggestions to return") @RequestParam(value = "limit", defaultValue = "5") int limit) {
        
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        
        List<String> suggestions = helpArticleService.getSearchSuggestions(query.trim(), limit);
        return ResponseEntity.ok(suggestions);
    }
}