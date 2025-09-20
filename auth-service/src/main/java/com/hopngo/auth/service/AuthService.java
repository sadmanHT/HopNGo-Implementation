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
    private final TwoFactorAuthService twoFactorAuthService;
    private final PasswordValidationService passwordValidationService;
    private final AccountLockoutService accountLockoutService;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;
    
    public AuthService(UserRepository userRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      UserFlagsRepository userFlagsRepository,
                      JwtService jwtService,
                      PasswordEncoder passwordEncoder,
                      UserMapper userMapper,
                      TwoFactorAuthService twoFactorAuthService,
                      PasswordValidationService passwordValidationService,
                      AccountLockoutService accountLockoutService,
                      RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userFlagsRepository = userFlagsRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.twoFactorAuthService = twoFactorAuthService;
        this.passwordValidationService = passwordValidationService;
        this.accountLockoutService = accountLockoutService;
        this.refreshTokenService = refreshTokenService;
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
        
        // Validate password
        PasswordValidationService.PasswordValidationResult validation = 
                passwordValidationService.validatePassword(request.getPassword(), request.getEmail());
        
        if (!validation.isValid()) {
            throw new RuntimeException("Password validation failed: " + 
                    String.join(", ", validation.getErrors()));
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(User.Role.TRAVELER);
        user.setIsActive(true);
        
        user = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", user.getId());
        
        // Create default user flags
        UserFlags userFlags = new UserFlags(user.getId());
        userFlagsRepository.save(userFlags);
        logger.info("Created default user flags for user ID: {}", user.getId());
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = generateRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }
    
    /**
     * Authenticate user login
     */
    public AuthResponse login(LoginRequest request, String ipAddress) {
        logger.info("Attempting login for email: {}", request.getEmail());
        
        // Check for account/IP lockout first
        AccountLockoutService.LockoutStatus lockoutStatus = 
                accountLockoutService.getLockoutStatus(request.getEmail(), ipAddress);
        
        if (lockoutStatus.isLocked()) {
            if (lockoutStatus.isUserLocked()) {
                throw new RuntimeException("Account is temporarily locked due to too many failed attempts. Try again in " + 
                        lockoutStatus.getUserLockoutRemainingSeconds() / 60 + " minutes.");
            }
            if (lockoutStatus.isIpLocked()) {
                throw new RuntimeException("IP address is temporarily blocked due to too many failed attempts. Try again in " + 
                        lockoutStatus.getIpLockoutRemainingSeconds() / 60 + " minutes.");
            }
        }
        
        // Find user by email
        User user = userRepository.findByEmailIgnoreCaseAndIsActiveTrue(request.getEmail())
                .orElse(null);
        
        // Check credentials
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Record failed attempt
            accountLockoutService.recordFailedAttempt(request.getEmail(), ipAddress);
            logger.warn("Invalid credentials attempt for email: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }
        
        // Check if 2FA is enabled
        if (user.is2faEnabled()) {
            // If 2FA code is not provided, return response indicating 2FA is required
            if (request.getTotpCode() == null || request.getTotpCode().trim().isEmpty()) {
                return new AuthResponse(null, null, null, true);
            }
            
            // Verify 2FA code
            boolean is2FAValid = twoFactorAuthService.verify2FA(user, request.getTotpCode(), request.isBackupCode());
            if (!is2FAValid) {
                // Record failed attempt for invalid 2FA
                accountLockoutService.recordFailedAttempt(request.getEmail(), ipAddress);
                throw new RuntimeException("Invalid 2FA code");
            }
        }
        
        // Record successful login (clears failed attempts)
        accountLockoutService.recordSuccessfulLogin(request.getEmail(), ipAddress);
        
        logger.info("User logged in successfully: {}", user.getId());
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = generateRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user), false);
    }
    
    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Attempting to refresh token");
        
        // Validate refresh token using RefreshTokenService
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));
        
        User user = refreshToken.getUser();
        
        // Check if user is still active
        if (!user.getIsActive()) {
            refreshTokenService.revokeAllUserTokens(user.getId());
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
        logger.info("User logout requested");
        
        refreshTokenService.invalidateRefreshToken(refreshToken);
        logger.info("Refresh token invalidated");
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
    public String generateRefreshToken(User user) {
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return refreshToken.getToken();
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
     * Find user by email (returns Optional)
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseAndIsActiveTrue(email);
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
     * Clean up expired refresh tokens for a user
     */
    private void cleanupExpiredTokens(User user) {
        refreshTokenService.cleanupExpiredTokens();
        logger.info("Cleaned up expired tokens for user: {}", user.getId());
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
        
        // Validate new password
        PasswordValidationService.PasswordValidationResult validation = 
                passwordValidationService.validatePassword(newPassword, user.getEmail());
        
        if (!validation.isValid()) {
            throw new RuntimeException("Password validation failed: " + 
                    String.join(", ", validation.getErrors()));
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Invalidate all refresh tokens to force re-login
        refreshTokenRepository.deleteByUser(user);
        
        logger.info("Password changed successfully for user: {}", userId);
    }
}