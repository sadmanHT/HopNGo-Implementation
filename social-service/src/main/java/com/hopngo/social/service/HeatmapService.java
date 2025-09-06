package com.hopngo.social.service;

import com.hopngo.social.dto.HeatmapResponse;
import com.hopngo.social.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HeatmapService {
    
    private static final Logger logger = LoggerFactory.getLogger(HeatmapService.class);
    private static final double TAU_HOURS = 72.0; // Time decay constant
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    public HeatmapResponse getHeatmap(double minLat, double maxLat, double minLng, double maxLng) {
        // Legacy method - delegate to new implementation with default parameters
        BoundingBoxFilter bbox = new BoundingBoxFilter(minLat, maxLat, minLng, maxLng);
        List<HeatmapCell> cells = generateHeatmap(bbox, 5, 0, null);
        
        // Convert to legacy format
        List<HeatmapResponse.HeatmapPoint> points = cells.stream()
            .map(cell -> new HeatmapResponse.HeatmapPoint(
                cell.getGeohash(), cell.getLat(), cell.getLng(), cell.getWeight(), cell.getTagsTop()))
            .collect(Collectors.toList());
        
        return new HeatmapResponse(points);
    }
    
    public static class HeatmapCell {
        private String geohash;
        private double lat;
        private double lng;
        private double weight;
        private List<String> tagsTop;
        
        public HeatmapCell(String geohash, double lat, double lng, double weight, List<String> tagsTop) {
            this.geohash = geohash;
            this.lat = lat;
            this.lng = lng;
            this.weight = weight;
            this.tagsTop = tagsTop;
        }
        
        // Getters
        public String getGeohash() { return geohash; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public double getWeight() { return weight; }
        public List<String> getTagsTop() { return tagsTop; }
    }
    
    public static class BoundingBoxFilter {
        private double minLat;
        private double maxLat;
        private double minLng;
        private double maxLng;
        
        public BoundingBoxFilter(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }
        
        public double getMinLat() { return minLat; }
        public double getMaxLat() { return maxLat; }
        public double getMinLng() { return minLng; }
        public double getMaxLng() { return maxLng; }
    }
    
    public List<HeatmapCell> generateHeatmap(BoundingBoxFilter bbox, int precision, int sinceHours, String tag) {
        logger.debug("Generating heatmap: bbox={},{},{},{}, precision={}, sinceHours={}, tag={}", 
                bbox.getMinLat(), bbox.getMaxLat(), bbox.getMinLng(), bbox.getMaxLng(), precision, sinceHours, tag);
        
        // Build query criteria
        Criteria criteria = buildQueryCriteria(bbox, sinceHours, tag);
        Query query = new Query(criteria);
        
        // Fetch posts
        List<Post> posts = mongoTemplate.find(query, Post.class);
        logger.debug("Found {} posts for heatmap generation", posts.size());
        
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Group posts by geohash at specified precision
        Map<String, List<Post>> geohashGroups = groupPostsByGeohash(posts, precision);
        
        // Calculate weights and aggregate data
        List<HeatmapCell> cells = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, List<Post>> entry : geohashGroups.entrySet()) {
            String geohash = entry.getKey();
            List<Post> groupPosts = entry.getValue();
            
            // Calculate time-decayed weight
            double totalWeight = calculateTimeDecayedWeight(groupPosts, now);
            
            // Get representative lat/lng for the geohash
            GeoHash geoHashObj = GeoHash.fromGeohashString(geohash);
            ch.hsr.geohash.BoundingBox bboxGeo = geoHashObj.getBoundingBox();
            double centerLat = (bboxGeo.getNorthLatitude() + bboxGeo.getSouthLatitude()) / 2.0;
            double centerLng = (bboxGeo.getEastLongitude() + bboxGeo.getWestLongitude()) / 2.0;
            
            // Get top tags
            List<String> topTags = getTopTags(groupPosts, 2);
            
            cells.add(new HeatmapCell(geohash, centerLat, centerLng, totalWeight, topTags));
        }
        
        // Sort by weight descending
        cells.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        
        logger.debug("Generated {} heatmap cells", cells.size());
        return cells;
    }
    
    private Criteria buildQueryCriteria(BoundingBoxFilter bbox, int sinceHours, String tag) {
        Criteria criteria = new Criteria();
        
        // Location exists and within bounding box
        criteria.and("location").exists(true)
                .and("location.lat").gte(bbox.getMinLat()).lte(bbox.getMaxLat())
                .and("location.lng").gte(bbox.getMinLng()).lte(bbox.getMaxLng());
        
        // Time filter
        if (sinceHours > 0) {
            LocalDateTime since = LocalDateTime.now().minusHours(sinceHours);
            criteria.and("createdAt").gte(since);
        }
        
        // Tag filter
        if (tag != null && !tag.trim().isEmpty()) {
            criteria.and("tags").in(tag.trim().toLowerCase());
        }
        
        // Only public posts
        criteria.and("visibility").is(Post.Visibility.PUBLIC);
        
        return criteria;
    }
    
    private Map<String, List<Post>> groupPostsByGeohash(List<Post> posts, int precision) {
        Map<String, List<Post>> groups = new HashMap<>();
        
        for (Post post : posts) {
            Post.Location location = post.getLocation();
            if (location != null && location.getLat() != 0 && location.getLng() != 0) {
                try {
                    // Generate geohash at specified precision
                    String geohash = GeoHash.geoHashStringWithCharacterPrecision(
                            location.getLat(), location.getLng(), precision);
                    
                    groups.computeIfAbsent(geohash, k -> new ArrayList<>()).add(post);
                } catch (Exception e) {
                    logger.warn("Error generating geohash for post {}: {}", post.getId(), e.getMessage());
                }
            }
        }
        
        return groups;
    }
    
    private double calculateTimeDecayedWeight(List<Post> posts, LocalDateTime now) {
        double totalWeight = 0.0;
        
        for (Post post : posts) {
            if (post.getCreatedAt() != null) {
                // Calculate hours since creation
                double hoursSinceCreation = ChronoUnit.MINUTES.between(post.getCreatedAt(), now) / 60.0;
                
                // Apply exponential decay: weight = e^(-(now - createdAt)/tau)
                double weight = Math.exp(-hoursSinceCreation / TAU_HOURS);
                totalWeight += weight;
            } else {
                // If no creation time, give minimal weight
                totalWeight += 0.1;
            }
        }
        
        return totalWeight;
    }
    
    private List<String> getTopTags(List<Post> posts, int limit) {
        Map<String, Integer> tagCounts = new HashMap<>();
        
        for (Post post : posts) {
            if (post.getTags() != null) {
                for (String tag : post.getTags()) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        tagCounts.merge(tag.toLowerCase(), 1, Integer::sum);
                    }
                }
            }
        }
        
        return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    public static BoundingBoxFilter parseBoundingBox(String bboxStr) {
        if (bboxStr == null || bboxStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Bounding box parameter is required");
        }
        
        try {
            String[] parts = bboxStr.split(",");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Bounding box must have 4 coordinates: minLng,minLat,maxLng,maxLat");
            }
            
            double minLng = Double.parseDouble(parts[0].trim());
            double minLat = Double.parseDouble(parts[1].trim());
            double maxLng = Double.parseDouble(parts[2].trim());
            double maxLat = Double.parseDouble(parts[3].trim());
            
            return new BoundingBoxFilter(minLat, maxLat, minLng, maxLng);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bounding box coordinates: " + bboxStr);
        }
    }
}