package com.hopngo.config.service;

import com.hopngo.config.dto.FeatureFlagDto;
import com.hopngo.config.entity.FeatureFlag;
import com.hopngo.config.mapper.ConfigMapper;
import com.hopngo.config.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FeatureFlagService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);
    
    private final FeatureFlagRepository repository;
    private final ConfigMapper mapper;
    
    @Autowired
    public FeatureFlagService(FeatureFlagRepository repository, ConfigMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    
    @Cacheable(value = "feature-flags", key = "'all'")
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getAllFlags() {
        logger.debug("Fetching all feature flags");
        List<FeatureFlag> flags = repository.findAll();
        return mapper.toFeatureFlagDtos(flags);
    }
    
    @Cacheable(value = "feature-flags", key = "'enabled'")
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getEnabledFlags() {
        logger.debug("Fetching enabled feature flags");
        List<FeatureFlag> flags = repository.findByEnabledTrue();
        return mapper.toFeatureFlagDtos(flags);
    }
    
    @Cacheable(value = "feature-flag", key = "#key")
    @Transactional(readOnly = true)
    public Optional<FeatureFlagDto> getFlagByKey(String key) {
        logger.debug("Fetching feature flag by key: {}", key);
        return repository.findByKey(key)
                .map(mapper::toDto);
    }
    
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getFlagsByKeys(List<String> keys) {
        logger.debug("Fetching feature flags by keys: {}", keys);
        List<FeatureFlag> flags = repository.findByKeys(keys);
        return mapper.toFeatureFlagDtos(flags);
    }
    
    @Transactional(readOnly = true)
    public Page<FeatureFlagDto> getAllFlags(Pageable pageable) {
        logger.debug("Fetching feature flags with pagination: {}", pageable);
        return repository.findAll(pageable)
                .map(mapper::toDto);
    }
    
    @CacheEvict(value = {"feature-flags", "feature-flag"}, allEntries = true)
    public FeatureFlagDto createFlag(FeatureFlagDto flagDto) {
        logger.info("Creating feature flag: {}", flagDto.getKey());
        
        if (repository.existsByKey(flagDto.getKey())) {
            throw new IllegalArgumentException("Feature flag with key '" + flagDto.getKey() + "' already exists");
        }
        
        FeatureFlag flag = mapper.toEntity(flagDto);
        FeatureFlag savedFlag = repository.save(flag);
        
        logger.info("Created feature flag: {} with ID: {}", savedFlag.getKey(), savedFlag.getId());
        return mapper.toDto(savedFlag);
    }
    
    @CacheEvict(value = {"feature-flags", "feature-flag"}, allEntries = true)
    public FeatureFlagDto updateFlag(Long id, FeatureFlagDto flagDto) {
        logger.info("Updating feature flag with ID: {}", id);
        
        FeatureFlag existingFlag = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with ID: " + id));
        
        // Check if key is being changed and if new key already exists
        if (!existingFlag.getKey().equals(flagDto.getKey()) && repository.existsByKey(flagDto.getKey())) {
            throw new IllegalArgumentException("Feature flag with key '" + flagDto.getKey() + "' already exists");
        }
        
        mapper.updateFeatureFlagFromDto(flagDto, existingFlag);
        FeatureFlag updatedFlag = repository.save(existingFlag);
        
        logger.info("Updated feature flag: {}", updatedFlag.getKey());
        return mapper.toDto(updatedFlag);
    }
    
    @CacheEvict(value = {"feature-flags", "feature-flag"}, allEntries = true)
    public void deleteFlag(Long id) {
        logger.info("Deleting feature flag with ID: {}", id);
        
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Feature flag not found with ID: " + id);
        }
        
        repository.deleteById(id);
        logger.info("Deleted feature flag with ID: {}", id);
    }
    
    @CacheEvict(value = {"feature-flags", "feature-flag"}, allEntries = true)
    public FeatureFlagDto toggleFlag(String key) {
        logger.info("Toggling feature flag: {}", key);
        
        FeatureFlag flag = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found with key: " + key));
        
        flag.setEnabled(!flag.getEnabled());
        FeatureFlag updatedFlag = repository.save(flag);
        
        logger.info("Toggled feature flag: {} to {}", key, updatedFlag.getEnabled());
        return mapper.toDto(updatedFlag);
    }
}