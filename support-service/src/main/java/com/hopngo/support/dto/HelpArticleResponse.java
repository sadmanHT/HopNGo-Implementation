package com.hopngo.support.dto;

import com.hopngo.support.entity.HelpArticle;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class HelpArticleResponse {

    private Long id;
    private String slug;
    private String title;
    private String bodyMd;
    private List<String> tags;
    private Boolean published;
    private Integer viewCount;
    private String authorId;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public HelpArticleResponse() {}

    public HelpArticleResponse(HelpArticle article) {
        this.id = article.getId();
        this.slug = article.getSlug();
        this.title = article.getTitle();
        this.bodyMd = article.getBodyMd();
        this.tags = article.getTagsList();
        this.published = article.getPublished();
        this.viewCount = article.getViewCount();
        this.authorId = article.getAuthorId();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();
    }

    public HelpArticleResponse(HelpArticle article, boolean includeBody) {
        this(article);
        if (!includeBody) {
            this.bodyMd = null;
        }
    }

    // Static factory methods
    public static HelpArticleResponse from(HelpArticle article) {
        return new HelpArticleResponse(article);
    }

    public static HelpArticleResponse fromWithoutBody(HelpArticle article) {
        return new HelpArticleResponse(article, false);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyMd() {
        return bodyMd;
    }

    public void setBodyMd(String bodyMd) {
        this.bodyMd = bodyMd;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isPublished() {
        return Boolean.TRUE.equals(published);
    }

    public boolean isDraft() {
        return !isPublished();
    }

    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    public String getTagsAsString() {
        return tags != null ? String.join(", ", tags) : "";
    }

    @Override
    public String toString() {
        return "HelpArticleResponse{" +
                "id=" + id +
                ", slug='" + slug + '\'' +
                ", title='" + title + '\'' +
                ", published=" + published +
                ", viewCount=" + viewCount +
                ", authorId='" + authorId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}