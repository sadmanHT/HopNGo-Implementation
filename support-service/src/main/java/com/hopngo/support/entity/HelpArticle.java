package com.hopngo.support.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "help_articles")
@EntityListeners(AuditingEntityListener.class)
public class HelpArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    @Column(nullable = false, unique = true)
    private String slug;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String title;

    @NotBlank(message = "Body is required")
    @Column(name = "body_md", nullable = false, columnDefinition = "TEXT")
    private String bodyMd;

    @Column(columnDefinition = "text[]")
    private String[] tags = new String[0];

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "author_id")
    private String authorId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public HelpArticle() {}

    public HelpArticle(String slug, String title, String bodyMd) {
        this.slug = slug;
        this.title = title;
        this.bodyMd = bodyMd;
    }

    public HelpArticle(String slug, String title, String bodyMd, String[] tags) {
        this.slug = slug;
        this.title = title;
        this.bodyMd = bodyMd;
        this.tags = tags != null ? tags : new String[0];
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

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags != null ? tags : new String[0];
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published != null ? published : false;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount != null ? viewCount : 0;
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
    public List<String> getTagsList() {
        return tags != null ? Arrays.asList(tags) : new ArrayList<>();
    }

    public void setTagsList(List<String> tagsList) {
        this.tags = tagsList != null ? tagsList.toArray(new String[0]) : new String[0];
    }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            List<String> tagsList = new ArrayList<>(getTagsList());
            if (!tagsList.contains(tag.trim().toLowerCase())) {
                tagsList.add(tag.trim().toLowerCase());
                setTagsList(tagsList);
            }
        }
    }

    public void removeTag(String tag) {
        if (tag != null) {
            List<String> tagsList = new ArrayList<>(getTagsList());
            tagsList.remove(tag.trim().toLowerCase());
            setTagsList(tagsList);
        }
    }

    public boolean hasTag(String tag) {
        return tag != null && getTagsList().contains(tag.trim().toLowerCase());
    }

    public boolean isPublished() {
        return Boolean.TRUE.equals(published);
    }

    public boolean isDraft() {
        return !isPublished();
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0) + 1;
    }

    public String generateSlugFromTitle() {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @Override
    public String toString() {
        return "HelpArticle{" +
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