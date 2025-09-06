package com.hopngo.tripplanning.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.tripplanning.dto.CreateItineraryRequest;
import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.entity.Itinerary;
import org.mapstruct.*;

import java.time.Instant;
import java.util.*;

@Mapper(componentModel = "spring")
public interface ItineraryMapper {
    ObjectMapper JSON = new ObjectMapper();
    
    // JSON helpers
    default String toJson(Object value) {
        try { return value != null ? JSON.writeValueAsString(value) : null; }
        catch (Exception e) { throw new RuntimeException("JSON serialization failed", e); }
    }
    
    default List<Map<String, Object>> jsonToList(String json) {
        try { return json != null ? JSON.readValue(json, new TypeReference<>() {}) : Collections.emptyList(); }
        catch (Exception e) { throw new RuntimeException("JSON parsing failed", e); }
    }
    
    default Map<String, Object> jsonToMap(String json) {
        try { return json != null ? JSON.readValue(json, new TypeReference<>() {}) : Collections.emptyMap(); }
        catch (Exception e) { throw new RuntimeException("JSON parsing failed", e); }
    }

    // Mapping methods
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Itinerary toEntity(CreateItineraryRequest req, UUID userId);
    
    @AfterMapping
    default void formatEntity(@MappingTarget Itinerary itinerary, CreateItineraryRequest req, UUID userId) {
        itinerary.setId(UUID.randomUUID());
        itinerary.setUserId(userId.toString());
        itinerary.setOrigins(toJson(req.getOrigins()));
        itinerary.setDestinations(toJson(req.getDestinations()));
        itinerary.setPlan(toJson(req.getPlan()));
        itinerary.setCreatedAt(Instant.now());
        itinerary.setUpdatedAt(Instant.now());
    }
    
    @Mapping(target = "origins", expression = "java(jsonToList(entity.getOrigins()))")
    @Mapping(target = "destinations", expression = "java(jsonToList(entity.getDestinations()))")
    @Mapping(target = "plan", expression = "java(jsonToMap(entity.getPlan()))")
    ItineraryResponse toResponse(Itinerary entity);
}