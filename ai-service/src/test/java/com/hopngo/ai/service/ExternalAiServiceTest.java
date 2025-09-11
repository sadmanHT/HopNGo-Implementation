package com.hopngo.ai.service;

import com.hopngo.ai.dto.ChatbotRequest;
import com.hopngo.ai.dto.ChatbotResponse;
import com.hopngo.ai.dto.ImageSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalAiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExternalAiService externalAiService;

    private MockMultipartFile mockImageFile;
    private ChatbotRequest chatbotRequest;

    @BeforeEach
    void setUp() {
        // Set up test configuration values
        ReflectionTestUtils.setField(externalAiService, "openaiApiKey", "test-api-key");
        ReflectionTestUtils.setField(externalAiService, "googleVisionApiKey", "test-vision-key");
        ReflectionTestUtils.setField(externalAiService, "openaiModel", "gpt-3.5-turbo");
        ReflectionTestUtils.setField(externalAiService, "maxTokens", 1000);
        ReflectionTestUtils.setField(externalAiService, "temperature", 0.7);
        ReflectionTestUtils.setField(externalAiService, "requestTimeout", 30000);
        ReflectionTestUtils.setField(externalAiService, "maxConcurrentRequests", 50);
        ReflectionTestUtils.setField(externalAiService, "executorService", Executors.newFixedThreadPool(2));

        // Create mock image file
        mockImageFile = new MockMultipartFile(
            "image", 
            "test-image.jpg", 
            "image/jpeg", 
            "fake image content".getBytes()
        );

        // Create mock chatbot request
        chatbotRequest = new ChatbotRequest();
        chatbotRequest.setMessage("What are the best restaurants in Paris?");
        chatbotRequest.setLocation("Paris, France");
        chatbotRequest.setConversationId("test-conversation-123");
    }

    @Test
    void testSearchByImage_Success() throws Exception {
        // Arrange
        String mockApiResponse = "{\"choices\":[{\"message\":{\"content\":\"Eiffel Tower, Paris\"}}]}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockApiResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(mockResponse);
        when(objectMapper.readValue(anyString(), eq(Object.class)))
            .thenReturn(createMockOpenAIResponse());

        // Act
        CompletableFuture<ImageSearchResponse> future = externalAiService.searchByImage(mockImageFile, "landmark", "user123");
        ImageSearchResponse result = future.get();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResults());
        assertFalse(result.getResults().isEmpty());
        assertTrue(result.getProcessingTime().endsWith("ms"));
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testSearchByImage_ApiError() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid request"));

        // Act & Assert
        CompletableFuture<ImageSearchResponse> future = externalAiService.searchByImage(mockImageFile, "landmark", "user123");
        
        assertThrows(ExecutionException.class, () -> future.get());
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testGetChatbotResponse_Success() throws Exception {
        // Arrange
        String mockApiResponse = "{\"choices\":[{\"message\":{\"content\":\"Here are some great restaurants in Paris: Le Comptoir du Relais, L'As du Fallafel, Breizh Café.\"}}]}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockApiResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(mockResponse);
        when(objectMapper.readValue(anyString(), eq(Object.class)))
            .thenReturn(createMockOpenAIResponse());

        // Act
        CompletableFuture<ChatbotResponse> future = externalAiService.getChatbotResponse(chatbotRequest, "user123");
        ChatbotResponse result = future.get();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertNotNull(result.getConversationId());
        assertTrue(result.getConfidence() > 0);
        assertFalse(result.getSuggestions().isEmpty());
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testGetChatbotResponse_WithItinerary() throws Exception {
        // Arrange
        chatbotRequest.setCurrentItinerary("Louvre Museum, Eiffel Tower, Notre-Dame");
        
        String mockApiResponse = "{\"choices\":[{\"message\":{\"content\":\"Based on your itinerary, I recommend restaurants near these attractions.\"}}]}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockApiResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(mockResponse);
        when(objectMapper.readValue(anyString(), eq(Object.class)))
            .thenReturn(createMockOpenAIResponse());

        // Act
        CompletableFuture<ChatbotResponse> future = externalAiService.getChatbotResponse(chatbotRequest, "user123");
        ChatbotResponse result = future.get();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testIsFAQQuestion() {
        // Test FAQ questions
        assertTrue(externalAiService.isFAQQuestion("What is the best time to visit Paris?"));
        assertTrue(externalAiService.isFAQQuestion("How to get from airport to city center?"));
        assertTrue(externalAiService.isFAQQuestion("Where is the nearest metro station?"));
        assertTrue(externalAiService.isFAQQuestion("When is the museum open?"));
        
        // Test non-FAQ questions
        assertFalse(externalAiService.isFAQQuestion("I love this place!"));
        assertFalse(externalAiService.isFAQQuestion("Book me a hotel"));
        assertFalse(externalAiService.isFAQQuestion("Thank you for the help"));
    }

    @Test
    void testClearSessionData() {
        // Act
        assertDoesNotThrow(() -> externalAiService.clearSessionData("user123"));
        
        // This test ensures the method doesn't throw exceptions
        // In a real implementation, we would verify that session data is actually cleared
    }

    @Test
    void testChatbotResponse_ApiTimeout() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Timeout"));

        // Act & Assert
        CompletableFuture<ChatbotResponse> future = externalAiService.getChatbotResponse(chatbotRequest, "user123");
        
        assertThrows(ExecutionException.class, () -> future.get());
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testImageSearch_InvalidImageFormat() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
            "image", 
            "test.txt", 
            "text/plain", 
            "not an image".getBytes()
        );

        // Act & Assert
        CompletableFuture<ImageSearchResponse> future = externalAiService.searchByImage(invalidFile, "landmark", "user123");
        
        assertThrows(ExecutionException.class, () -> future.get());
    }

    private Object createMockOpenAIResponse() {
        // Create a mock OpenAI API response structure
        return new Object() {
            public Object[] choices = new Object[] {
                new Object() {
                    public Object message = new Object() {
                        public String content = "Mock AI response";
                    };
                }
            };
        };
    }
}