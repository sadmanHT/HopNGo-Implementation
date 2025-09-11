package com.hopngo.social.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.hopngo.social.config.TestContainersConfiguration;
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
@Provider("SocialService")
@PactFolder("../frontend/pacts")
public class SocialProviderTest {

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

    @State("user is authenticated and has valid post data")
    void userIsAuthenticatedAndHasValidPostData() {
        // Setup test data for valid post creation scenario
        // This would typically involve creating a test user and ensuring authentication
        // For now, we'll rely on the existing test data setup
    }

    @State("user provides invalid post content")
    void userProvidesInvalidPostContent() {
        // Setup for invalid post content scenario
        // No specific setup needed as invalid content will naturally fail validation
    }

    @State("user has posts in their feed")
    void userHasPostsInTheirFeed() {
        // Setup test data for feed retrieval with posts
        // This would typically involve creating test posts in the database
    }

    @State("user has no posts in their feed")
    void userHasNoPostsInTheirFeed() {
        // Setup for empty feed scenario
        // Ensure the test database has no posts for the test user
    }
}