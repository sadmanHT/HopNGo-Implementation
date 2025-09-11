package com.hopngo.config.service;

import com.hopngo.config.dto.AssignmentDto;
import com.hopngo.config.dto.ExperimentDto;
import com.hopngo.config.dto.ExperimentVariantDto;
import com.hopngo.config.entity.*;
import com.hopngo.config.mapper.ConfigMapper;
import com.hopngo.config.repository.AssignmentRepository;
import com.hopngo.config.repository.ExperimentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class ExperimentService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);
    
    private final ExperimentRepository experimentRepository;
    private final AssignmentRepository assignmentRepository;
    private final ConfigMapper mapper;
    private final Random random = new Random();
    
    @Autowired
    public ExperimentService(ExperimentRepository experimentRepository,
                           AssignmentRepository assignmentRepository,
                           ConfigMapper mapper) {
        this.experimentRepository = experimentRepository;
        this.assignmentRepository = assignmentRepository;
        this.mapper = mapper;
    }
    
    @Cacheable(value = "experiments", key = "'all'")
    @Transactional(readOnly = true)
    public List<ExperimentDto> getAllExperiments() {
        logger.debug("Fetching all experiments");
        List<Experiment> experiments = experimentRepository.findAll();
        return mapper.toExperimentDtos(experiments);
    }
    
    @Cacheable(value = "experiments", key = "'active'")
    @Transactional(readOnly = true)
    public List<ExperimentDto> getActiveExperiments() {
        logger.debug("Fetching active experiments");
        List<Experiment> experiments = experimentRepository.findActiveExperiments(ExperimentStatus.RUNNING);
        return mapper.toExperimentDtos(experiments);
    }
    
    @Cacheable(value = "experiment", key = "#key")
    @Transactional(readOnly = true)
    public Optional<ExperimentDto> getExperimentByKey(String key) {
        logger.debug("Fetching experiment by key: {}", key);
        return experimentRepository.findByKey(key)
                .map(mapper::toDto);
    }
    
    @Transactional(readOnly = true)
    public Page<ExperimentDto> getAllExperiments(Pageable pageable) {
        logger.debug("Fetching experiments with pagination: {}", pageable);
        return experimentRepository.findAll(pageable)
                .map(mapper::toDto);
    }
    
    @CacheEvict(value = {"experiments", "experiment"}, allEntries = true)
    public ExperimentDto createExperiment(ExperimentDto experimentDto) {
        logger.info("Creating experiment: {}", experimentDto.getKey());
        
        if (experimentRepository.existsByKey(experimentDto.getKey())) {
            throw new IllegalArgumentException("Experiment with key '" + experimentDto.getKey() + "' already exists");
        }
        
        validateExperimentVariants(experimentDto.getVariants());
        
        Experiment experiment = mapper.toEntity(experimentDto);
        
        // Set up bidirectional relationship for variants
        if (experiment.getVariants() != null) {
            experiment.getVariants().forEach(variant -> variant.setExperiment(experiment));
        }
        
        Experiment savedExperiment = experimentRepository.save(experiment);
        
        logger.info("Created experiment: {} with ID: {}", savedExperiment.getKey(), savedExperiment.getId());
        return mapper.toDto(savedExperiment);
    }
    
    @CacheEvict(value = {"experiments", "experiment"}, allEntries = true)
    public ExperimentDto updateExperiment(Long id, ExperimentDto experimentDto) {
        logger.info("Updating experiment with ID: {}", id);
        
        Experiment existingExperiment = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with ID: " + id));
        
        // Check if key is being changed and if new key already exists
        if (!existingExperiment.getKey().equals(experimentDto.getKey()) && 
            experimentRepository.existsByKey(experimentDto.getKey())) {
            throw new IllegalArgumentException("Experiment with key '" + experimentDto.getKey() + "' already exists");
        }
        
        if (experimentDto.getVariants() != null) {
            validateExperimentVariants(experimentDto.getVariants());
        }
        
        mapper.updateExperimentFromDto(experimentDto, existingExperiment);
        Experiment updatedExperiment = experimentRepository.save(existingExperiment);
        
        logger.info("Updated experiment: {}", updatedExperiment.getKey());
        return mapper.toDto(updatedExperiment);
    }
    
    @CacheEvict(value = {"experiments", "experiment"}, allEntries = true)
    public void deleteExperiment(Long id) {
        logger.info("Deleting experiment with ID: {}", id);
        
        if (!experimentRepository.existsById(id)) {
            throw new IllegalArgumentException("Experiment not found with ID: " + id);
        }
        
        experimentRepository.deleteById(id);
        logger.info("Deleted experiment with ID: {}", id);
    }
    
    @Transactional
    public AssignmentDto assignUserToExperiment(String experimentKey, String userId) {
        logger.debug("Assigning user {} to experiment: {}", userId, experimentKey);
        
        // Check if user already has an assignment
        Optional<Assignment> existingAssignment = assignmentRepository
                .findByExperimentKeyAndUserId(experimentKey, userId);
        
        if (existingAssignment.isPresent()) {
            logger.debug("User {} already assigned to experiment: {}", userId, experimentKey);
            return mapper.toDto(existingAssignment.get());
        }
        
        Experiment experiment = experimentRepository.findByKey(experimentKey)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found with key: " + experimentKey));
        
        if (experiment.getStatus() != ExperimentStatus.RUNNING) {
            throw new IllegalStateException("Experiment is not running: " + experimentKey);
        }
        
        // Check if user should be included in experiment based on traffic percentage
        if (!shouldIncludeUser(userId, experimentKey, experiment.getTrafficPct())) {
            logger.debug("User {} excluded from experiment {} due to traffic percentage", userId, experimentKey);
            return null;
        }
        
        // Assign user to a variant
        ExperimentVariant selectedVariant = selectVariantForUser(userId, experiment);
        
        Assignment assignment = new Assignment();
        assignment.setExperiment(experiment);
        assignment.setUserId(userId);
        assignment.setVariantName(selectedVariant.getName());
        
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        logger.info("Assigned user {} to variant {} in experiment {}", 
                   userId, selectedVariant.getName(), experimentKey);
        
        return mapper.toDto(savedAssignment);
    }
    
    @Transactional(readOnly = true)
    public List<AssignmentDto> getUserAssignments(String userId) {
        logger.debug("Fetching assignments for user: {}", userId);
        List<Assignment> assignments = assignmentRepository.findByUserId(userId);
        return mapper.toAssignmentDtos(assignments);
    }
    
    private void validateExperimentVariants(List<ExperimentVariantDto> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("At least one variant is required");
        }
        
        int totalWeight = variants.stream()
                .mapToInt(ExperimentVariantDto::getWeightPct)
                .sum();
        
        if (totalWeight != 100) {
            throw new IllegalArgumentException("Variant weights must sum to 100, got: " + totalWeight);
        }
    }
    
    private boolean shouldIncludeUser(String userId, String experimentKey, Integer trafficPct) {
        if (trafficPct == null || trafficPct >= 100) {
            return true;
        }
        
        // Use consistent hashing to determine if user should be included
        String hashInput = userId + ":" + experimentKey;
        int hash = Math.abs(hashInput.hashCode()) % 100;
        return hash < trafficPct;
    }
    
    private ExperimentVariant selectVariantForUser(String userId, Experiment experiment) {
        // Use consistent hashing to select variant
        String hashInput = userId + ":" + experiment.getKey() + ":variant";
        int hash = Math.abs(hashInput.hashCode()) % 100;
        
        int cumulativeWeight = 0;
        for (ExperimentVariant variant : experiment.getVariants()) {
            cumulativeWeight += variant.getWeightPct();
            if (hash < cumulativeWeight) {
                return variant;
            }
        }
        
        // Fallback to first variant (should not happen if weights sum to 100)
        return experiment.getVariants().get(0);
    }
}