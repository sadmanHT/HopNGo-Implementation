package com.hopngo.auth.service;

import com.hopngo.auth.entity.User;
import com.hopngo.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for user management operations
 */
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Find user by username (using email)
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username);
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Create a new user
     */
    public User createUser(User user) {
        // Encode password before saving
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Update user information
     */
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Update user password
     */
    public void updatePassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Update last login time
     */
    public void updateLastLogin(String username) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Enable or disable user account
     */
    public void setUserEnabled(Long userId, boolean enabled) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(enabled);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Lock or unlock user account
     */
    public void setUserLocked(Long userId, boolean locked) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(!locked);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByEmailIgnoreCase(username);
    }
    
    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }
    
    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Delete user by ID
     */
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    /**
     * Enable 2FA for user
     */
    public void enable2FA(Long userId, String secret) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIs2faEnabled(true);
            user.setTotpSecret(secret);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Disable 2FA for user
     */
    public void disable2FA(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIs2faEnabled(false);
            user.setTotpSecret(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
}