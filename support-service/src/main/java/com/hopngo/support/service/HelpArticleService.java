package com.hopngo.support.service;

import com.hopngo.support.entity.HelpArticle;
import com.hopngo.support.repository.HelpArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class HelpArticleService {

    private final HelpArticleRepository helpArticleRepository;

    @Autowired
    public HelpArticleService(HelpArticleRepository helpArticleRepository) {
        this.helpArticleRepository = helpArticleRepository;
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getPublishedArticles(Pageable pageable) {
        return helpArticleRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getPublishedArticlesByTag(String tag, Pageable pageable) {
        return helpArticleRepository.findByPublishedTrueAndTagsContainingIgnoreCaseOrderByCreatedAtDesc(tag, pageable);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> searchPublishedArticles(String query, Pageable pageable) {
        return helpArticleRepository.searchPublishedArticles(query, pageable);
    }

    @Transactional(readOnly = true)
    public HelpArticle getPublishedArticleBySlug(String slug) {
        return helpArticleRepository.findBySlugAndPublishedTrue(slug).orElse(null);
    }

    @Transactional(readOnly = true)
    public HelpArticle getArticleById(Long id) {
        return helpArticleRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public HelpArticle getArticleBySlug(String slug) {
        return helpArticleRepository.findBySlug(slug).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getAllArticles(Pageable pageable) {
        return helpArticleRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getArticlesByAuthor(String authorId, Pageable pageable) {
        return helpArticleRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getPopularArticles(Pageable pageable) {
        return helpArticleRepository.findByPublishedTrueOrderByViewCountDescCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getRecentArticles(Pageable pageable) {
        return helpArticleRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticle> getRelatedArticles(Long articleId, Pageable pageable) {
        return helpArticleRepository.findRelatedArticles(articleId, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> getAllPublishedTags() {
        return helpArticleRepository.findAllPublishedTags()
                .stream()
                .flatMap(tagArray -> tagArray.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return helpArticleRepository.findAllTags()
                .stream()
                .flatMap(tagArray -> tagArray.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String query, int limit) {
        return helpArticleRepository.findTitleSuggestions(query, limit);
    }

    public void incrementViewCount(Long articleId) {
        helpArticleRepository.incrementViewCount(articleId);
    }

    public HelpArticle createArticle(HelpArticle article) {
        // Generate slug if not provided
        if (article.getSlug() == null || article.getSlug().trim().isEmpty()) {
            article.generateSlugFromTitle();
        }
        
        // Ensure slug is unique
        String originalSlug = article.getSlug();
        int counter = 1;
        while (helpArticleRepository.findBySlug(article.getSlug()).isPresent()) {
            article.setSlug(originalSlug + "-" + counter);
            counter++;
        }
        
        return helpArticleRepository.save(article);
    }

    public HelpArticle updateArticle(Long id, HelpArticle updatedArticle) {
        HelpArticle existingArticle = getArticleById(id);
        if (existingArticle == null) {
            throw new RuntimeException("Article not found with id: " + id);
        }
        
        // Update fields
        existingArticle.setTitle(updatedArticle.getTitle());
        existingArticle.setBodyMd(updatedArticle.getBodyMd());
        existingArticle.setTags(updatedArticle.getTags());
        existingArticle.setPublished(updatedArticle.isPublished());
        
        // Update slug if title changed
        if (!existingArticle.getTitle().equals(updatedArticle.getTitle())) {
            String newSlug = HelpArticle.generateSlugFromTitle(updatedArticle.getTitle());
            if (!newSlug.equals(existingArticle.getSlug())) {
                // Ensure new slug is unique
                String originalSlug = newSlug;
                int counter = 1;
                while (helpArticleRepository.findBySlug(newSlug).isPresent() && 
                       !newSlug.equals(existingArticle.getSlug())) {
                    newSlug = originalSlug + "-" + counter;
                    counter++;
                }
                existingArticle.setSlug(newSlug);
            }
        }
        
        return helpArticleRepository.save(existingArticle);
    }

    public void deleteArticle(Long id) {
        HelpArticle article = getArticleById(id);
        if (article == null) {
            throw new RuntimeException("Article not found with id: " + id);
        }
        
        helpArticleRepository.delete(article);
    }

    public HelpArticle publishArticle(Long id) {
        HelpArticle article = getArticleById(id);
        if (article == null) {
            throw new RuntimeException("Article not found with id: " + id);
        }
        
        article.setPublished(true);
        return helpArticleRepository.save(article);
    }

    public HelpArticle unpublishArticle(Long id) {
        HelpArticle article = getArticleById(id);
        if (article == null) {
            throw new RuntimeException("Article not found with id: " + id);
        }
        
        article.setPublished(false);
        return helpArticleRepository.save(article);
    }

    @Transactional(readOnly = true)
    public boolean canUserModifyArticle(HelpArticle article, String userId) {
        if (article == null || userId == null) {
            return false;
        }
        
        // Article author can modify
        if (userId.equals(article.getAuthorId())) {
            return true;
        }
        
        // Admin can modify any article (this would be checked at controller level with @PreAuthorize)
        return false;
    }

    @Transactional(readOnly = true)
    public long getPublishedArticleCount() {
        return helpArticleRepository.countByPublishedTrue();
    }

    @Transactional(readOnly = true)
    public long getTotalArticleCount() {
        return helpArticleRepository.count();
    }

    @Transactional(readOnly = true)
    public long getTotalViewCount() {
        return helpArticleRepository.sumViewCount();
    }
}