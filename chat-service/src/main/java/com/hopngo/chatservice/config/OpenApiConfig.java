package com.hopngo.chatservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI chatServiceOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8085");
        devServer.setDescription("Server URL in Development environment");
        
        Server prodServer = new Server();
        prodServer.setUrl("https://api.hopngo.com");
        prodServer.setDescription("Server URL in Production environment");
        
        Contact contact = new Contact();
        contact.setEmail("support@hopngo.com");
        contact.setName("HopNGo Support");
        contact.setUrl("https://www.hopngo.com");
        
        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");
        
        Info info = new Info()
                .title("Chat Service API")
                .version("1.0")
                .contact(contact)
                .description("This API provides real-time chat functionality for the HopNGo platform. It supports both WebSocket STOMP messaging and REST endpoints for conversation management and message history.")
                .termsOfService("https://www.hopngo.com/terms")
                .license(mitLicense);
        
        return new OpenAPI().info(info).servers(List.of(devServer, prodServer));
    }
}