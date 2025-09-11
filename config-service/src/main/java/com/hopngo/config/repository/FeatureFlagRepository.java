package com.hopngo.config.repository;

import com.hopngo.config.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    
    Optional<FeatureFlag> findByKey(String key);
    
    List<FeatureFlag> findByEnabledTrue();
    
    @Query("SELECT f FROM FeatureFlag f WHERE f.key IN :keys")
    List<FeatureFlag> findByKeys(@Param("keys") List<String> keys);
    
    boolean existsByKey(String key);
}