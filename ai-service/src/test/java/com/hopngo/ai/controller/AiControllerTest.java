package com.hopngo.ai.controller;

import com.hopngo.ai.dto.*;
import com.hopngo.ai.service.ExternalAiService;
import com.hopngo.ai.service.ModerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalAiService externalAiService;

    @MockBean
    private ModerationService moderationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ImageSearchRequest imageSearchRequest;
    private ChatbotRequest chatbotRequest;
    private ImageSearchResponse imageSearchResponse;
    private ChatbotResponse chatbotResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        imageSearchRequest = new ImageSearchRequest();
        imageSearchRequest.setQuery("landmark");
        imageSearchRequest.setLimit(5);

        chatbotRequest = new ChatbotRequest();
        chatbotRequest.setMessage("What are the best restaurants in Paris?");
        chatbotRequest.setLocation("Paris, France");
        chatbotRequest.setConversationId("test-conversation-123");

        // Setup mock responses
        List<ImageSearchResponse.SearchResult> searchResults = Arrays.asList(
            new ImageSearchResponse.SearchResult(
                "place", "place_1", 0.95, "Eiffel Tower", 
                "Famous landmark in Paris", "https://example.com/eiffel.jpg", "Paris, France"
            ),
            new ImageSearchResponse.SearchResult(
                "place", "place_2", 0.87, "Louvre Museum", 
                "World famous art museum", "https://example.com/louvre.jpg", "Paris, France"
            )
        );
        imageSearchResponse = new ImageSearchResponse(searchResults, 2, "150ms");

        chatbotResponse = new ChatbotResponse();
        chatbotResponse.setResponse("Here are some great restaurants in Paris: Le Comptoir du Relais, L'As du Fallafel.");
        chatbotResponse.setConversationId("test-conversation-123");
        chatbotResponse.setConfidence(0.85);
        chatbotResponse.setSuggestions(Arrays.asList(
            "Tell me about local attractions",
            "What's the weather like?",
            "Recommend hotels nearby"
        ));
        chatbotResponse.setContext("Currently discussing Paris, France");
        chatbotResponse.setRequiresFollowUp(false);
    }

    @Test
    void testImageSearch_Success() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
            "imageFile", "test-image.jpg", "image/jpeg", "fake image content".getBytes()
        );
        
        when(externalAiService.searchByImage(any(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(imageSearchResponse));
        doNothing().when(externalAiService).clearSessionData(anyString());

        // Act & Assert
        mockMvc.perform(multipart("/ai/image-search")
                .file(imageFile)
                .param("query", "landmark")
                .param("limit", "5")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].title").value("Eiffel Tower"))
                .andExpect(jsonPath("$.totalResults").value(2))
                .andExpect(jsonPath("$.processingTime").value("150ms"));

        verify(externalAiService, times(1)).searchByImage(any(), eq("landmark"), eq("user123"));
        verify(externalAiService, times(1)).clearSessionData("user123");
    }

    @Test
    void testImageSearch_Timeout() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
            "imageFile", "test-image.jpg", "image/jpeg", "fake image content".getBytes()
        );
        
        CompletableFuture<ImageSearchResponse> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.completeExceptionally(new TimeoutException("Request timeout"));
        
        when(externalAiService.searchByImage(any(), anyString(), anyString()))
            .thenReturn(timeoutFuture);

        // Act & Assert
        mockMvc.perform(multipart("/ai/image-search")
                .file(imageFile)
                .param("query", "landmark")
                .param("limit", "5")
                .header("X-User-Id", "user123"))
                .andExpect(status().isRequestTimeout());

        verify(externalAiService, times(1)).searchByImage(any(), eq("landmark"), eq("user123"));
    }

    @Test
    void testImageSearch_InternalError() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
            "imageFile", "test-image.jpg", "image/jpeg", "fake image content".getBytes()
        );
        
        CompletableFuture<ImageSearchResponse> errorFuture = new CompletableFuture<>();
        errorFuture.completeExceptionally(new RuntimeException("API Error"));
        
        when(externalAiService.searchByImage(any(), anyString(), anyString()))
            .thenReturn(errorFuture);

        // Act & Assert
        mockMvc.perform(multipart("/ai/image-search")
                .file(imageFile)
                .param("query", "landmark")
                .param("limit", "5")
                .header("X-User-Id", "user123"))
                .andExpect(status().isInternalServerError());

        verify(externalAiService, times(1)).searchByImage(any(), eq("landmark"), eq("user123"));
    }

    @Test
    void testChatbot_Success() throws Exception {
        // Arrange
        when(externalAiService.getChatbotResponse(any(ChatbotRequest.class), anyString()))
            .thenReturn(CompletableFuture.completedFuture(chatbotResponse));
        doNothing().when(externalAiService).clearSessionData(anyString());

        // Act & Assert
        mockMvc.perform(post("/ai/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatbotRequest))
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Here are some great restaurants in Paris: Le Comptoir du Relais, L'As du Fallafel."))
                .andExpect(jsonPath("$.conversationId").value("test-conversation-123"))
                .andExpect(jsonPath("$.confidence").value(0.85))
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions.length()").value(3))
                .andExpect(jsonPath("$.context").value("Currently discussing Paris, France"))
                .andExpect(jsonPath("$.requiresFollowUp").value(false));

        verify(externalAiService, times(1)).getChatbotResponse(any(ChatbotRequest.class), eq("user123"));
        verify(externalAiService, times(1)).clearSessionData("user123");
    }

    @Test
    void testChatbot_Timeout() throws Exception {
        // Arrange
        CompletableFuture<ChatbotResponse> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.completeExceptionally(new TimeoutException("Request timeout"));
        
        when(externalAiService.getChatbotResponse(any(ChatbotRequest.class), anyString()))
            .thenReturn(timeoutFuture);

        // Act & Assert
        mockMvc.perform(post("/ai/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatbotRequest))
                .header("X-User-Id", "user123"))
                .andExpect(status().isRequestTimeout());

        verify(externalAiService, times(1)).getChatbotResponse(any(ChatbotRequest.class), eq("user123"));
    }

    @Test
    void testChatbot_BadRequest() throws Exception {
        // Arrange
        CompletableFuture<ChatbotResponse> errorFuture = new CompletableFuture<>();
        errorFuture.completeExceptionally(new IllegalArgumentException("Invalid request"));
        
        when(externalAiService.getChatbotResponse(any(ChatbotRequest.class), anyString()))
            .thenReturn(errorFuture);

        // Act & Assert
        mockMvc.perform(post("/ai/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatbotRequest))
                .header("X-User-Id", "user123"))
                .andExpect(status().isBadRequest());

        verify(externalAiService, times(1)).getChatbotResponse(any(ChatbotRequest.class), eq("user123"));
    }

    @Test
    void testChatbot_MissingUserIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/ai/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatbotRequest)))
                .andExpect(status().isBadRequest());

        verify(externalAiService, never()).getChatbotResponse(any(), any());
    }

    @Test
    void testImageSearch_MissingUserIdHeader() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
            "imageFile", "test-image.jpg", "image/jpeg", "fake image content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/ai/image-search")
                .file(imageFile)
                .param("query", "landmark")
                .param("limit", "5"))
                .andExpect(status().isBadRequest());

        verify(externalAiService, never()).searchByImage(any(), any(), any());
    }

    @Test
    void testChatbot_InvalidJsonRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/ai/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}")
                .header("X-User-Id", "user123"))
                .andExpect(status().isBadRequest());

        verify(externalAiService, never()).getChatbotResponse(any(), any());
    }

    @Test
    void testChatbot_WithItinerary() throws Exception {
        // Arrange
        chatbotRequest.setCurrentItinerary("Louvre Museum, Eiffel Tower, Notre-Dame");
        
        when(externalAiService.getChatbotResponse(any(ChatbotRequest.class), anyString()))
            .thenReturn(CompletableFuture.completedFuture(chatbotResponse));
        doNothing().when(externalAiService).clearSessionData(anyString());

        // Act & Assert
        mockMvc.perform(post("/ai/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatbotRequest))
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.conversationId").exists());

        verify(externalAiService, times(1)).getChatbotResponse(any(ChatbotRequest.class), eq("user123"));
        verify(externalAiService, times(1)).clearSessionData("user123");
    }
}