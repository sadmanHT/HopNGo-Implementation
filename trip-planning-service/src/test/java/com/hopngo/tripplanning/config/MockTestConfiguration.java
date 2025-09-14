package com.hopngo.tripplanning.config;

import com.hopngo.tripplanning.service.AIServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test configuration for mocking external dependencies
 */
@TestConfiguration
@Profile("test")
public class MockTestConfiguration {

    @Bean
    @Primary
    public AIServiceClient mockAIServiceClient() {
        AIServiceClient mockClient = Mockito.mock(AIServiceClient.class);
        
        // Mock destination suggestions
        when(mockClient.getNextDestinationSuggestion(
                anyString(), anyList(), anyString(), anyInt(), anyInt()))
                .thenReturn("Based on your travel to Paris, I recommend visiting Rome for its rich history and amazing cuisine!");
        
        // Mock travel tips
        when(mockClient.getTravelTips(anyString(), anyString(), anyInt()))
                .thenReturn("Here are some great tips for Rome: Visit the Colosseum early morning, try authentic pasta in Trastevere, and don't forget to throw a coin in Trevi Fountain!");
        
        // Mock service availability
        when(mockClient.isAIServiceAvailable())
                .thenReturn(true);
        
        // Remove this line as getFallbackDestinationSuggestion is private
        
        return mockClient;
    }
}