package com.hopngo.admin.service;

import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(integrationService, "authServiceUrl", "http://auth-service");
        ReflectionTestUtils.setField(integrationService, "socialServiceUrl", "http://social-service");
        ReflectionTestUtils.setField(integrationService, "bookingServiceUrl", "http://booking-service");
        ReflectionTestUtils.setField(integrationService, "marketServiceUrl", "http://market-service");
    }

    @Test
    void removeSocialPost_ShouldCallCorrectEndpoint() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        integrationService.removeSocialPost(Long.valueOf("post123"), "Inappropriate content");

        // Then
        verify(restTemplate).exchange(
            eq("http://social-service/internal/posts/post123"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void removeSocialPost_WhenServiceReturnsError_ShouldThrowException() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> integrationService.removeSocialPost(Long.valueOf("post123"), "reason"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to remove social post");
    }

    @Test
    void removeSocialPost_WhenRestTemplateThrowsException_ShouldThrowRuntimeException() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // When & Then
        assertThatThrownBy(() -> integrationService.removeSocialPost(Long.valueOf("post123"), "reason"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Social post removal failed");
    }

    @Test
    void removeSocialComment_ShouldCallCorrectEndpoint() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        integrationService.removeSocialComment(Long.valueOf("comment123"), "Spam content");

        // Then
        verify(restTemplate).exchange(
            eq("http://social-service/internal/comments/comment123"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void removeMarketListing_ShouldCallCorrectEndpoint() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        integrationService.removeMarketListing(Long.valueOf("listing123"), "Fraudulent listing");

        // Then
        verify(restTemplate).exchange(
            eq("http://market-service/internal/listings/listing123"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void removeBookingTrip_ShouldCallCorrectEndpoint() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        integrationService.removeBookingTrip(Long.valueOf("trip123"), "Unsafe trip");

        // Then
        verify(restTemplate).exchange(
            eq("http://booking-service/internal/trips/trip123"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void banUser_ShouldCallCorrectEndpointAndReturnTrue() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        boolean result = integrationService.banUser("user123", "Repeated violations");

        // Then
        assertThat(result).isTrue();
        verify(restTemplate).exchange(
            eq("http://auth-service/internal/users/user123/ban"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void banUser_WhenServiceReturnsError_ShouldReturnFalse() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        boolean result = integrationService.banUser("user123", "reason");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void banUser_WhenRestTemplateThrowsException_ShouldReturnFalse() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // When
        boolean result = integrationService.banUser("user123", "reason");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void removeContent_ShouldCallCorrectServiceBasedOnType() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        boolean result = integrationService.removeContent(ModerationItemType.POST, Long.valueOf("post123"));

        // Then
        assertThat(result).isTrue();
        verify(restTemplate).exchange(
            eq("http://social-service/internal/posts/post123"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void removeContent_WithListingType_ShouldCallMarketService() {
        // Given
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(response);

        // When
        boolean result = integrationService.removeContent(ModerationItemType.LISTING, Long.valueOf("listing123"));

        // Then
        assertThat(result).isTrue();
        verify(restTemplate).exchange(
            eq("http://market-service/internal/listings/listing123"),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }
}