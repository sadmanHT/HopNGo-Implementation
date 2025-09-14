package com.hopngo.auth.service;

import com.hopngo.auth.dto.AuthResponse;
import com.hopngo.auth.dto.SocialLoginRequest;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.mapper.UserMapper;
import com.hopngo.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class SocialLoginService {
    
    private static final Logger logger = LoggerFactory.getLogger(SocialLoginService.class);
    
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final WebClient webClient;
    
    @Value("${oauth2.google.user-info-uri:https://www.googleapis.com/oauth2/v2/userinfo}")
    private String googleUserInfoUri;
    
    public SocialLoginService(UserRepository userRepository,
                             JwtService jwtService,
                             AuthService authService,
                             UserMapper userMapper,
                             WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authService = authService;
        this.userMapper = userMapper;
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * Handle social login (Google)
     */
    public AuthResponse handleSocialLogin(SocialLoginRequest request) {
        try {
            switch (request.getProvider().toLowerCase()) {
                case "google":
                    return handleGoogleLogin(request.getAccessToken());
                default:
                    throw new RuntimeException("Unsupported social login provider: " + request.getProvider());
            }
        } catch (Exception e) {
            logger.error("Social login failed for provider: {}", request.getProvider(), e);
            throw new RuntimeException("Social login failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle Google OAuth2 login
     */
    private AuthResponse handleGoogleLogin(String accessToken) {
        // Get user info from Google
        GoogleUserInfo googleUser = getUserInfoFromGoogle(accessToken);
        
        if (googleUser == null || googleUser.getEmail() == null) {
            throw new RuntimeException("Failed to retrieve user information from Google");
        }
        
        // Find or create user
        User user = findOrCreateUser(googleUser);
        
        // Generate JWT tokens
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = authService.generateRefreshToken(user);
        
        logger.info("Social login successful for user: {}", user.getEmail());
        
        return new AuthResponse(jwtToken, refreshToken, userMapper.toDto(user), false);
    }
    
    /**
     * Get user information from Google API
     */
    private GoogleUserInfo getUserInfoFromGoogle(String accessToken) {
        try {
            Mono<Map> response = webClient.get()
                    .uri(googleUserInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class);
            
            Map<String, Object> userInfo = response.block();
            
            if (userInfo == null) {
                throw new RuntimeException("No user information received from Google");
            }
            
            return new GoogleUserInfo(
                    (String) userInfo.get("id"),
                    (String) userInfo.get("email"),
                    (String) userInfo.get("given_name"),
                    (String) userInfo.get("family_name"),
                    (String) userInfo.get("picture"),
                    (Boolean) userInfo.get("verified_email")
            );
            
        } catch (Exception e) {
            logger.error("Failed to get user info from Google", e);
            throw new RuntimeException("Failed to verify Google access token");
        }
    }
    
    /**
     * Find existing user or create new one
     */
    private User findOrCreateUser(GoogleUserInfo googleUser) {
        Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Update user info if needed
            if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                user.setFirstName(googleUser.getGivenName());
            }
            if (user.getLastName() == null || user.getLastName().isEmpty()) {
                user.setLastName(googleUser.getFamilyName());
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } else {
            // Create new user
            User newUser = new User();
            newUser.setEmail(googleUser.getEmail());
            newUser.setFirstName(googleUser.getGivenName() != null ? googleUser.getGivenName() : "");
            newUser.setLastName(googleUser.getFamilyName() != null ? googleUser.getFamilyName() : "");
            newUser.setRole(User.Role.TRAVELER);
            newUser.setIsActive(true);
            newUser.setPassword(""); // No password for social login users
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            
            return userRepository.save(newUser);
        }
    }
    
    /**
     * Inner class to hold Google user information
     */
    private static class GoogleUserInfo {
        private final String id;
        private final String email;
        private final String givenName;
        private final String familyName;
        private final String picture;
        private final Boolean verifiedEmail;
        
        public GoogleUserInfo(String id, String email, String givenName, 
                             String familyName, String picture, Boolean verifiedEmail) {
            this.id = id;
            this.email = email;
            this.givenName = givenName;
            this.familyName = familyName;
            this.picture = picture;
            this.verifiedEmail = verifiedEmail;
        }
        
        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getGivenName() { return givenName; }
        public String getFamilyName() { return familyName; }
        public String getPicture() { return picture; }
        public Boolean getVerifiedEmail() { return verifiedEmail; }
    }
}