package com.hopngo.config.repository;

import com.hopngo.config.entity.Experiment;
import com.hopngo.config.entity.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    
    Optional<Experiment> findByKey(String key);
    
    List<Experiment> findByStatus(ExperimentStatus status);
    
    @Query("SELECT e FROM Experiment e WHERE e.status = :status AND e.trafficPct > 0")
    List<Experiment> findActiveExperiments(@Param("status") ExperimentStatus status);
    
    @Query("SELECT e FROM Experiment e WHERE e.key IN :keys")
    List<Experiment> findByKeys(@Param("keys") List<String> keys);
    
    boolean existsByKey(String key);
}