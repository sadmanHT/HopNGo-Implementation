# HopNGo Gateway

Spring Cloud Gateway service that acts as the API gateway for all HopNGo microservices.

## Configuration

- **Server Port**: 8080
- **CORS**: Configured to allow requests from `http://localhost:3000`
- **JWT Filter**: Placeholder implementation that logs and forwards Authorization Bearer tokens

## Routes

The gateway routes requests to the following services:

| Path | Target Service | Port |
|------|----------------|------|
| `/api/v1/auth/**` | auth-service | 8081 |
| `/social/**` | social-service | 8082 |
| `/bookings/**` | booking-service | 8083 |
| `/market/**` | market-service | 8084 |
| `/chat/**` | chat-service | 8085 |
| `/notify/**` | notification-service | 8086 |
| `/trips/**` | trip-planning-service | 8087 |
| `/ai/**` | ai-service | 8088 |
| `/emergency/**` | emergency-service | 8089 |

## Health Endpoints

- **Health Check**: `http://localhost:8080/actuator/health`
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`

## How to Run

### Prerequisites
- Java 17 or higher
- Maven 3.6+ (or use the included Maven wrapper)

### Running the Application

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or using system Maven
mvn spring-boot:run
```

### Building the Application

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Package
./mvnw package
```

### Docker

```bash
# Build Docker image
docker build -t hopngo-gateway .

# Run Docker container
docker run -p 8080:8080 hopngo-gateway
```

## Dependencies

- Spring Cloud Gateway
- Spring Boot Actuator
- Spring Boot Validation
- Micrometer Prometheus Registry
- Spring Data Redis (for future rate limiting)
- Spring Boot Logging

## Notes

- The JWT filter is currently a placeholder that only logs tokens
- Redis configuration is included for future rate limiting implementation
- All routes include the JWT authentication filter
- CORS is configured globally for the frontend application