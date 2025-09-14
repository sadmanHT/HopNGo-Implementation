package com.hopngo.tripplanning.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.tripplanning.dto.TripPlanRequest;
import com.hopngo.tripplanning.dto.ItineraryUpdateRequest;
import com.hopngo.tripplanning.dto.ItineraryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
public class TripPlanningIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("hopngo_trip_planning_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID format
    private static final String USER_ID_HEADER = "X-User-Id";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    private TripPlanRequest createValidTripPlanRequest() {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle("Amazing Tokyo Adventure");
        request.setDays(7);
        request.setBudget(200000); // $2000.00 in cents
        request.setOrigin(Map.of(
                "city", "New York",
                "country", "USA",
                "coordinates", Map.of("lat", 40.7128, "lng", -74.0060)
        ));
        request.setDestinations(List.of(
                Map.of(
                        "city", "Tokyo",
                        "country", "Japan",
                        "coordinates", Map.of("lat", 35.6762, "lng", 139.6503)
                ),
                Map.of(
                        "city", "Kyoto",
                        "country", "Japan",
                        "coordinates", Map.of("lat", 35.0116, "lng", 135.7681)
                )
        ));
        return request;
    }

    @Test
    public void testCreateTripPlan_Success() throws Exception {
        TripPlanRequest request = createValidTripPlanRequest();

        mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Amazing Tokyo Adventure"))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.budget").value(200000))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.origins").isArray())
                .andExpect(jsonPath("$.destinations").isArray())
                .andExpect(jsonPath("$.plan").exists());
    }

    @Test
    public void testCreateTripPlan_MissingUserIdHeader() throws Exception {
        TripPlanRequest request = createValidTripPlanRequest();

        mockMvc.perform(post("/api/v1/trips/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_USER_ID"));
    }

    @Test
    public void testCreateTripPlan_InvalidRequest() throws Exception {
        TripPlanRequest request = new TripPlanRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetItinerary_Success() throws Exception {
        // First create an itinerary
        TripPlanRequest request = createValidTripPlanRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        ItineraryResponse createdItinerary = objectMapper.readValue(responseContent, ItineraryResponse.class);
        UUID itineraryId = createdItinerary.getId();

        // Then retrieve it
        mockMvc.perform(get("/api/v1/trips/{id}", itineraryId)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itineraryId.toString()))
                .andExpect(jsonPath("$.title").value("Amazing Tokyo Adventure"))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    public void testGetItinerary_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/{id}", nonExistentId)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetItinerary_UnauthorizedAccess() throws Exception {
        // Create itinerary with one user
        TripPlanRequest request = createValidTripPlanRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        ItineraryResponse createdItinerary = objectMapper.readValue(responseContent, ItineraryResponse.class);
        UUID itineraryId = createdItinerary.getId();

        // Try to access with different user
        mockMvc.perform(get("/api/v1/trips/{id}", itineraryId)
                        .header(USER_ID_HEADER, "different-user-456"))
                .andExpect(status().isNotFound()); // Should return 404 for security
    }

    @Test
    public void testUpdateItinerary_Success() throws Exception {
        // First create an itinerary
        TripPlanRequest request = createValidTripPlanRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        ItineraryResponse createdItinerary = objectMapper.readValue(responseContent, ItineraryResponse.class);
        UUID itineraryId = createdItinerary.getId();

        // Update the itinerary
        ItineraryUpdateRequest updateRequest = new ItineraryUpdateRequest();
        updateRequest.setTitle("Updated Tokyo Adventure");
        updateRequest.setBudget(250000); // $2500.00 in cents

        mockMvc.perform(patch("/api/v1/trips/{id}", itineraryId)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itineraryId.toString()))
                .andExpect(jsonPath("$.title").value("Updated Tokyo Adventure"))
                .andExpect(jsonPath("$.budget").value(250000))
                .andExpect(jsonPath("$.days").value(7)); // Should remain unchanged
    }

    @Test
    public void testDeleteItinerary_Success() throws Exception {
        // First create an itinerary
        TripPlanRequest request = createValidTripPlanRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        ItineraryResponse createdItinerary = objectMapper.readValue(responseContent, ItineraryResponse.class);
        UUID itineraryId = createdItinerary.getId();

        // Delete the itinerary
        mockMvc.perform(delete("/api/v1/trips/{id}", itineraryId)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/v1/trips/{id}", itineraryId)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetUserItineraries_Success() throws Exception {
        // Create multiple itineraries
        for (int i = 1; i <= 3; i++) {
            TripPlanRequest request = createValidTripPlanRequest();
            request.setTitle("Trip " + i);
            
            mockMvc.perform(post("/api/v1/trips/plan")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get user itineraries
        mockMvc.perform(get("/api/v1/trips")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    public void testSearchItineraries_Success() throws Exception {
        // Create itineraries with different titles and budgets
        TripPlanRequest request1 = createValidTripPlanRequest();
        request1.setTitle("Tokyo Adventure");
        request1.setBudget(150000); // $1500.00 in cents
        
        TripPlanRequest request2 = createValidTripPlanRequest();
        request2.setTitle("Kyoto Journey");
        request2.setBudget(250000); // $2500.00 in cents

        mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/trips/plan")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Search by title
        mockMvc.perform(get("/api/v1/trips/search")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("title", "Tokyo")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Tokyo Adventure"));

        // Search by budget range
        mockMvc.perform(get("/api/v1/trips/search")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("minBudget", "2000")
                        .param("maxBudget", "3000")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Kyoto Journey"));
    }

    @Test
    public void testGetUserItineraryCount_Success() throws Exception {
        // Create some itineraries
        for (int i = 1; i <= 2; i++) {
            TripPlanRequest request = createValidTripPlanRequest();
            request.setTitle("Trip " + i);
            
            mockMvc.perform(post("/api/v1/trips/plan")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get count
        mockMvc.perform(get("/api/v1/trips/count")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }
}