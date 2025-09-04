package com.hopngo.social.service;

import com.hopngo.social.dto.HeatmapResponse;
import com.hopngo.social.entity.Post;
import com.hopngo.social.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HeatmapService {
    
    @Autowired
    private PostRepository postRepository;
    
    public HeatmapResponse getHeatmap(double minLat, double maxLat, double minLng, double maxLng) {
        // For now, this is a stub implementation using geohash buckets
        // In a real implementation, you would use MongoDB aggregation pipeline
        
        List<Post> postsInBounds = postRepository.findPostsInBoundingBox(minLat, maxLat, minLng, maxLng);
        
        // Group posts by geohash (simplified to 2 decimal places for demo)
        Map<String, List<Post>> geohashGroups = postsInBounds.stream()
            .filter(post -> post.getLocation() != null)
            .collect(Collectors.groupingBy(post -> 
                generateSimpleGeohash(post.getLocation().getLat(), post.getLocation().getLng())
            ));
        
        // Convert to heatmap points
        List<HeatmapResponse.HeatmapPoint> points = geohashGroups.entrySet().stream()
            .map(entry -> {
                String geohash = entry.getKey();
                List<Post> posts = entry.getValue();
                
                // Calculate average position for the cluster
                double avgLat = posts.stream()
                    .mapToDouble(post -> post.getLocation().getLat())
                    .average()
                    .orElse(0.0);
                
                double avgLng = posts.stream()
                    .mapToDouble(post -> post.getLocation().getLng())
                    .average()
                    .orElse(0.0);
                
                return new HeatmapResponse.HeatmapPoint(
                    avgLat,
                    avgLng,
                    posts.size(),
                    geohash
                );
            })
            .collect(Collectors.toList());
        
        // Add some random points for demo purposes if no real data
        if (points.isEmpty()) {
            points = generateRandomHeatmapPoints(minLat, maxLat, minLng, maxLng);
        }
        
        return new HeatmapResponse(points);
    }
    
    private String generateSimpleGeohash(double lat, double lng) {
        // Simplified geohash - round to 2 decimal places
        // In a real implementation, use a proper geohash library
        return String.format("%.2f,%.2f", 
            Math.round(lat * 100.0) / 100.0, 
            Math.round(lng * 100.0) / 100.0
        );
    }
    
    private List<HeatmapResponse.HeatmapPoint> generateRandomHeatmapPoints(
            double minLat, double maxLat, double minLng, double maxLng) {
        
        Random random = new Random();
        List<HeatmapResponse.HeatmapPoint> points = new ArrayList<>();
        
        // Generate 5-15 random points within the bounding box
        int numPoints = 5 + random.nextInt(11);
        
        for (int i = 0; i < numPoints; i++) {
            double lat = minLat + (maxLat - minLat) * random.nextDouble();
            double lng = minLng + (maxLng - minLng) * random.nextDouble();
            int count = 1 + random.nextInt(20); // Random count between 1-20
            
            String geohash = generateSimpleGeohash(lat, lng);
            
            points.add(new HeatmapResponse.HeatmapPoint(lat, lng, count, geohash));
        }
        
        return points;
    }
}