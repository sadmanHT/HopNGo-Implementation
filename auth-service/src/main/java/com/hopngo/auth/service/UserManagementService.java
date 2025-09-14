package com.hopngo.auth.service;

import com.hopngo.auth.entity.User;
import com.hopngo.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@Transactional
public class UserManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    
    public UserManagementService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Ban a user by setting their active status to false
     */
    public void banUser(Long userId) {
        logger.info("Banning user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User is already banned");
        }
        user.setIsActive(false);
        userRepository.save(user);
        
        logger.info("User {} has been banned successfully", userId);
    }
    
    /**
     * Unban a user by setting their active status to true
     */
    public void unbanUser(Long userId) {
        logger.info("Unbanning user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        if (user.getIsActive()) {
            throw new RuntimeException("User is not banned");
        }
        user.setIsActive(true);
        userRepository.save(user);
        
        logger.info("User {} has been unbanned successfully", userId);
    }
    
    /**
     * Remove a post by calling the social service
     */
    public void removePost(Long postId) {
        logger.info("Removing post with ID: {}", postId);
        
        try {
            // Call social service to remove the post
            String socialServiceUrl = "http://localhost:8083/api/posts/" + postId;
            ResponseEntity<String> response = restTemplate.exchange(
                socialServiceUrl,
                HttpMethod.DELETE,
                null,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Post {} removed successfully", postId);
            } else {
                throw new RuntimeException("Failed to remove post from social service");
            }
        } catch (Exception e) {
            logger.error("Error removing post {}: {}", postId, e.getMessage());
            throw new RuntimeException("Failed to remove post: " + e.getMessage());
        }
    }
}