package com.hopngo.social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class MongoIndexConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("posts");
        
        // Index on userId for user-specific queries
        indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC));
        
        // Index on createdAt for chronological sorting (descending for recent posts first)
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
        
        // Index on location.geohash for geospatial queries
        indexOps.ensureIndex(new Index().on("location.geohash", Sort.Direction.ASC));
        
        // Index on tags for tag-based filtering
        indexOps.ensureIndex(new Index().on("tags", Sort.Direction.ASC));
        
        // Compound index for feed queries (createdAt + visibility)
        indexOps.ensureIndex(new Index()
            .on("createdAt", Sort.Direction.DESC)
            .on("visibility", Sort.Direction.ASC));
        
        // Compound index for geospatial + time queries
        indexOps.ensureIndex(new Index()
            .on("location.geohash", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC));
        
        // Index on visibility for content moderation queries
        indexOps.ensureIndex(new Index().on("visibility", Sort.Direction.ASC));
    }
}