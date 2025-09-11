package com.hopngo.booking.integration;

import com.hopngo.booking.config.TestContainersConfiguration;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testcontainers")
@Testcontainers
@Import(TestContainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public abstract class BaseIntegrationTest {
    
    // Common test setup and utilities can be added here
    
    protected static final String TEST_USER_ID = "test-user-123";
    protected static final String TEST_VENDOR_USER_ID = "test-vendor-456";
    protected static final String CUSTOMER_ROLE = "CUSTOMER";
    protected static final String VENDOR_ROLE = "VENDOR";
}