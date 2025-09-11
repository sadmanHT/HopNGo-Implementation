package com.hopngo.repository;

import com.hopngo.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    
    Optional<FeatureFlag> findByKey(String key);
    
    @Query("SELECT f FROM FeatureFlag f WHERE f.enabled = true")
    List<FeatureFlag> findAllEnabled();
    
    boolean existsByKey(String key);
}