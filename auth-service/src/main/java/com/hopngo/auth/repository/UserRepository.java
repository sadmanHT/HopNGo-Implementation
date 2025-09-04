package com.hopngo.auth.repository;

import com.hopngo.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);
    
    /**
     * Check if user exists by email (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);
    
    /**
     * Find active users by role
     */
    List<User> findByRoleAndIsActiveTrue(User.Role role);
    
    /**
     * Find all active users
     */
    List<User> findByIsActiveTrue();
    
    /**
     * Find user by email and active status
     */
    Optional<User> findByEmailIgnoreCaseAndIsActiveTrue(String email);
    
    /**
     * Count users by role
     */
    long countByRole(User.Role role);
    
    /**
     * Count active users
     */
    long countByIsActiveTrue();
    
    /**
     * Find users by first name or last name containing (case-insensitive)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContaining(@Param("name") String name);
    
    /**
     * Find users created after a specific date
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedAfter(@Param("date") java.time.LocalDateTime date);
}