package com.hopngo.support.controller;

import com.hopngo.support.dto.HelpArticleCreateRequest;
import com.hopngo.support.dto.HelpArticleResponse;
import com.hopngo.support.dto.TicketResponse;
import com.hopngo.support.entity.HelpArticle;
import com.hopngo.support.entity.Ticket;
import com.hopngo.support.service.HelpArticleService;
import com.hopngo.support.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/support/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative functions for support system")
public class AdminController {

    private final HelpArticleService helpArticleService;
    private final TicketService ticketService;

    @Autowired
    public AdminController(HelpArticleService helpArticleService, TicketService ticketService) {
        this.helpArticleService = helpArticleService;
        this.ticketService = ticketService;
    }

    // Help Article Management
    @GetMapping("/articles")
    @Operation(summary = "Get all help articles", description = "Get all help articles including drafts (admin only)")
    public ResponseEntity<List<HelpArticleResponse>> getAllArticles(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Filter by author") @RequestParam(value = "author", required = false) String authorId,
            @Parameter(description = "Include article body") @RequestParam(value = "includeBody", defaultValue = "false") boolean includeBody) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<HelpArticle> articles;
        
        if (authorId != null && !authorId.trim().isEmpty()) {
            articles = helpArticleService.getArticlesByAuthor(authorId.trim(), pageable);
        } else {
            articles = helpArticleService.getAllArticles(pageable);
        }
        
        List<HelpArticleResponse> response = articles.getContent().stream()
                .map(article -> includeBody ? 
                    HelpArticleResponse.from(article) : 
                    HelpArticleResponse.fromWithoutBody(article))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "Get help article by ID", description = "Get a specific help article by ID (admin only)")
    public ResponseEntity<HelpArticleResponse> getArticleById(
            @Parameter(description = "Article ID") @PathVariable Long id) {
        
        HelpArticle article = helpArticleService.getArticleById(id);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(HelpArticleResponse.from(article));
    }

    @PostMapping("/articles")
    @Operation(summary = "Create help article", description = "Create a new help article (admin only)")
    public ResponseEntity<HelpArticleResponse> createArticle(
            @Valid @RequestBody HelpArticleCreateRequest request,
            Authentication authentication) {
        
        HelpArticle article = new HelpArticle();
        article.setTitle(request.getTitle());
        article.setBodyMd(request.getBodyMd());
        article.setTags(request.getTags());
        article.setPublished(request.isPublished());
        article.setAuthorId(authentication.getName());
        
        HelpArticle savedArticle = helpArticleService.createArticle(article);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HelpArticleResponse.from(savedArticle));
    }

    @PutMapping("/articles/{id}")
    @Operation(summary = "Update help article", description = "Update an existing help article (admin only)")
    public ResponseEntity<HelpArticleResponse> updateArticle(
            @Parameter(description = "Article ID") @PathVariable Long id,
            @Valid @RequestBody HelpArticleCreateRequest request) {
        
        HelpArticle updatedArticle = new HelpArticle();
        updatedArticle.setTitle(request.getTitle());
        updatedArticle.setBodyMd(request.getBodyMd());
        updatedArticle.setTags(request.getTags());
        updatedArticle.setPublished(request.isPublished());
        
        HelpArticle savedArticle = helpArticleService.updateArticle(id, updatedArticle);
        return ResponseEntity.ok(HelpArticleResponse.from(savedArticle));
    }

    @DeleteMapping("/articles/{id}")
    @Operation(summary = "Delete help article", description = "Delete a help article (admin only)")
    public ResponseEntity<Void> deleteArticle(
            @Parameter(description = "Article ID") @PathVariable Long id) {
        
        helpArticleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/articles/{id}/publish")
    @Operation(summary = "Publish help article", description = "Publish a help article (admin only)")
    public ResponseEntity<HelpArticleResponse> publishArticle(
            @Parameter(description = "Article ID") @PathVariable Long id) {
        
        HelpArticle article = helpArticleService.publishArticle(id);
        return ResponseEntity.ok(HelpArticleResponse.from(article));
    }

    @PutMapping("/articles/{id}/unpublish")
    @Operation(summary = "Unpublish help article", description = "Unpublish a help article (admin only)")
    public ResponseEntity<HelpArticleResponse> unpublishArticle(
            @Parameter(description = "Article ID") @PathVariable Long id) {
        
        HelpArticle article = helpArticleService.unpublishArticle(id);
        return ResponseEntity.ok(HelpArticleResponse.from(article));
    }

    // System Statistics and Management
    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard data", description = "Get comprehensive dashboard statistics (admin only)")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> dashboard = ticketService.getTicketStatistics();
        
        // Add help article statistics
        dashboard.put("totalArticles", helpArticleService.getTotalArticleCount());
        dashboard.put("publishedArticles", helpArticleService.getPublishedArticleCount());
        dashboard.put("totalArticleViews", helpArticleService.getTotalViewCount());
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/tickets/all")
    @Operation(summary = "Get all tickets", description = "Get all tickets in the system (admin only)")
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> tickets = ticketService.getTicketsForQueue(null, null, null, pageable);
        
        List<TicketResponse> response = tickets.getContent().stream()
                .map(TicketResponse::fromWithoutMessages)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/articles/tags")
    @Operation(summary = "Get all article tags", description = "Get all tags used in articles including unpublished (admin only)")
    public ResponseEntity<List<String>> getAllArticleTags() {
        List<String> tags = helpArticleService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/articles/stats")
    @Operation(summary = "Get article statistics", description = "Get detailed article statistics (admin only)")
    public ResponseEntity<Map<String, Object>> getArticleStatistics() {
        Map<String, Object> stats = Map.of(
            "totalArticles", helpArticleService.getTotalArticleCount(),
            "publishedArticles", helpArticleService.getPublishedArticleCount(),
            "draftArticles", helpArticleService.getTotalArticleCount() - helpArticleService.getPublishedArticleCount(),
            "totalViews", helpArticleService.getTotalViewCount(),
            "totalTags", helpArticleService.getAllTags().size()
        );
        
        return ResponseEntity.ok(stats);
    }
}