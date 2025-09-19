package com.hopngo.auth.security;

import com.hopngo.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Request Filter to validate JWT tokens in incoming requests
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    // List of public endpoints that should skip JWT validation
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/health",
        "/actuator/health",
        "/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                if (jwtService.validateToken(jwtToken)) {
                    // Extract user information from token
                    Long userId = jwtService.extractUserId(jwtToken);
                    String email = jwtService.extractEmail(jwtToken);
                    String rolesString = jwtService.extractRoles(jwtToken);
                    
                    // Convert roles string to authorities
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                            .collect(Collectors.toList());
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Add user info to request attributes for easy access
                    request.setAttribute("userId", userId);
                    request.setAttribute("userEmail", email);
                    request.setAttribute("userRoles", rolesString);
                }
            } catch (Exception e) {
                logger.error("Cannot set user authentication: {}", e.getMessage(), e);
            }
        }
        
        chain.doFilter(request, response);
    }
}