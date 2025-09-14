package com.hopngo.tripplanning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing user's itineraries and personalized recommendations")
public class ItineraryListResponse {

    @JsonProperty("itineraries")
    @Schema(description = "List of user's own itineraries")
    private List<ItineraryResponse> itineraries;

    @JsonProperty("recommendations")
    @Schema(description = "List of personalized itinerary recommendations")
    private List<ItineraryResponse> recommendations;

    @JsonProperty("totalElements")
    @Schema(description = "Total number of user's itineraries")
    private long totalElements;

    @JsonProperty("totalPages")
    @Schema(description = "Total number of pages")
    private int totalPages;

    @JsonProperty("currentPage")
    @Schema(description = "Current page number (0-based)")
    private int currentPage;

    @JsonProperty("pageSize")
    @Schema(description = "Number of items per page")
    private int pageSize;

    @JsonProperty("hasNext")
    @Schema(description = "Whether there are more pages available")
    private boolean hasNext;

    @JsonProperty("hasPrevious")
    @Schema(description = "Whether there are previous pages available")
    private boolean hasPrevious;

    // Default constructor
    public ItineraryListResponse() {
    }

    // Constructor with basic pagination
    public ItineraryListResponse(List<ItineraryResponse> itineraries, 
                               List<ItineraryResponse> recommendations,
                               long totalElements, int totalPages, 
                               int currentPage, int pageSize) {
        this.itineraries = itineraries;
        this.recommendations = recommendations;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }

    // Constructor without recommendations
    public ItineraryListResponse(List<ItineraryResponse> itineraries,
                               long totalElements, int totalPages, 
                               int currentPage, int pageSize) {
        this(itineraries, null, totalElements, totalPages, currentPage, pageSize);
    }

    // Getters and Setters
    public List<ItineraryResponse> getItineraries() {
        return itineraries;
    }

    public void setItineraries(List<ItineraryResponse> itineraries) {
        this.itineraries = itineraries;
    }

    public List<ItineraryResponse> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ItineraryResponse> recommendations) {
        this.recommendations = recommendations;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    // Utility methods
    public boolean hasRecommendations() {
        return recommendations != null && !recommendations.isEmpty();
    }

    public int getRecommendationCount() {
        return recommendations != null ? recommendations.size() : 0;
    }

    public int getItineraryCount() {
        return itineraries != null ? itineraries.size() : 0;
    }

    public boolean isEmpty() {
        return getItineraryCount() == 0 && getRecommendationCount() == 0;
    }

    @Override
    public String toString() {
        return "ItineraryListResponse{" +
                "itineraries=" + (itineraries != null ? itineraries.size() : 0) + " items" +
                ", recommendations=" + (recommendations != null ? recommendations.size() : 0) + " items" +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItineraryListResponse that = (ItineraryListResponse) o;

        if (totalElements != that.totalElements) return false;
        if (totalPages != that.totalPages) return false;
        if (currentPage != that.currentPage) return false;
        if (pageSize != that.pageSize) return false;
        if (hasNext != that.hasNext) return false;
        if (hasPrevious != that.hasPrevious) return false;
        if (!java.util.Objects.equals(itineraries, that.itineraries)) return false;
        return java.util.Objects.equals(recommendations, that.recommendations);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(itineraries, recommendations, totalElements, 
                totalPages, currentPage, pageSize, hasNext, hasPrevious);
    }
}