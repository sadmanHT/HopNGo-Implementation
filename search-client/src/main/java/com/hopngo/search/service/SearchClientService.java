package com.hopngo.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for OpenSearch operations including indexing, searching, and bulk operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchClientService {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    /**
     * Create an index with the given mapping
     */
    public boolean createIndex(String indexName, Map<String, Object> mapping) {
        try {
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            request.mapping(mapping);
            
            CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
            log.info("Index '{}' created successfully: {}", indexName, response.isAcknowledged());
            return response.isAcknowledged();
        } catch (IOException e) {
            log.error("Failed to create index '{}'", indexName, e);
            return false;
        }
    }

    /**
     * Check if an index exists
     */
    public boolean indexExists(String indexName) {
        try {
            GetIndexRequest request = new GetIndexRequest(indexName);
            return client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Failed to check if index '{}' exists", indexName, e);
            return false;
        }
    }

    /**
     * Index a single document
     */
    public String indexDocument(String indexName, String documentId, Object document) {
        try {
            IndexRequest request = new IndexRequest(indexName)
                    .id(documentId)
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON);
            
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            log.debug("Document indexed: {} in index '{}'", response.getId(), indexName);
            return response.getId();
        } catch (IOException e) {
            log.error("Failed to index document in '{}'", indexName, e);
            return null;
        }
    }

    /**
     * Bulk index multiple documents
     */
    public boolean bulkIndex(String indexName, Map<String, Object> documents) {
        try {
            BulkRequest bulkRequest = new BulkRequest();
            
            for (Map.Entry<String, Object> entry : documents.entrySet()) {
                IndexRequest indexRequest = new IndexRequest(indexName)
                        .id(entry.getKey())
                        .source(objectMapper.writeValueAsString(entry.getValue()), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
            
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            
            if (response.hasFailures()) {
                log.warn("Bulk indexing had failures: {}", response.buildFailureMessage());
                return false;
            }
            
            log.info("Bulk indexed {} documents in index '{}'", documents.size(), indexName);
            return true;
        } catch (IOException e) {
            log.error("Failed to bulk index documents in '{}'", indexName, e);
            return false;
        }
    }

    /**
     * Delete a document by ID
     */
    public boolean deleteDocument(String indexName, String documentId) {
        try {
            DeleteRequest request = new DeleteRequest(indexName, documentId);
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            
            log.debug("Document '{}' deleted from index '{}'", documentId, indexName);
            return response.getResult().name().equals("DELETED");
        } catch (IOException e) {
            log.error("Failed to delete document '{}' from index '{}'", documentId, indexName, e);
            return false;
        }
    }

    /**
     * Search documents using a query builder
     */
    public <T> List<T> search(String indexName, QueryBuilder query, Class<T> clazz, int size, int from) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(query)
                    .size(size)
                    .from(from);
            
            searchRequest.source(searchSourceBuilder);
            
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            
            List<T> results = new ArrayList<>();
            for (SearchHit hit : response.getHits().getHits()) {
                T document = objectMapper.readValue(hit.getSourceAsString(), clazz);
                results.add(document);
            }
            
            log.debug("Search returned {} results from index '{}'", results.size(), indexName);
            return results;
        } catch (IOException e) {
            log.error("Failed to search in index '{}'", indexName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Search documents with raw response for more control
     */
    public SearchResponse searchRaw(String indexName, SearchSourceBuilder searchSourceBuilder) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);
            
            return client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Failed to execute raw search in index '{}'", indexName, e);
            return null;
        }
    }

    /**
     * Get the total count of documents in an index
     */
    public long getDocumentCount(String indexName) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .size(0); // We only want the count
            
            searchRequest.source(searchSourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            
            return response.getHits().getTotalHits().value;
        } catch (IOException e) {
            log.error("Failed to get document count for index '{}'", indexName, e);
            return 0;
        }
    }
}