package com.hopngo.auth.controller;

import com.hopngo.auth.dto.*;
import com.hopngo.auth.service.AuthService;
import com.hopngo.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final JwtService jwtService;
    
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            logger.info("Registration attempt for email: {}", request.getEmail());
            AuthResponse response = authService.register(request);
            logger.info("User registered successfully: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Registration failed for email: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Registration failed",
                    "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("Login attempt for email: {}", request.getEmail());
            String ipAddress = getClientIpAddress(httpRequest);
            AuthResponse response = authService.login(request, ipAddress);
            logger.info("User logged in successfully: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Authentication failed",
                    "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            logger.info("Token refresh attempt");
            AuthResponse response = authService.refreshToken(request);
            logger.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Token refresh failed",
                    "message", e.getMessage()
            ));
        }
    }
    

    
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Unauthorized",
                        "message", "Missing or invalid authorization header"
                ));
            }
            
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Unauthorized",
                        "message", "Invalid or expired token"
                ));
            }
            
            Long userId = jwtService.extractUserId(token);
            logger.info("Fetching user info for ID: {}", userId);
            
            UserDto user = authService.getCurrentUser(userId);
            logger.info("User info retrieved successfully for ID: {}", userId);
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidate refresh token and logout user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            logger.info("Logout attempt");
            authService.logout(request.getRefreshToken());
            logger.info("User logged out successfully");
            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Logout failed",
                    "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Invalidate all refresh tokens for the user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout from all devices successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<?> logoutAll(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null || !jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Unauthorized",
                        "message", "Invalid or missing token"
                ));
            }
            
            Long userId = jwtService.extractUserId(token);
            logger.info("Logout all attempt for user: {}", userId);
            
            authService.logoutAll(userId);
            logger.info("User logged out from all devices successfully: {}", userId);
            
            return ResponseEntity.ok(Map.of("message", "Logout from all devices successful"));
        } catch (Exception e) {
            logger.error("Logout all failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Logout failed",
                    "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate JWT token and return user info")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "valid", false,
                        "message", "Missing authorization header"
                ));
            }
            
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "valid", false,
                        "message", "Invalid or expired token"
                ));
            }
            
            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            String roles = jwtService.extractRoles(token);
            
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", userId,
                    "email", email,
                    "roles", roles
            ));
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "valid", false,
                    "message", "Token validation failed"
            ));
        }
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}