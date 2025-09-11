package com.hopngo.auth.controller;

import com.hopngo.auth.dto.AuthResponse;
import com.hopngo.auth.dto.SocialLoginRequest;
import com.hopngo.auth.service.SocialLoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/social")
public class SocialLoginController {
    
    private final SocialLoginService socialLoginService;
    
    public SocialLoginController(SocialLoginService socialLoginService) {
        this.socialLoginService = socialLoginService;
    }
    
    /**
     * Social login endpoint (Google, Facebook, etc.)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = socialLoginService.handleSocialLogin(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Google OAuth2 login endpoint
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestParam String accessToken) {
        SocialLoginRequest request = new SocialLoginRequest("google", accessToken);
        AuthResponse response = socialLoginService.handleSocialLogin(request);
        return ResponseEntity.ok(response);
    }
}