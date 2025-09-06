package com.hopngo.auth.repository;

import com.hopngo.auth.entity.UserFlags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFlagsRepository extends JpaRepository<UserFlags, Long> {
    
    /**
     * Find user flags by user ID
     */
    Optional<UserFlags> findByUserId(Long userId);
    
    /**
     * Find all verified providers
     */
    @Query("SELECT uf FROM UserFlags uf WHERE uf.verifiedProvider = true AND uf.banned = false")
    List<UserFlags> findAllVerifiedProviders();
    
    /**
     * Find all banned users
     */
    @Query("SELECT uf FROM UserFlags uf WHERE uf.banned = true")
    List<UserFlags> findAllBannedUsers();
    
    /**
     * Check if user is verified provider
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFlags uf WHERE uf.userId = :userId AND uf.verifiedProvider = true AND uf.banned = false")
    boolean isVerifiedProvider(@Param("userId") Long userId);
    
    /**
     * Check if user is banned
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFlags uf WHERE uf.userId = :userId AND uf.banned = true")
    boolean isBanned(@Param("userId") Long userId);
    
    /**
     * Update verified provider status
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFlags uf SET uf.verifiedProvider = :verified WHERE uf.userId = :userId")
    int updateVerifiedProvider(@Param("userId") Long userId, @Param("verified") Boolean verified);
    
    /**
     * Update banned status
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFlags uf SET uf.banned = :banned WHERE uf.userId = :userId")
    int updateBannedStatus(@Param("userId") Long userId, @Param("banned") Boolean banned);
    
    /**
     * Create or update user flags
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_flags (user_id, verified_provider, banned, created_at, updated_at) " +
                   "VALUES (:userId, :verifiedProvider, :banned, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                   "ON CONFLICT (user_id) DO UPDATE SET " +
                   "verified_provider = :verifiedProvider, " +
                   "banned = :banned, " +
                   "updated_at = CURRENT_TIMESTAMP", 
           nativeQuery = true)
    int upsertUserFlags(@Param("userId") Long userId, 
                       @Param("verifiedProvider") Boolean verifiedProvider, 
                       @Param("banned") Boolean banned);
    
    /**
     * Find verified providers
     */
    @Query("SELECT uf FROM UserFlags uf WHERE uf.verifiedProvider = true")
    List<UserFlags> findVerifiedProviders();
    
    /**
     * Count verified providers
     */
    @Query("SELECT COUNT(uf) FROM UserFlags uf WHERE uf.verifiedProvider = true")
    long countVerifiedProviders();
    
    /**
     * Count banned users
     */
    @Query("SELECT COUNT(uf) FROM UserFlags uf WHERE uf.banned = true")
    long countBannedUsers();
}