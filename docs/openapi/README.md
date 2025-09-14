# HopNGo API Documentation

This directory contains OpenAPI specifications for all HopNGo microservices.

## Services

### auth-service
- **Description**: Authentication and user management
- **OpenAPI Spec**: [auth-service.json](./auth-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8081/swagger-ui/index.html)
- **Service URL**: http://localhost:8081

### social-service
- **Description**: Social media and posts
- **OpenAPI Spec**: [social-service.json](./social-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8082/swagger-ui/index.html)
- **Service URL**: http://localhost:8082

### booking-service
- **Description**: Booking and vendor management
- **OpenAPI Spec**: [booking-service.json](./booking-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8083/swagger-ui/index.html)
- **Service URL**: http://localhost:8083

### chat-service
- **Description**: Real-time chat and messaging
- **OpenAPI Spec**: [chat-service.json](./chat-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8085/swagger-ui/index.html)
- **Service URL**: http://localhost:8085

### market-service
- **Description**: Marketplace and vendor listings
- **OpenAPI Spec**: [market-service.json](./market-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8086/swagger-ui/index.html)
- **Service URL**: http://localhost:8086

### search-service
- **Description**: Search and discovery functionality
- **OpenAPI Spec**: [search-service.json](./search-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8087/swagger-ui/index.html)
- **Service URL**: http://localhost:8087

### ai-service
- **Description**: AI-powered recommendations and insights
- **OpenAPI Spec**: [ai-service.json](./ai-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8088/swagger-ui/index.html)
- **Service URL**: http://localhost:8088

### trip-planning-service
- **Description**: Trip planning and itinerary management
- **OpenAPI Spec**: [trip-planning-service.json](./trip-planning-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8089/swagger-ui/index.html)
- **Service URL**: http://localhost:8089

### admin-service
- **Description**: Administrative operations and management
- **OpenAPI Spec**: [admin-service.json](./admin-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8090/swagger-ui/index.html)
- **Service URL**: http://localhost:8090

### analytics-service
- **Description**: Analytics and reporting
- **OpenAPI Spec**: [analytics-service.json](./analytics-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8091/swagger-ui/index.html)
- **Service URL**: http://localhost:8091

### emergency-service
- **Description**: Emergency assistance and safety features
- **OpenAPI Spec**: [emergency-service.json](./emergency-service.json)
- **Swagger UI**: [View API Docs](http://localhost:8092/swagger-ui/index.html)
- **Service URL**: http://localhost:8092


## Usage

### Local Development
1. Start all services using Docker Compose
2. Run the aggregator: `npm run start` from `tools/openapi-aggregator`
3. View individual service docs at their respective Swagger UI endpoints

### Generated Files
- Each service has its own `.json` file with the complete OpenAPI specification
- Use these files for SDK generation, testing, or documentation

### SDK Generation
Generate TypeScript SDKs using:
```bash
npm run sdk:generate
```

Last updated: 2025-09-14T15:13:35.159Z
