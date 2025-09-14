package com.hopngo.auth.config;

import com.hopngo.auth.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Enhanced Security Configuration with mandatory 2FA for providers and admins
 * Includes session management, refresh token rotation, and device tracking
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class Enhanced2FASecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private TwoFactorAuthenticationFilter twoFactorAuthenticationFilter;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Increased strength
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/health", "/actuator/health").permitAll()
                
                // 2FA setup endpoints (require basic auth)
                .requestMatchers("/api/auth/2fa/setup", "/api/auth/2fa/verify").authenticated()
                
                // Provider endpoints - require 2FA
                .requestMatchers("/api/provider/**").hasRole("PROVIDER")
                
                // Admin endpoints - require 2FA
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // User endpoints
                .requestMatchers("/api/user/**").hasAnyRole("USER", "PROVIDER", "ADMIN")
                
                // Session management endpoints
                .requestMatchers("/api/auth/sessions/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(customLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "refresh_token")
            );

        // Add custom filters
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(twoFactorAuthenticationFilter, JwtRequestFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://hopngo.com",
            "https://*.hopngo.com",
            "http://localhost:3000", // Development
            "http://localhost:8080"  // Development
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-CSRF-Token",
            "X-Device-ID",
            "X-2FA-Token"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public LogoutSuccessHandler customLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            // Invalidate all user sessions
            if (authentication != null && authentication.getName() != null) {
                sessionManagementService.invalidateAllUserSessions(authentication.getName());
                refreshTokenService.revokeAllUserTokens(authentication.getName());
            }
            
            response.setStatus(200);
            response.getWriter().write("{\"message\":\"Logout successful\"}");
            response.setContentType("application/json");
        };
    }
}