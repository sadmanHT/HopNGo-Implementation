package com.hopngo.tripplanning.mapper;

import com.hopngo.tripplanning.dto.CreateSavedRouteRequest;
import com.hopngo.tripplanning.dto.SavedRouteResponse;
import com.hopngo.tripplanning.dto.UpdateSavedRouteRequest;
import com.hopngo.tripplanning.entity.SavedRoute;
import com.hopngo.tripplanning.util.JsonHelper;
import org.mapstruct.*;

import java.time.Instant;
import java.util.*;

@Mapper(componentModel = "spring", imports = {JsonHelper.class, UUID.class, Instant.class})
public interface SavedRouteMapper {

    // Mapping methods for creation
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", expression = "java(JsonHelper.toJson(req.getName()))")
    @Mapping(target = "mode", expression = "java(JsonHelper.toJson(req.getMode()))")
    SavedRoute toEntity(CreateSavedRouteRequest req);
    
    @AfterMapping
    default void formatEntity(@MappingTarget SavedRoute savedRoute, CreateSavedRouteRequest req, String userId) {
        savedRoute.setId(UUID.randomUUID());
        savedRoute.setUserId(userId);
        savedRoute.setWaypoints(JsonHelper.toJson(req.getWaypoints()));
        savedRoute.setCreatedAt(Instant.now());
        savedRoute.setUpdatedAt(Instant.now());
    }
    
    // Mapping method for updates
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", expression = "java(req.getName() != null ? JsonHelper.toJson(req.getName()) : null)")
    @Mapping(target = "mode", expression = "java(req.getMode() != null ? JsonHelper.toJson(req.getMode()) : null)")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget SavedRoute savedRoute, UpdateSavedRouteRequest req);
    
    @AfterMapping
    default void formatUpdateEntity(@MappingTarget SavedRoute savedRoute, UpdateSavedRouteRequest req) {
        if (req.getWaypoints() != null) {
            savedRoute.setWaypoints(JsonHelper.toJson(req.getWaypoints()));
        }
        savedRoute.setUpdatedAt(Instant.now());
    }
    
    // Mapping method for response
    @Mapping(target = "userId", expression = "java(JsonHelper.toJson(entity.getUserId()))")
    @Mapping(target = "name", expression = "java(JsonHelper.toJson(entity.getName()))")
    @Mapping(target = "mode", expression = "java(JsonHelper.toJson(entity.getMode()))")
    @Mapping(target = "waypoints", expression = "java(JsonHelper.jsonToList(entity.getWaypoints()))")
    SavedRouteResponse toResponse(SavedRoute entity);
    
    // Convenience method for creating entity with user ID
    default SavedRoute toEntity(CreateSavedRouteRequest req, String userId) {
        SavedRoute entity = toEntity(req);
        formatEntity(entity, req, userId);
        return entity;
    }
}