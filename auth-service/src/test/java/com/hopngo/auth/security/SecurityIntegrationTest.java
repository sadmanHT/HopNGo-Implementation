package com.hopngo.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.auth.dto.LoginRequest;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.repository.UserRepository;
import com.hopngo.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User travelerUser;
    private User serviceProviderUser;
    private String adminToken;
    private String travelerToken;
    private String serviceProviderToken;

    @BeforeEach
    void setUp() {
        // Create test users with different roles
        adminUser = createTestUser("admin@test.com", "Admin123!", User.Role.ADMIN);
        travelerUser = createTestUser("traveler@test.com", "Traveler123!", User.Role.TRAVELER);
        serviceProviderUser = createTestUser("provider@test.com", "Provider123!", User.Role.SERVICE_PROVIDER);

        // Generate JWT tokens for each user
        adminToken = jwtService.generateToken(adminUser);
        travelerToken = jwtService.generateToken(travelerUser);
        serviceProviderToken = jwtService.generateToken(serviceProviderUser);
    }

    private User createTestUser(String email, String password, User.Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    @Test
    void testPublicEndpointsAccessible() throws Exception {
        // Test login endpoint is accessible without authentication
        LoginRequest loginRequest = new LoginRequest("admin@test.com", "Admin123!");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Test register endpoint is accessible without authentication
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"newuser@test.com\",\"password\":\"NewUser123!\",\"firstName\":\"New\",\"lastName\":\"User\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminOnlyEndpointsWithAdminRole() throws Exception {
        // Admin should be able to access admin endpoints
        mockMvc.perform(post("/api/auth/admin/ban-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + travelerUser.getId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/admin/unban-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + travelerUser.getId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/auth/admin/remove-post/123")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminOnlyEndpointsWithTravelerRole() throws Exception {
        // Traveler should be denied access to admin endpoints
        mockMvc.perform(post("/api/auth/admin/ban-user")
                .header("Authorization", "Bearer " + travelerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + serviceProviderUser.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/admin/unban-user")
                .header("Authorization", "Bearer " + travelerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + serviceProviderUser.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/auth/admin/remove-post/123")
                .header("Authorization", "Bearer " + travelerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminOnlyEndpointsWithServiceProviderRole() throws Exception {
        // Service Provider should be denied access to admin endpoints
        mockMvc.perform(post("/api/auth/admin/ban-user")
                .header("Authorization", "Bearer " + serviceProviderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + travelerUser.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/admin/unban-user")
                .header("Authorization", "Bearer " + serviceProviderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + travelerUser.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/auth/admin/remove-post/123")
                .header("Authorization", "Bearer " + serviceProviderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminOnlyEndpointsWithoutAuthentication() throws Exception {
        // Unauthenticated requests should be denied
        mockMvc.perform(post("/api/auth/admin/ban-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + travelerUser.getId() + "}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/admin/unban-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + travelerUser.getId() + "}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/auth/admin/remove-post/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpointsWithValidToken() throws Exception {
        // All authenticated users should be able to access /me endpoint
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + travelerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + serviceProviderToken))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpointsWithInvalidToken() throws Exception {
        // Invalid token should be rejected
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpointsWithoutToken() throws Exception {
        // No token should be rejected
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshTokenEndpoint() throws Exception {
        // Test refresh token functionality
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isUnauthorized()); // Should fail with invalid token
    }

    @Test
    void testLogoutEndpoint() throws Exception {
        // Test logout functionality
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"some-refresh-token\"}"))
                .andExpect(status().isOk());

        // Test logout with missing refresh token
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}