package com.hopngo.auth.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.hopngo.auth.config.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testcontainers")
@Import(TestContainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Provider("AuthService")
@PactFolder("../frontend/pacts")
public class AuthProviderTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("user exists with valid credentials")
    void userExistsWithValidCredentials() {
        // Setup test data for valid login scenario
        // This would typically involve creating a test user in the database
        // For now, we'll rely on the existing test data setup
    }

    @State("user provides invalid credentials")
    void userProvidesInvalidCredentials() {
        // Setup for invalid credentials scenario
        // No specific setup needed as invalid credentials will naturally fail
    }

    @State("user registration data is valid")
    void userRegistrationDataIsValid() {
        // Setup for valid registration scenario
        // Ensure the email doesn't already exist in the test database
    }
}