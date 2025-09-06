package com.hopngo.social.service;

import com.hopngo.social.entity.Post;
import com.hopngo.social.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ch.hsr.geohash.GeoHash;

@Service
public class GeohashMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeohashMigrationService.class);
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    public void backfillGeohashes() {
        logger.info("Starting geohash backfill migration...");
        
        // Find posts with location but no geohash
        Query query = new Query(Criteria.where("location").exists(true)
                .and("location.geohash").exists(false)
                .and("location.lat").exists(true)
                .and("location.lng").exists(true));
        
        long totalPosts = mongoTemplate.count(query, Post.class);
        logger.info("Found {} posts to update with geohash", totalPosts);
        
        if (totalPosts == 0) {
            logger.info("No posts need geohash backfill");
            return;
        }
        
        int batchSize = 100;
        int processed = 0;
        
        while (processed < totalPosts) {
            Query batchQuery = new Query(Criteria.where("location").exists(true)
                    .and("location.geohash").exists(false)
                    .and("location.lat").exists(true)
                    .and("location.lng").exists(true))
                    .limit(batchSize);
            
            var posts = mongoTemplate.find(batchQuery, Post.class);
            
            for (Post post : posts) {
                try {
                    Post.Location location = post.getLocation();
                    if (location != null && location.getLat() != 0 && location.getLng() != 0) {
                        String geohash = GeoHash.geoHashStringWithCharacterPrecision(
                                location.getLat(), location.getLng(), 6);
                        
                        // Update the geohash field directly in MongoDB
                        Query updateQuery = new Query(Criteria.where("id").is(post.getId()));
                        Update update = new Update().set("location.geohash", geohash);
                        mongoTemplate.updateFirst(updateQuery, update, Post.class);
                        
                        processed++;
                        
                        if (processed % 50 == 0) {
                            logger.info("Processed {}/{} posts", processed, totalPosts);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing post {}: {}", post.getId(), e.getMessage());
                }
            }
            
            if (posts.isEmpty()) {
                break; // No more posts to process
            }
        }
        
        logger.info("Geohash backfill migration completed. Processed {} posts", processed);
    }
    
    public long regenerateGeohashes(int precision) {
        logger.info("Regenerating geohashes with precision {}", precision);
        
        Query query = new Query(Criteria.where("location").exists(true)
                .and("location.lat").exists(true)
                .and("location.lng").exists(true));
        
        long totalPosts = mongoTemplate.count(query, Post.class);
        logger.info("Found {} posts to regenerate geohash", totalPosts);
        
        int batchSize = 100;
        int processed = 0;
        
        while (processed < totalPosts) {
            Query batchQuery = new Query(Criteria.where("location").exists(true)
                    .and("location.lat").exists(true)
                    .and("location.lng").exists(true))
                    .limit(batchSize)
                    .skip(processed);
            
            var posts = mongoTemplate.find(batchQuery, Post.class);
            
            for (Post post : posts) {
                try {
                    Post.Location location = post.getLocation();
                    if (location != null && location.getLat() != 0 && location.getLng() != 0) {
                        String geohash = GeoHash.geoHashStringWithCharacterPrecision(
                                location.getLat(), location.getLng(), precision);
                        
                        Query updateQuery = new Query(Criteria.where("id").is(post.getId()));
                        Update update = new Update().set("location.geohash", geohash);
                        mongoTemplate.updateFirst(updateQuery, update, Post.class);
                        
                        processed++;
                        
                        if (processed % 50 == 0) {
                            logger.info("Regenerated {}/{} posts", processed, totalPosts);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error regenerating geohash for post {}: {}", post.getId(), e.getMessage());
                }
            }
            
            if (posts.isEmpty()) {
                break;
            }
        }
        
        logger.info("Geohash regeneration completed. Processed {} posts", processed);
        return processed;
    }
}