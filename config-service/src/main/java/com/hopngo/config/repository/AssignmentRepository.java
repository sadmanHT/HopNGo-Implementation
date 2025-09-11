package com.hopngo.config.repository;

import com.hopngo.config.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    Optional<Assignment> findByExperimentIdAndUserId(Long experimentId, String userId);
    
    List<Assignment> findByUserId(String userId);
    
    List<Assignment> findByExperimentId(Long experimentId);
    
    @Query("SELECT a FROM Assignment a WHERE a.experiment.key = :experimentKey AND a.userId = :userId")
    Optional<Assignment> findByExperimentKeyAndUserId(@Param("experimentKey") String experimentKey, @Param("userId") String userId);
    
    @Query("SELECT a FROM Assignment a WHERE a.experiment.key IN :experimentKeys AND a.userId = :userId")
    List<Assignment> findByExperimentKeysAndUserId(@Param("experimentKeys") List<String> experimentKeys, @Param("userId") String userId);
    
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.experiment.id = :experimentId")
    long countByExperimentId(@Param("experimentId") Long experimentId);
}