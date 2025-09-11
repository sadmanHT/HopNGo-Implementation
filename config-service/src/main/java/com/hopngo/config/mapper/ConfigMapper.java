package com.hopngo.config.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.config.dto.AssignmentDto;
import com.hopngo.config.dto.ExperimentDto;
import com.hopngo.config.dto.ExperimentVariantDto;
import com.hopngo.config.dto.FeatureFlagDto;
import com.hopngo.config.entity.Assignment;
import com.hopngo.config.entity.Experiment;
import com.hopngo.config.entity.ExperimentVariant;
import com.hopngo.config.entity.FeatureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Map;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ConfigMapper {
    
    // FeatureFlag mappings
    FeatureFlagDto toDto(FeatureFlag entity);
    
    FeatureFlag toEntity(FeatureFlagDto dto);
    
    List<FeatureFlagDto> toFeatureFlagDtos(List<FeatureFlag> entities);
    
    void updateFeatureFlagFromDto(FeatureFlagDto dto, @MappingTarget FeatureFlag entity);
    
    // Experiment mappings
    @Mapping(target = "variants", source = "variants")
    ExperimentDto toDto(Experiment entity);
    
    @Mapping(target = "variants", source = "variants")
    @Mapping(target = "assignments", ignore = true)
    Experiment toEntity(ExperimentDto dto);
    
    List<ExperimentDto> toExperimentDtos(List<Experiment> entities);
    
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    void updateExperimentFromDto(ExperimentDto dto, @MappingTarget Experiment entity);
    
    // ExperimentVariant mappings
    ExperimentVariantDto toDto(ExperimentVariant entity);
    
    @Mapping(target = "experiment", ignore = true)
    ExperimentVariant toEntity(ExperimentVariantDto dto);
    
    List<ExperimentVariantDto> toVariantDtos(List<ExperimentVariant> entities);
    
    // Assignment mappings
    @Mapping(target = "experimentKey", source = "experiment.key")
    AssignmentDto toDto(Assignment entity);
    
    @Mapping(target = "experiment", ignore = true)
    Assignment toEntity(AssignmentDto dto);
    
    List<AssignmentDto> toAssignmentDtos(List<Assignment> entities);
    
    // Custom mapping methods for JsonNode <-> Map conversion
    default JsonNode mapToJsonNode(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.valueToTree(map);
    }
    
    default Map<String, Object> mapToMap(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(jsonNode, Map.class);
    }
}