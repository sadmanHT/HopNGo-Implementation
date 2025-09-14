package com.hopngo.tripplanning.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.tripplanning.dto.*;
import com.hopngo.tripplanning.entity.Itinerary;
import com.hopngo.tripplanning.repository.ItineraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.mockito.Mockito;
import com.hopngo.tripplanning.service.AIServiceClient;

import java.util.*;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for the complete trip planning workflow:
 * Planning -> Saving -> Retrieving -> Modifying -> Rating -> Deleting
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class TripPlanningWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private AIServiceClient aiServiceClient;

    @MockBean
    private com.hopngo.tripplanning.service.RecommendationService recommendationService;
    
    @MockBean
    private com.hopngo.tripplanning.service.AIService aiService;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID format
    private static final String BASE_URL = "/api/v1/trips";
    private static UUID createdItineraryId;

    @BeforeEach
    void setUp() {
        // Clean up test data - delete all itineraries for test user
        itineraryRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID, Pageable.unpaged())
                .getContent().forEach(itinerary -> itineraryRepository.delete(itinerary));
        
        // Mock AIServiceClient for AI service calls
        Mockito.when(aiServiceClient.getNextDestinationSuggestion(
            Mockito.anyString(), 
            Mockito.anyList(), 
            Mockito.anyString(), 
            Mockito.anyInt(), 
            Mockito.anyInt()
        )).thenReturn("Mock AI destination suggestion");
        
        Mockito.when(aiServiceClient.getTravelTips(
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyInt()
        )).thenReturn("Mock AI travel tips");
        
        // Mock recommendation service with sample recommendations
        List<ItineraryResponse> mockRecommendations = Arrays.asList(
                new ItineraryResponse(UUID.randomUUID(), TEST_USER_ID, "Recommended Trip 1", 5, 100000, 
                        Arrays.asList(), Arrays.asList(), Map.of(), Instant.now(), Instant.now())
        );
        Mockito.when(recommendationService.getRecommendationsForUser(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(mockRecommendations);
    }

    @Test
    @Order(1)
    void testCompleteWorkflow_PlanTrip() throws Exception {
        // Step 1: Plan a new trip
        TripPlanRequest planRequest = createTripPlanRequest();
        
        MvcResult result = mockMvc.perform(post(BASE_URL + "/planTrip")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("\"" + TEST_USER_ID + "\""))
                .andExpect(jsonPath("$.title").value("\"European Adventure\""))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.budget").value(150000))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ItineraryResponse response = objectMapper.readValue(responseContent, ItineraryResponse.class);
        createdItineraryId = response.getId();
        
        assertNotNull(createdItineraryId);
        assertNotNull(response.getCreatedAt());
        
        // Verify itinerary was saved in database
        Optional<Itinerary> savedItinerary = itineraryRepository.findById(createdItineraryId);
        assertTrue(savedItinerary.isPresent());
        assertEquals(TEST_USER_ID, savedItinerary.get().getUserId());
    }

    @Test
    @Order(2)
    void testCompleteWorkflow_RetrieveItineraries() throws Exception {
        // First create an itinerary
        createTestItinerary();
        
        // Step 2: Retrieve user itineraries
        mockMvc.perform(get(BASE_URL + "/itineraries")
                .header("X-User-Id", TEST_USER_ID)
                .param("userId", TEST_USER_ID)
                .param("page", "0")
                .param("size", "10")
                .param("includeRecommendations", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itineraries").isArray())
                .andExpect(jsonPath("$.itineraries[0].userId").value("\"" + TEST_USER_ID + "\""))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.recommendations").exists());
    }

    @Test
    @Order(3)
    void testCompleteWorkflow_UpdateItinerary() throws Exception {
        // First create an itinerary
        UUID itineraryId = createTestItinerary();
        
        // Step 3: Update the itinerary
        ItineraryUpdateRequest updateRequest = new ItineraryUpdateRequest();
        updateRequest.setTitle("Updated European Adventure");
        updateRequest.setBudget(200000);
        
        mockMvc.perform(patch(BASE_URL + "/" + itineraryId)
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("\"Updated European Adventure\""))
                .andExpect(jsonPath("$.budget").value(200000));
        
        // Verify update in database
        Optional<Itinerary> updatedItinerary = itineraryRepository.findById(itineraryId);
        assertTrue(updatedItinerary.isPresent());
        assertEquals("Updated European Adventure", updatedItinerary.get().getTitle());
        assertEquals(200000, updatedItinerary.get().getBudget());
    }

    @Test
    @Order(4)
    void testCompleteWorkflow_RateItinerary() throws Exception {
        // First create an itinerary
        UUID itineraryId = createTestItinerary();
        
        // Step 4: Rate the itinerary
        mockMvc.perform(post(BASE_URL + "/" + itineraryId + "/rate")
                .header("X-User-Id", TEST_USER_ID)
                .param("rating", "5"))
                .andExpect(status().isOk());
        
        // Verify rating was recorded (this would typically update user interaction data)
        // The actual verification depends on your rating storage implementation
    }

    @Test
    @Order(5)
    void testCompleteWorkflow_DeleteItinerary() throws Exception {
        // First create an itinerary
        UUID itineraryId = createTestItinerary();
        
        // Verify itinerary exists
        assertTrue(itineraryRepository.findById(itineraryId).isPresent());
        
        // Step 5: Delete the itinerary
        mockMvc.perform(delete(BASE_URL + "/" + itineraryId)
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isNoContent());
        
        // Verify itinerary was deleted
        assertFalse(itineraryRepository.findById(itineraryId).isPresent());
    }

    @Test
    @Order(6)
    void testCompleteWorkflow_AIIntegration() throws Exception {
        // Test AI destination suggestions
        mockMvc.perform(get(BASE_URL + "/ai/destinations")
                .header("X-User-Id", TEST_USER_ID)
                .param("preferences", "adventure")
                .param("budget", "1000"))
                .andExpect(status().isOk());
        
        // Test AI travel tips
        mockMvc.perform(get(BASE_URL + "/ai/travel-tips")
                .header("X-User-Id", TEST_USER_ID)
                .param("destination", "Rome")
                .param("travelStyle", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    void testCompleteWorkflow_ErrorHandling() throws Exception {
        // Test invalid itinerary ID
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(get(BASE_URL + "/" + nonExistentId)
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isNotFound());
        
        // Test unauthorized access - delete with different user should return 404 (not found for that user)
        UUID itineraryId = createTestItinerary();
        
        mockMvc.perform(delete(BASE_URL + "/" + itineraryId)
                .header("X-User-Id", "different-user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void testCompleteWorkflow_ValidationErrors() throws Exception {
        // Test invalid trip plan request
        TripPlanRequest invalidRequest = new TripPlanRequest();
        // Missing required fields
        
        mockMvc.perform(post(BASE_URL + "/planTrip")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    private TripPlanRequest createTripPlanRequest() {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("European Adventure");
        
        // Set origin
        Map<String, Object> origin = Map.of("name", "London", "country", "UK");
        request.setOrigin(origin);
        
        // Set destinations
        List<Map<String, Object>> destinations = Arrays.asList(
                Map.of("name", "Paris", "country", "France"),
                Map.of("name", "Rome", "country", "Italy")
        );
        request.setDestinations(destinations);
        
        // Set other fields
        request.setDays(7);
        request.setBudget(150000);
        request.setInterests(Arrays.asList("culture", "food"));
        
        return request;
    }

    private UUID createTestItinerary() throws Exception {
        TripPlanRequest planRequest = createTripPlanRequest();
        
        MvcResult result = mockMvc.perform(post(BASE_URL + "/planTrip")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ItineraryResponse response = objectMapper.readValue(responseContent, ItineraryResponse.class);
        return response.getId();
    }
}