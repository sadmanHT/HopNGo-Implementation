package com.hopngo.tripplanning.service;

import com.hopngo.tripplanning.dto.CreateItineraryRequest;
import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.entity.Itinerary;
import com.hopngo.tripplanning.mapper.ItineraryMapper;
import com.hopngo.tripplanning.repository.ItineraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItineraryServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private ItineraryMapper itineraryMapper;

    @Mock
    private AIService aiService;

    @InjectMocks
    private ItineraryService itineraryService;

    private CreateItineraryRequest validRequest;
    private Itinerary mockItinerary;
    private ItineraryResponse mockResponse;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID format
        
        validRequest = new CreateItineraryRequest();
        validRequest.setTitle("Test Trip");
        validRequest.setDays(7);
        validRequest.setBudget(150000);
        validRequest.setOrigins(List.of(Map.of("city", "Tokyo", "country", "Japan")));
        validRequest.setDestinations(List.of(Map.of("city", "Kyoto", "country", "Japan")));
        validRequest.setPlan(null);

        mockItinerary = new Itinerary();
        mockItinerary.setId(UUID.randomUUID());
        mockItinerary.setUserId(userId);
        mockItinerary.setTitle("Test Trip");
        mockItinerary.setDays(7);
        mockItinerary.setBudget(200000);

        mockResponse = new ItineraryResponse();
        mockResponse.setId(mockItinerary.getId());
        mockResponse.setUserId(userId);
        mockResponse.setTitle("Test Trip");
        mockResponse.setDays(7);
        mockResponse.setBudget(200000);
    }

    @Test
    void testCreateTripPlan_Success() {
        // Arrange
        Map<String, Object> mockPlan = Map.of("day1", "Visit Tokyo Tower");
        when(itineraryMapper.toEntity(eq(validRequest), any(UUID.class))).thenReturn(mockItinerary);
        when(aiService.generateItinerary(any())).thenReturn(mockPlan);
        when(itineraryRepository.save(any(Itinerary.class))).thenReturn(mockItinerary);
        when(itineraryMapper.toResponse(mockItinerary)).thenReturn(mockResponse);

        // Act
        ItineraryResponse result = itineraryService.createTripPlan(validRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(mockResponse.getId(), result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals("Test Trip", result.getTitle());
        
        verify(itineraryMapper).toEntity(eq(validRequest), any(UUID.class));
        verify(aiService).generateItinerary(any());
        verify(itineraryRepository).save(any(Itinerary.class));
        verify(itineraryMapper).toResponse(mockItinerary);
    }

    @Test
    void testCreateTripPlan_InvalidBudget() {
        // Arrange
        validRequest.setBudget(0); // Invalid budget

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> itineraryService.createTripPlan(validRequest, userId)
        );
        
        assertEquals("Budget must be greater than 0", exception.getMessage());
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void testCreateTripPlan_InvalidDays() {
        // Arrange
        validRequest.setDays(0); // Invalid days

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> itineraryService.createTripPlan(validRequest, userId)
        );
        
        assertEquals("Days must be between 1 and 30", exception.getMessage());
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void testGetItinerary_Success() {
        // Arrange
        UUID itineraryId = mockItinerary.getId();
        when(itineraryRepository.findByIdAndUserId(itineraryId, userId))
                .thenReturn(Optional.of(mockItinerary));
        when(itineraryMapper.toResponse(mockItinerary)).thenReturn(mockResponse);

        // Act
        Optional<ItineraryResponse> result = itineraryService.getItinerary(itineraryId, userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(mockResponse.getId(), result.get().getId());
        assertEquals(userId, result.get().getUserId());
        
        verify(itineraryRepository).findByIdAndUserId(itineraryId, userId);
        verify(itineraryMapper).toResponse(mockItinerary);
    }

    @Test
    void testGetItinerary_NotFound() {
        // Arrange
        UUID itineraryId = UUID.randomUUID();
        when(itineraryRepository.findByIdAndUserId(itineraryId, userId))
                .thenReturn(Optional.empty());

        // Act
        Optional<ItineraryResponse> result = itineraryService.getItinerary(itineraryId, userId);

        // Assert
        assertTrue(result.isEmpty());
        verify(itineraryRepository).findByIdAndUserId(itineraryId, userId);
        verify(itineraryMapper, never()).toResponse(any());
    }

    @Test
    void testDeleteItinerary_Success() {
        // Arrange
        UUID itineraryId = mockItinerary.getId();
        when(itineraryRepository.findByIdAndUserId(itineraryId, userId))
                .thenReturn(Optional.of(mockItinerary));

        // Act
        boolean result = itineraryService.deleteItinerary(itineraryId, userId);

        // Assert
        assertTrue(result);
        verify(itineraryRepository).findByIdAndUserId(itineraryId, userId);
        verify(itineraryRepository).delete(mockItinerary);
    }

    @Test
    void testDeleteItinerary_NotFound() {
        // Arrange
        UUID itineraryId = UUID.randomUUID();
        when(itineraryRepository.findByIdAndUserId(itineraryId, userId))
                .thenReturn(Optional.empty());

        // Act
        boolean result = itineraryService.deleteItinerary(itineraryId, userId);

        // Assert
        assertFalse(result);
        verify(itineraryRepository).findByIdAndUserId(itineraryId, userId);
        verify(itineraryRepository, never()).delete(any());
    }

    @Test
    void testGetUserItineraryCount() {
        // Arrange
        long expectedCount = 5L;
        when(itineraryRepository.countByUserId(userId)).thenReturn(expectedCount);

        // Act
        long result = itineraryService.getUserItineraryCount(userId);

        // Assert
        assertEquals(expectedCount, result);
        verify(itineraryRepository).countByUserId(userId);
    }
}