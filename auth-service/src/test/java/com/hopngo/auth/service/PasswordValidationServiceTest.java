package com.hopngo.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@ActiveProfiles("test")
class PasswordValidationServiceTest {

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Test
    void contextLoads() {
        // This test will pass if the service can be created successfully
        assert passwordValidationService != null;
    }

    @Test
    void testPasswordValidation() {
        var result = passwordValidationService.validatePassword("TestPassword123!", "test@example.com");
        assert result != null;
    }
}