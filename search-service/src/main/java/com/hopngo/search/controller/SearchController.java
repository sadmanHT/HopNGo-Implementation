package com.hopngo.search.controller;

import com.hopngo.search.client.BookingServiceClient;
import com.hopngo.search.client.SocialServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SocialServiceClient socialServiceClient;

    @Autowired
    private BookingServiceClient bookingServiceClient;

    @GetMapping
    public ResponseEntity<Map<String, Object>> unifiedSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "checkIn", required = false) String checkIn,
            @RequestParam(value = "checkOut", required = false) String checkOut,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "amenities", required = false) String amenities,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        try {
            // Execute searches in parallel
            CompletableFuture<Map<String, Object>> postsSearch = CompletableFuture.supplyAsync(() -> {
                try {
                    return socialServiceClient.searchPosts(query, page, size, location, tags);
                } catch (Exception e) {
                    // Return empty result if service fails
                    Map<String, Object> emptyResult = new HashMap<>();
                    emptyResult.put("content", Collections.emptyList());
                    emptyResult.put("totalElements", 0);
                    emptyResult.put("totalPages", 0);
                    return emptyResult;
                }
            });

            CompletableFuture<Map<String, Object>> listingsSearch = CompletableFuture.supplyAsync(() -> {
                try {
                    return bookingServiceClient.searchListings(query, page, size, location, 
                            minPrice, maxPrice, checkIn, checkOut, guests, amenities);
                } catch (Exception e) {
                    // Return empty result if service fails
                    Map<String, Object> emptyResult = new HashMap<>();
                    emptyResult.put("content", Collections.emptyList());
                    emptyResult.put("totalElements", 0);
                    emptyResult.put("totalPages", 0);
                    return emptyResult;
                }
            });

            // Wait for both searches to complete
            Map<String, Object> postsResult = postsSearch.get();
            Map<String, Object> listingsResult = listingsSearch.get();

            // Combine results
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.put("posts", postsResult);
            combinedResult.put("listings", listingsResult);
            
            // Calculate total counts
            int totalPosts = (Integer) postsResult.getOrDefault("totalElements", 0);
            int totalListings = (Integer) listingsResult.getOrDefault("totalElements", 0);
            combinedResult.put("totalResults", totalPosts + totalListings);
            
            return ResponseEntity.ok(combinedResult);
            
        } catch (InterruptedException | ExecutionException e) {
            // Return error response
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Search service temporarily unavailable");
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        try {
            Map<String, Object> result = socialServiceClient.searchPosts(query, page, size, location, tags);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Posts search service temporarily unavailable");
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @GetMapping("/listings")
    public ResponseEntity<Map<String, Object>> searchListings(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "checkIn", required = false) String checkIn,
            @RequestParam(value = "checkOut", required = false) String checkOut,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "amenities", required = false) String amenities
    ) {
        try {
            Map<String, Object> result = bookingServiceClient.searchListings(query, page, size, location,
                    minPrice, maxPrice, checkIn, checkOut, guests, amenities);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Listings search service temporarily unavailable");
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestions(@RequestParam("q") String query) {
        try {
            // Get suggestions from both services in parallel
            CompletableFuture<Map<String, Object>> postSuggestions = CompletableFuture.supplyAsync(() -> {
                try {
                    return socialServiceClient.getPostSuggestions(query);
                } catch (Exception e) {
                    Map<String, Object> empty = new HashMap<>();
                    empty.put("suggestions", Collections.emptyList());
                    return empty;
                }
            });

            CompletableFuture<Map<String, Object>> listingSuggestions = CompletableFuture.supplyAsync(() -> {
                try {
                    return bookingServiceClient.getListingSuggestions(query);
                } catch (Exception e) {
                    Map<String, Object> empty = new HashMap<>();
                    empty.put("suggestions", Collections.emptyList());
                    return empty;
                }
            });

            // Combine suggestions
            Map<String, Object> postSuggestionsResult = postSuggestions.get();
            Map<String, Object> listingSuggestionsResult = listingSuggestions.get();

            List<String> allSuggestions = new ArrayList<>();
            
            @SuppressWarnings("unchecked")
            List<String> posts = (List<String>) postSuggestionsResult.getOrDefault("suggestions", Collections.emptyList());
            @SuppressWarnings("unchecked")
            List<String> listings = (List<String>) listingSuggestionsResult.getOrDefault("suggestions", Collections.emptyList());
            
            allSuggestions.addAll(posts);
            allSuggestions.addAll(listings);
            
            // Remove duplicates and limit to 10
            Set<String> uniqueSuggestions = new LinkedHashSet<>(allSuggestions);
            List<String> finalSuggestions = new ArrayList<>(uniqueSuggestions);
            if (finalSuggestions.size() > 10) {
                finalSuggestions = finalSuggestions.subList(0, 10);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("suggestions", finalSuggestions);
            return ResponseEntity.ok(result);
            
        } catch (InterruptedException | ExecutionException e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("suggestions", Collections.emptyList());
            return ResponseEntity.ok(errorResult); // Return empty suggestions on error
        }
    }

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        // This would typically be an admin-only endpoint
        // For now, return a simple acknowledgment
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Reindex request received");
        result.put("status", "processing");
        return ResponseEntity.ok(result);
    }
}