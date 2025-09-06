package com.hopngo.search.helper;

import com.hopngo.search.service.SearchClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.common.geo.GeoPoint;
import org.opensearch.common.unit.DistanceUnit;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.GeoDistanceSortBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for managing listings search index
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListingsIndexHelper {

    public static final String INDEX_NAME = "listings_v1";
    
    private final SearchClientService searchClientService;

    /**
     * Create the listings index with proper mapping
     */
    public boolean createIndex() {
        if (searchClientService.indexExists(INDEX_NAME)) {
            log.info("Listings index '{}' already exists", INDEX_NAME);
            return true;
        }

        Map<String, Object> mapping = createListingsMapping();
        return searchClientService.createIndex(INDEX_NAME, mapping);
    }

    /**
     * Index a single listing
     */
    public String indexListing(ListingDocument listing) {
        return searchClientService.indexDocument(INDEX_NAME, listing.getId(), listing);
    }

    /**
     * Bulk index multiple listings
     */
    public boolean bulkIndexListings(List<ListingDocument> listings) {
        Map<String, Object> documents = new HashMap<>();
        for (ListingDocument listing : listings) {
            documents.put(listing.getId(), listing);
        }
        return searchClientService.bulkIndex(INDEX_NAME, documents);
    }

    /**
     * Delete a listing from the index
     */
    public boolean deleteListing(String listingId) {
        return searchClientService.deleteDocument(INDEX_NAME, listingId);
    }

    /**
     * Search listings with text query and filters
     */
    public List<ListingDocument> searchListings(String query, String vendorId, List<String> amenities,
                                               Double minPrice, Double maxPrice, String currency,
                                               Double lat, Double lng, String radius,
                                               int size, int from) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Text search across content fields
        if (query != null && !query.trim().isEmpty()) {
            boolQuery.should(QueryBuilders.multiMatchQuery(query, "title", "description")
                    .boost(2.0f))
                    .should(QueryBuilders.matchQuery("title", query).boost(1.5f))
                    .should(QueryBuilders.matchQuery("description", query))
                    .minimumShouldMatch(1);
        }

        // Filter by vendor
        if (vendorId != null && !vendorId.trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("vendorId", vendorId));
        }

        // Filter by amenities
        if (amenities != null && !amenities.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("amenities", amenities));
        }

        // Price range filter
        if (minPrice != null || maxPrice != null) {
            var rangeQuery = QueryBuilders.rangeQuery("price");
            if (minPrice != null) {
                rangeQuery.gte(minPrice);
            }
            if (maxPrice != null) {
                rangeQuery.lte(maxPrice);
            }
            boolQuery.filter(rangeQuery);
        }

        // Currency filter
        if (currency != null && !currency.trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("currency", currency));
        }

        // Geo distance filter
        if (lat != null && lng != null && radius != null) {
            boolQuery.filter(QueryBuilders.geoDistanceQuery("geo")
                    .point(lat, lng)
                    .distance(radius, DistanceUnit.KILOMETERS));
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .size(size)
                .from(from);

        // Sort by distance if geo coordinates are provided
        if (lat != null && lng != null) {
            searchSourceBuilder.sort(new GeoDistanceSortBuilder("geo", lat, lng)
                    .order(SortOrder.ASC)
                    .unit(DistanceUnit.KILOMETERS));
        } else {
            // Default sort by rating descending
            searchSourceBuilder.sort("rating", SortOrder.DESC);
        }

        return searchClientService.search(INDEX_NAME, boolQuery, ListingDocument.class, size, from);
    }

    /**
     * Search listings near a location
     */
    public List<ListingDocument> searchNearby(double lat, double lng, String radius, 
                                             String query, int size, int from) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Geo distance query
        boolQuery.filter(QueryBuilders.geoDistanceQuery("geo")
                .point(lat, lng)
                .distance(radius, DistanceUnit.KILOMETERS));

        // Optional text query
        if (query != null && !query.trim().isEmpty()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(query, "title", "description"));
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .size(size)
                .from(from)
                .sort(new GeoDistanceSortBuilder("geo", lat, lng)
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));

        return searchClientService.search(INDEX_NAME, boolQuery, ListingDocument.class, size, from);
    }

    /**
     * Get listings count
     */
    public long getListingsCount() {
        return searchClientService.getDocumentCount(INDEX_NAME);
    }

    /**
     * Create the mapping for listings index
     */
    private Map<String, Object> createListingsMapping() {
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
        properties.put("vendorId", Map.of("type", "keyword"));
        properties.put("title", Map.of(
                "type", "text",
                "analyzer", "multilingual_analyzer"
        ));
        properties.put("description", Map.of(
                "type", "text",
                "analyzer", "multilingual_analyzer"
        ));
        properties.put("amenities", Map.of("type", "keyword"));
        properties.put("geo", Map.of("type", "geo_point"));
        properties.put("price", Map.of("type", "double"));
        properties.put("currency", Map.of("type", "keyword"));
        properties.put("rating", Map.of("type", "float"));
        properties.put("createdAt", Map.of("type", "date"));
        
        mappings.put("properties", properties);
        mapping.put("mappings", mappings);
        
        return mapping;
    }

    /**
     * Listing document structure for search
     */
    public static class ListingDocument {
        private String id;
        private String vendorId;
        private String title;
        private String description;
        private List<String> amenities;
        private GeoLocation geo;
        private Double price;
        private String currency;
        private Float rating;
        private String createdAt;

        // Constructors
        public ListingDocument() {}

        public ListingDocument(String id, String vendorId, String title, String description,
                              List<String> amenities, GeoLocation geo, Double price, 
                              String currency, Float rating, String createdAt) {
            this.id = id;
            this.vendorId = vendorId;
            this.title = title;
            this.description = description;
            this.amenities = amenities;
            this.geo = geo;
            this.price = price;
            this.currency = currency;
            this.rating = rating;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getVendorId() { return vendorId; }
        public void setVendorId(String vendorId) { this.vendorId = vendorId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getAmenities() { return amenities; }
        public void setAmenities(List<String> amenities) { this.amenities = amenities; }

        public GeoLocation getGeo() { return geo; }
        public void setGeo(GeoLocation geo) { this.geo = geo; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public Float getRating() { return rating; }
        public void setRating(Float rating) { this.rating = rating; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Geo location structure
     */
    public static class GeoLocation {
        private double lat;
        private double lon;

        public GeoLocation() {}

        public GeoLocation(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }
}