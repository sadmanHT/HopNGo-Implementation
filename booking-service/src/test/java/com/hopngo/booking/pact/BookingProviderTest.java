package com.hopngo.booking.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.hopngo.booking.config.TestContainersConfiguration;
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
@Provider("BookingService")
@PactFolder("../frontend/pacts")
public class BookingProviderTest {

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

    @State("listings are available for search criteria")
    void listingsAreAvailableForSearchCriteria() {
        // Setup test data for successful listing search
        // This would typically involve creating test listings in the database
        // For now, we'll rely on the existing test data setup
    }

    @State("no listings match the search criteria")
    void noListingsMatchTheSearchCriteria() {
        // Setup for empty search results scenario
        // Ensure the test database has no matching listings for the search criteria
    }

    @State("user has valid booking data and listing exists")
    void userHasValidBookingDataAndListingExists() {
        // Setup test data for valid booking creation
        // This would typically involve creating a test listing and ensuring it's available
    }

    @State("user provides invalid booking data")
    void userProvidesInvalidBookingData() {
        // Setup for invalid booking data scenario
        // No specific setup needed as invalid data will naturally fail validation
    }
}