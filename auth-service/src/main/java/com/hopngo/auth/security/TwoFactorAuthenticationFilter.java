package com.hopngo.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.auth.model.User;
import com.hopngo.auth.service.TwoFactorAuthService;
import com.hopngo.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to enforce 2FA for providers and admins
 * Checks if user has completed 2FA verification for privileged operations
 */
@Component
public class TwoFactorAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String[] PROVIDER_PATHS = {
        "/api/provider/",
        "/api/admin/"
    };

    private static final String[] EXEMPT_PATHS = {
        "/api/auth/2fa/setup",
        "/api/auth/2fa/verify",
        "/api/auth/2fa/status",
        "/api/auth/logout",
        "/api/health"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip filter for exempt paths
        if (isExemptPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if this is a privileged path requiring 2FA
        if (requiresTwoFactor(requestPath)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userService.findByUsername(username);
                
                if (user != null && isPrivilegedUser(user)) {
                    // Check if user has 2FA enabled
                    if (!twoFactorAuthService.is2FAEnabled(user.getId())) {
                        sendTwoFactorRequiredResponse(response, "2FA_SETUP_REQUIRED", 
                            "Two-factor authentication setup is required for this account type");
                        return;
                    }
                    
                    // Check if current session has valid 2FA verification
                    String twoFactorToken = request.getHeader("X-2FA-Token");
                    if (twoFactorToken == null || !twoFactorAuthService.isValidSession2FA(user.getId(), twoFactorToken)) {
                        sendTwoFactorRequiredResponse(response, "2FA_VERIFICATION_REQUIRED", 
                            "Two-factor authentication verification required");
                        return;
                    }
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        for (String exemptPath : EXEMPT_PATHS) {
            if (path.startsWith(exemptPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresTwoFactor(String path) {
        for (String providerPath : PROVIDER_PATHS) {
            if (path.startsWith(providerPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPrivilegedUser(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> "PROVIDER".equals(role.getName()) || "ADMIN".equals(role.getName()));
    }

    private void sendTwoFactorRequiredResponse(HttpServletResponse response, String code, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", code);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        if ("2FA_SETUP_REQUIRED".equals(code)) {
            errorResponse.put("setupUrl", "/api/auth/2fa/setup");
        } else {
            errorResponse.put("verifyUrl", "/api/auth/2fa/verify");
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}