package com.hopngo.emergency.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.emergency.dto.EmergencyContactRequest;
import com.hopngo.emergency.dto.EmergencyTriggerRequest;
import com.hopngo.emergency.entity.EmergencyContact;
import com.hopngo.emergency.repository.EmergencyContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestEntityManager
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.stream.bindings.emergency-triggered.destination=emergency.triggered",
    "spring.security.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmergencyTriggerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EmergencyContactRepository contactRepository;
    
    @Autowired
    private OutputDestination outputDestination;
    
    @Autowired
    private EntityManager entityManager;
    
    private static final Logger logger = LoggerFactory.getLogger(EmergencyTriggerIntegrationTest.class);
    private static final String TEST_USER_ID = "test-user-123";
    private static final String USER_ID_HEADER = "X-User-Id";
    
    @BeforeEach
    void setUp() {
        // Clean up any existing data
        contactRepository.deleteAll();
        
        // Create test emergency contacts
        EmergencyContact primaryContact = new EmergencyContact(
            TEST_USER_ID,
            "John Doe",
            "+1234567890",
            "Spouse",
            true
        );
        
        EmergencyContact secondaryContact = new EmergencyContact(
            TEST_USER_ID,
            "Jane Smith",
            "+0987654321",
            "Sister",
            false
        );
        
        contactRepository.saveAndFlush(primaryContact);
        contactRepository.saveAndFlush(secondaryContact);
        
        // Verify contacts were saved
        long contactCount = contactRepository.count();
        logger.info("Number of contacts saved: {}", contactCount);
        
        List<EmergencyContact> savedContacts = contactRepository.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(TEST_USER_ID);
        logger.info("Contacts for user {}: {}", TEST_USER_ID, savedContacts.size());
    }
    
    @Test
    void testEmergencyTriggerEndToEnd() throws Exception {
        // Prepare emergency trigger request
        EmergencyTriggerRequest.Location location = new EmergencyTriggerRequest.Location();
        location.setLat(40.7128);
        location.setLng(-74.0060);
        
        EmergencyTriggerRequest request = new EmergencyTriggerRequest();
        request.setLocation(location);
        request.setNote("Help! I'm in trouble at Times Square!");
        
        // Trigger emergency via REST API
        var result = webTestClient.post()
                .uri("/emergency/trigger")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();
        
        // Capture response body for debugging
        String responseBody = result.expectBody(String.class)
                .returnResult()
                .getResponseBody();
        
        System.out.println("Response status: " + result.returnResult(String.class).getStatus());
        System.out.println("Response body: " + responseBody);
        
        // Now check the status
        result.expectStatus().isOk();
        
        // Parse response and verify
        assertThat(responseBody).contains("success");
        assertThat(responseBody).contains("Emergency notification sent to all contacts");
        
        // Verify message was published to message queue
        Message<byte[]> message = outputDestination.receive(5000, "emergency.triggered");
        assertThat(message).isNotNull();
        
        // Parse and verify the published event
        String eventJson = new String(message.getPayload());
        assertThat(eventJson).contains(TEST_USER_ID);
        assertThat(eventJson).contains("John Doe");
        assertThat(eventJson).contains("+1234567890");
        assertThat(eventJson).contains("Jane Smith");
        assertThat(eventJson).contains("+0987654321");
        assertThat(eventJson).contains("Help! I'm in trouble at Times Square!");
        assertThat(eventJson).contains("40.7128");
        assertThat(eventJson).contains("-74.006");
    }
    
    @Test
    void testEmergencyTriggerWithoutContacts() throws Exception {
        // Clean up contacts for this test
        contactRepository.deleteAll();
        
        EmergencyTriggerRequest.Location location = new EmergencyTriggerRequest.Location();
        location.setLat(40.7128);
        location.setLng(-74.0060);
        
        EmergencyTriggerRequest request = new EmergencyTriggerRequest();
        request.setLocation(location);
        request.setNote("Emergency without contacts");
        
        // Should return bad request when no contacts exist
        webTestClient.post()
                .uri("/emergency/trigger")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                 .expectBody()
                 .jsonPath("$.status").isEqualTo("error")
                 .jsonPath("$.message").isEqualTo("No emergency contacts configured. Please add emergency contacts before triggering.");
        
        // Verify no message was published
        Message<byte[]> message = outputDestination.receive(1000, "emergency.triggered");
        assertThat(message).isNull();
    }
    
    @Test
    void testEmergencyTriggerWithoutUserId() throws Exception {
        EmergencyTriggerRequest.Location location = new EmergencyTriggerRequest.Location();
        location.setLat(40.7128);
        location.setLng(-74.0060);
        
        EmergencyTriggerRequest request = new EmergencyTriggerRequest();
        request.setLocation(location);
        request.setNote("Emergency without user ID");
        
        // Should return unauthorized when X-User-Id header is missing
        webTestClient.post()
                .uri("/emergency/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }
    
    @Test
    void testCreateContactAndTriggerEmergency() throws Exception {
        // Clean up existing contacts
        contactRepository.deleteAll();
        
        // First, create an emergency contact
        EmergencyContactRequest contactRequest = new EmergencyContactRequest();
        contactRequest.setName("Emergency Contact");
        contactRequest.setPhone("+1555123456");
        contactRequest.setRelation("Friend");
        contactRequest.setIsPrimary(true);
        
        webTestClient.post()
                .uri("/emergency/contacts")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(contactRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Emergency Contact")
                .jsonPath("$.phone").isEqualTo("+1555123456");
        
        // Then trigger emergency
        EmergencyTriggerRequest.Location location = new EmergencyTriggerRequest.Location();
        location.setLat(37.7749);
        location.setLng(-122.4194);
        
        EmergencyTriggerRequest triggerRequest = new EmergencyTriggerRequest();
        triggerRequest.setLocation(location);
        triggerRequest.setNote("Emergency in San Francisco");
        
        webTestClient.post()
                .uri("/emergency/trigger")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(triggerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("success");
        
        // Verify the event was published with the new contact
        Message<byte[]> message = outputDestination.receive(5000, "emergency.triggered");
        assertThat(message).isNotNull();
        
        String eventJson = new String(message.getPayload());
        assertThat(eventJson).contains("Emergency Contact");
        assertThat(eventJson).contains("+1555123456");
        assertThat(eventJson).contains("Emergency in San Francisco");
    }
}