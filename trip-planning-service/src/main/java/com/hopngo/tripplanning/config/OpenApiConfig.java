package com.hopngo.tripplanning.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8087}")
    private String serverPort;

    @Bean
    public OpenAPI tripPlanningOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HopNGo Trip Planning Service API")
                        .description("REST API for managing travel itineraries and trip planning")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("HopNGo Development Team")
                                .email("dev@hopngo.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Gateway server")
                ))
                .components(new Components()
                        .addSecuritySchemes("X-User-Id", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("User ID header required for authentication")));
    }
}