package com.hopngo.auth.service;

import com.hopngo.auth.dto.*;
import com.hopngo.auth.entity.RefreshToken;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.entity.UserFlags;
import com.hopngo.auth.mapper.UserMapper;
import com.hopngo.auth.repository.RefreshTokenRepository;
import com.hopngo.auth.repository.UserRepository;
import com.hopngo.auth.repository.UserFlagsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserFlagsRepository userFlagsRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;
    
    public AuthService(UserRepository userRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      UserFlagsRepository userFlagsRepository,
                      JwtService jwtService,
                      PasswordEncoder passwordEncoder,
                      UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userFlagsRepository = userFlagsRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }
    
    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(User.Role.USER);
        user.setIsActive(true);
        
        user = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", user.getId());
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = generateRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }
    
    /**
     * Authenticate user login
     */
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting login for email: {}", request.getEmail());
        
        // Find user by email
        User user = userRepository.findByEmailIgnoreCaseAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Invalid password attempt for user: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }
        
        logger.info("User logged in successfully: {}", user.getId());
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = generateRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }
    
    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Attempting to refresh token");
        
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        
        // Check if token is expired
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired");
        }
        
        User user = refreshToken.getUser();
        
        // Check if user is still active
        if (!user.getIsActive()) {
            refreshTokenRepository.deleteByUser(user);
            throw new RuntimeException("User account is inactive");
        }
        
        logger.info("Token refreshed successfully for user: {}", user.getId());
        
        // Generate new access token
        String newAccessToken = jwtService.generateToken(user);
        
        return new AuthResponse(newAccessToken, refreshToken.getToken(), userMapper.toDto(user));
    }
    
    /**
     * Get current user information
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        logger.info("Fetching current user info for ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is inactive");
        }
        
        // Get user flags to include verified provider status
        UserFlags userFlags = userFlagsRepository.findByUserId(userId).orElse(null);
        boolean isVerifiedProvider = userFlags != null && userFlags.isVerifiedProvider();
        
        UserDto userDto = userMapper.toDto(user);
        userDto.setVerifiedProvider(isVerifiedProvider);
        
        return userDto;
    }
    
    /**
     * Logout user by invalidating refresh token
     */
    public void logout(String refreshToken) {
        logger.info("Attempting to logout user");
        
        Optional<RefreshToken> token = refreshTokenRepository.findByToken(refreshToken);
        if (token.isPresent()) {
            refreshTokenRepository.delete(token.get());
            logger.info("User logged out successfully");
        }
    }
    
    /**
     * Logout user from all devices
     */
    public void logoutAll(Long userId) {
        logger.info("Logging out user from all devices: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        refreshTokenRepository.deleteByUser(user);
        logger.info("User logged out from all devices successfully");
    }
    
    /**
     * Generate refresh token for user
     */
    private String generateRefreshToken(User user) {
        // Clean up expired tokens for this user
        cleanupExpiredTokens(user);
        
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        
        RefreshToken refreshToken = new RefreshToken(tokenValue, user, expiresAt);
        refreshTokenRepository.save(refreshToken);
        
        return tokenValue;
    }
    
    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseAndIsActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    /**
     * Clean up expired refresh tokens for user
     */
    private void cleanupExpiredTokens(User user) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByUser(user).stream()
                .filter(token -> token.getExpiresAt().isBefore(now))
                .forEach(refreshTokenRepository::delete);
    }
    

    
    /**
     * Validate user exists and is active
     */
    @Transactional(readOnly = true)
    public boolean validateUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getIsActive)
                .orElse(false);
    }
    
    /**
     * Change user password
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        logger.info("Attempting to change password for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Invalidate all refresh tokens to force re-login
        refreshTokenRepository.deleteByUser(user);
        
        logger.info("Password changed successfully for user: {}", userId);
    }
}