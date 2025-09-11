package com.hopngo.tripplanning.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.tripplanning.dto.CreateSavedRouteRequest;
import com.hopngo.tripplanning.dto.SavedRouteResponse;
import com.hopngo.tripplanning.dto.UpdateSavedRouteRequest;
import com.hopngo.tripplanning.entity.SavedRoute;
import org.mapstruct.*;

import java.time.Instant;
import java.util.*;

@Mapper(componentModel = "spring")
public interface SavedRouteMapper {
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

    // Mapping methods for creation
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SavedRoute toEntity(CreateSavedRouteRequest req);
    
    @AfterMapping
    default void formatEntity(@MappingTarget SavedRoute savedRoute, CreateSavedRouteRequest req, String userId) {
        savedRoute.setId(UUID.randomUUID());
        savedRoute.setUserId(userId);
        savedRoute.setWaypoints(toJson(req.getWaypoints()));
        savedRoute.setCreatedAt(Instant.now());
        savedRoute.setUpdatedAt(Instant.now());
    }
    
    // Mapping method for updates
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget SavedRoute savedRoute, UpdateSavedRouteRequest req);
    
    @AfterMapping
    default void formatUpdateEntity(@MappingTarget SavedRoute savedRoute, UpdateSavedRouteRequest req) {
        if (req.getWaypoints() != null) {
            savedRoute.setWaypoints(toJson(req.getWaypoints()));
        }
        savedRoute.setUpdatedAt(Instant.now());
    }
    
    // Mapping method for response
    @Mapping(target = "waypoints", expression = "java(jsonToList(entity.getWaypoints()))")
    SavedRouteResponse toResponse(SavedRoute entity);
    
    // Convenience method for creating entity with user ID
    default SavedRoute toEntity(CreateSavedRouteRequest req, String userId) {
        SavedRoute entity = toEntity(req);
        formatEntity(entity, req, userId);
        return entity;
    }
}