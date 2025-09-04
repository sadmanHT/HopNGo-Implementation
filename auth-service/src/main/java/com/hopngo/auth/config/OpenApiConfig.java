package com.hopngo.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for Auth Service
 * Configures Swagger UI with JWT Bearer token authentication
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "HopNGo Auth Service API",
        version = "1.0.0",
        description = "Authentication and user management service for HopNGo application. " +
                     "Provides JWT-based authentication with refresh token support.",
        contact = @Contact(
            name = "HopNGo Development Team",
            email = "dev@hopngo.com",
            url = "https://hopngo.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8081",
            description = "Local Development Server"
        ),
        @Server(
            url = "http://localhost:8080/auth",
            description = "Local Development Server via Gateway"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token authentication. " +
                 "Obtain a token by calling the /auth/login endpoint, " +
                 "then include it in the Authorization header as 'Bearer {token}'."
)
public class OpenApiConfig {
    // Configuration is handled by annotations
}