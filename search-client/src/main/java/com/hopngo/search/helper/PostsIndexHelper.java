package com.hopngo.search.helper;

import com.hopngo.search.service.SearchClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for managing posts search index
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostsIndexHelper {

    public static final String INDEX_NAME = "posts_v1";

    private final SearchClientService searchClientService;

    /**
     * Create the posts index with proper mapping
     */
    public boolean createIndex() {
        if (searchClientService.indexExists(INDEX_NAME)) {
            log.info("Posts index '{}' already exists", INDEX_NAME);
            return true;
        }

        Map<String, Object> mapping = createPostsMapping();
        return searchClientService.createIndex(INDEX_NAME, mapping);
    }

    /**
     * Index a single post
     */
    public String indexPost(PostDocument post) {
        return searchClientService.indexDocument(INDEX_NAME, post.getId(), post);
    }

    /**
     * Bulk index multiple posts
     */
    public boolean bulkIndexPosts(List<PostDocument> posts) {
        Map<String, Object> documents = new HashMap<>();
        for (PostDocument post : posts) {
            documents.put(post.getId(), post);
        }
        return searchClientService.bulkIndex(INDEX_NAME, documents);
    }

    /**
     * Delete a post from the index
     */
    public boolean deletePost(String postId) {
        return searchClientService.deleteDocument(INDEX_NAME, postId);
    }

    /**
     * Search posts with text query and optional filters
     */
    public List<PostDocument> searchPosts(String query, String authorId, List<String> tags,
            String place, int size, int from) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Text search across content fields
        if (query != null && !query.trim().isEmpty()) {
            boolQuery.should(QueryBuilders.multiMatchQuery(query, "text", "place")
                    .boost(2.0f))
                    .should(QueryBuilders.matchQuery("text", query))
                    .minimumShouldMatch(1);
        }

        // Filter by author
        if (authorId != null && !authorId.trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("authorId", authorId));
        }

        // Filter by tags
        if (tags != null && !tags.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tags", tags));
        }

        // Filter by place
        if (place != null && !place.trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.matchQuery("place", place));
        }

        return searchClientService.search(INDEX_NAME, boolQuery, PostDocument.class, size, from);
    }

    /**
     * Hybrid search combining BM25 and vector similarity
     */
    public List<PostDocument> hybridSearch(String query, float[] embedding, String authorId,
            List<String> tags, int size, int from) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // BM25 text search
        if (query != null && !query.trim().isEmpty()) {
            boolQuery.should(QueryBuilders.multiMatchQuery(query, "text", "place")
                    .boost(1.0f));
        }

        // Vector similarity search (if embedding is provided)
        if (embedding != null && embedding.length > 0) {
            // Note: This is a placeholder for kNN search
            // In a real implementation, you would use OpenSearch's kNN plugin
            Map<String, Object> params = new HashMap<>();
            params.put("query_vector", embedding);
            Script script = new Script(
                    ScriptType.INLINE,
                    "painless",
                    "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
                    params
            );
            boolQuery.should(QueryBuilders.scriptScoreQuery(
                    QueryBuilders.matchAllQuery(),
                    script
            ).boost(0.5f));
        }

        // Apply filters
        if (authorId != null && !authorId.trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("authorId", authorId));
        }

        if (tags != null && !tags.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tags", tags));
        }

        return searchClientService.search(INDEX_NAME, boolQuery, PostDocument.class, size, from);
    }

    /**
     * Get posts count
     */
    public long getPostsCount() {
        return searchClientService.getDocumentCount(INDEX_NAME);
    }

    /**
     * Create the mapping for posts index
     */
    private Map<String, Object> createPostsMapping() {
        Map<String, Object> mapping = new HashMap<>();

        // Settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", 1);
        settings.put("number_of_replicas", 0);

        // Analysis settings for multilingual support
        Map<String, Object> analysis = new HashMap<>();
        Map<String, Object> analyzer = new HashMap<>();
        Map<String, Object> multilingualAnalyzer = new HashMap<>();
        multilingualAnalyzer.put("type", "custom");
        multilingualAnalyzer.put("tokenizer", "standard");
        multilingualAnalyzer.put("filter", List.of("lowercase", "stop"));
        analyzer.put("multilingual_analyzer", multilingualAnalyzer);
        analysis.put("analyzer", analyzer);
        settings.put("analysis", analysis);

        mapping.put("settings", settings);

        // Mappings
        Map<String, Object> mappings = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

        // Field mappings
        properties.put("id", Map.of("type", "keyword"));
        properties.put("authorId", Map.of("type", "keyword"));
        properties.put("text", Map.of(
                "type", "text",
                "analyzer", "multilingual_analyzer"
        ));
        properties.put("tags", Map.of("type", "keyword"));
        properties.put("place", Map.of("type", "text"));
        properties.put("createdAt", Map.of("type", "date"));
        properties.put("embedding", Map.of(
                "type", "dense_vector",
                "dims", 1536
        ));

        mappings.put("properties", properties);
        mapping.put("mappings", mappings);

        return mapping;
    }

    /**
     * Post document structure for search
     */
    public static class PostDocument {

        private String id;
        private String authorId;
        private String text;
        private List<String> tags;
        private String place;
        private String createdAt;
        private float[] embedding;

        // Constructors
        public PostDocument() {
        }

        public PostDocument(String id, String authorId, String text, List<String> tags,
                String place, String createdAt, float[] embedding) {
            this.id = id;
            this.authorId = authorId;
            this.text = text;
            this.tags = tags;
            this.place = place;
            this.createdAt = createdAt;
            this.embedding = embedding;
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAuthorId() {
            return authorId;
        }

        public void setAuthorId(String authorId) {
            this.authorId = authorId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }
    }
}
