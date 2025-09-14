package com.hopngo.tripplanning.mapper;

import com.hopngo.tripplanning.dto.CreateItineraryRequest;
import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.entity.Itinerary;
import com.hopngo.tripplanning.util.JsonHelper;
import org.mapstruct.*;

import java.time.Instant;
import java.util.*;

@Mapper(componentModel = "spring", imports = {JsonHelper.class, UUID.class, Instant.class})
public interface ItineraryMapper {

    // Mapping methods
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userId", expression = "java(JsonHelper.toJson(userId))")
    @Mapping(target = "title", expression = "java(JsonHelper.toJson(req.getTitle()))")
    @Mapping(target = "origins", expression = "java(JsonHelper.toJson(req.getOrigins()))")
    @Mapping(target = "destinations", expression = "java(JsonHelper.toJson(req.getDestinations()))")
    @Mapping(target = "plan", expression = "java(JsonHelper.toJson(req.getPlan()))")
    Itinerary toEntity(CreateItineraryRequest req, UUID userId);
    
    @AfterMapping
    default void formatEntity(@MappingTarget Itinerary itinerary, CreateItineraryRequest req, UUID userId) {
        itinerary.setId(UUID.randomUUID());
        itinerary.setCreatedAt(Instant.now());
        itinerary.setUpdatedAt(Instant.now());
    }
    
    @Mapping(target = "userId", expression = "java(JsonHelper.toJson(entity.getUserId()))")
    @Mapping(target = "title", expression = "java(JsonHelper.toJson(entity.getTitle()))")
    @Mapping(target = "origins", expression = "java(JsonHelper.jsonToList(entity.getOrigins()))")
    @Mapping(target = "destinations", expression = "java(JsonHelper.jsonToList(entity.getDestinations()))")
    @Mapping(target = "plan", expression = "java(JsonHelper.jsonToMap(entity.getPlan()))")
    ItineraryResponse toResponse(Itinerary entity);
}