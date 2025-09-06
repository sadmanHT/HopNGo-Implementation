package com.hopngo.chatservice.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketConfig {
    
    // Removed @Value injection to fix BeanCreationException
    // private List<String> allowedOrigins;
    
    // Note: WebSocket configuration will be implemented once Spring WebSocket dependencies are properly resolved
    // For now, this is a placeholder to allow compilation
    
    public void configureMessageBroker() {
        // Enable a simple memory-based message broker to carry messages back to the client
        // on destinations prefixed with "/topic" and "/queue"
        // config.enableSimpleBroker("/topic", "/queue");
        
        // Designate the "/app" prefix for messages that are bound to methods
        // annotated with @MessageMapping
        // config.setApplicationDestinationPrefixes("/app");
        
        // Optional: Set user destination prefix
        // config.setUserDestinationPrefix("/user");
    }
    
    public void registerStompEndpoints() {
        // registry.addEndpoint("/ws/chat")
        //         .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
        //         .addInterceptors(jwtHandshakeInterceptor)
        //         .withSockJS();
    }
}