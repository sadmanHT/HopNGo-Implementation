---
sidebar_position: 7
---

# API Reference

This document provides comprehensive API reference documentation for all HopNGo services. Each service exposes OpenAPI 3.0 compliant REST APIs with interactive documentation.

## Overview

HopNGo's microservices architecture consists of multiple specialized services, each with its own API. All APIs are accessible through the API Gateway at `https://api.hopngo.com` in production or `http://localhost:8080` in development.

### Authentication

All APIs (except authentication endpoints) require JWT Bearer token authentication:

```http
Authorization: Bearer <your-jwt-token>
```

Obtain tokens through the Auth Service `/api/v1/auth/login` endpoint.

### Base URLs

| Environment | Base URL |
|-------------|----------|
| **Production** | `https://api.hopngo.com` |
| **Development** | `http://localhost:8080` |

### Response Format

All APIs return JSON responses with consistent error handling:

```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

Error responses:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": []
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Service APIs

### Auth Service API

**Base Path:** `/api/v1/auth`  
**Port:** 8081 (direct) | 8080 (via gateway)  
**Description:** Authentication and user management service with JWT-based authentication and refresh token support.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/login` | User authentication |
| `POST` | `/register` | User registration |
| `POST` | `/refresh` | Refresh JWT token |
| `POST` | `/logout` | User logout |
| `GET` | `/profile` | Get user profile |
| `PUT` | `/profile` | Update user profile |
| `POST` | `/forgot-password` | Password reset request |
| `POST` | `/reset-password` | Reset password |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8081/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Auth Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8081/api-docs](http://localhost:8081/api-docs)

---

### Chat Service API

**Base Path:** `/chat`  
**Port:** 8085 (direct) | 8080 (via gateway)  
**Description:** Real-time chat functionality with WebSocket STOMP messaging and REST endpoints for conversation management.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/conversations` | List user conversations |
| `POST` | `/conversations` | Create new conversation |
| `GET` | `/conversations/{id}` | Get conversation details |
| `GET` | `/conversations/{id}/messages` | Get conversation messages |
| `POST` | `/conversations/{id}/messages` | Send message |
| `PUT` | `/messages/{id}` | Update message |
| `DELETE` | `/messages/{id}` | Delete message |

#### WebSocket Endpoints

| Destination | Description |
|-------------|-------------|
| `/app/chat.sendMessage` | Send message via WebSocket |
| `/app/chat.addUser` | Add user to conversation |
| `/topic/public` | Public chat messages |
| `/queue/reply` | Private message replies |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8085/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Chat Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8085/api-docs](http://localhost:8085/api-docs)

---

### Booking Service API

**Base Path:** `/bookings`  
**Port:** 8083 (direct) | 8080 (via gateway)  
**Description:** Booking and reservation management service for travel experiences and accommodations.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/bookings` | List user bookings |
| `POST` | `/bookings` | Create new booking |
| `GET` | `/bookings/{id}` | Get booking details |
| `PUT` | `/bookings/{id}` | Update booking |
| `DELETE` | `/bookings/{id}` | Cancel booking |
| `POST` | `/bookings/{id}/confirm` | Confirm booking |
| `POST` | `/bookings/{id}/payment` | Process payment |
| `GET` | `/bookings/{id}/status` | Get booking status |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8083/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Booking Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8083/api-docs](http://localhost:8083/api-docs)

---

### Market Service API

**Base Path:** `/market`  
**Port:** 8084 (direct) | 8080 (via gateway)  
**Description:** Marketplace service for travel listings, experiences, and provider management.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/listings` | Search and list travel offerings |
| `POST` | `/listings` | Create new listing |
| `GET` | `/listings/{id}` | Get listing details |
| `PUT` | `/listings/{id}` | Update listing |
| `DELETE` | `/listings/{id}` | Delete listing |
| `GET` | `/categories` | Get listing categories |
| `GET` | `/providers` | List service providers |
| `POST` | `/providers` | Register as provider |
| `GET` | `/reviews` | Get reviews and ratings |
| `POST` | `/reviews` | Submit review |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8084/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Market Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8084/api-docs](http://localhost:8084/api-docs)

---

### Search Service API

**Base Path:** `/search`  
**Port:** 8091 (direct) | 8080 (via gateway)  
**Description:** Advanced search and filtering service powered by OpenSearch for travel listings and experiences.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/search` | Search listings with filters |
| `GET` | `/search/suggestions` | Get search suggestions |
| `GET` | `/search/popular` | Get popular searches |
| `POST` | `/search/advanced` | Advanced search with complex filters |
| `GET` | `/search/nearby` | Location-based search |
| `GET` | `/search/categories/{category}` | Search within category |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8091/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Search Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8091/api-docs](http://localhost:8091/api-docs)

---

### AI Service API

**Base Path:** `/api/v1/ai`  
**Port:** 8088 (direct) | 8080 (via gateway)  
**Description:** AI-powered recommendations, trip planning, and intelligent assistance service.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/recommendations` | Get personalized recommendations |
| `POST` | `/trip-planning` | AI-powered trip planning |
| `POST` | `/chat` | AI assistant chat |
| `GET` | `/insights` | Travel insights and trends |
| `POST` | `/optimize-itinerary` | Optimize travel itinerary |
| `POST` | `/price-prediction` | Predict price trends |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8088/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="AI Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8088/api-docs](http://localhost:8088/api-docs)

---

### Trip Planning Service API

**Base Path:** `/trips`  
**Port:** 8087 (direct) | 8080 (via gateway)  
**Description:** Comprehensive trip planning and itinerary management service.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/trips` | List user trips |
| `POST` | `/trips` | Create new trip |
| `GET` | `/trips/{id}` | Get trip details |
| `PUT` | `/trips/{id}` | Update trip |
| `DELETE` | `/trips/{id}` | Delete trip |
| `POST` | `/trips/{id}/itinerary` | Add itinerary item |
| `GET` | `/trips/{id}/itinerary` | Get trip itinerary |
| `POST` | `/trips/{id}/share` | Share trip with others |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8087/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Trip Planning Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8087/api-docs](http://localhost:8087/api-docs)

---

### Admin Service API

**Base Path:** `/api/v1/admin`  
**Port:** 8090 (direct) | 8080 (via gateway)  
**Description:** Administrative functions and system management (requires admin role).

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/users` | List all users |
| `PUT` | `/users/{id}/status` | Update user status |
| `GET` | `/analytics` | System analytics |
| `GET` | `/reports` | Generate reports |
| `POST` | `/maintenance` | System maintenance tasks |
| `GET` | `/logs` | System logs |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8090/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Admin Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8090/api-docs](http://localhost:8090/api-docs)

---

### Analytics Service API

**Base Path:** `/api/v1/analytics`  
**Port:** 8093 (direct) | 8080 (via gateway)  
**Description:** Analytics and metrics collection service for business intelligence.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/events` | Track user events |
| `GET` | `/metrics` | Get system metrics |
| `GET` | `/reports/usage` | Usage reports |
| `GET` | `/reports/revenue` | Revenue reports |
| `GET` | `/dashboards` | Analytics dashboards |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8093/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Analytics Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8093/api-docs](http://localhost:8093/api-docs)

---

### Emergency Service API

**Base Path:** `/api/v1/emergency`  
**Port:** 8089 (direct) | 8080 (via gateway)  
**Description:** Emergency assistance and safety features for travelers.

#### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/alerts` | Create emergency alert |
| `GET` | `/contacts` | Emergency contacts |
| `POST` | `/assistance` | Request assistance |
| `GET` | `/safety-info` | Safety information |

#### Interactive Documentation

<div style={{border: '1px solid #e1e4e8', borderRadius: '6px', overflow: 'hidden'}}>
  <iframe 
    src="http://localhost:8089/swagger-ui.html" 
    width="100%" 
    height="600px" 
    frameBorder="0"
    title="Emergency Service API Documentation"
  />
</div>

**OpenAPI Spec:** [http://localhost:8089/api-docs](http://localhost:8089/api-docs)

---

## API Gateway Routes

All services are accessible through the API Gateway with the following routing configuration:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8081
          predicates:
            - Path=/api/v1/auth/**
        
        - id: booking-service
          uri: http://booking-service:8083
          predicates:
            - Path=/bookings/**
        
        - id: market-service
          uri: http://market-service:8084
          predicates:
            - Path=/market/**
        
        - id: chat-service
          uri: http://chat-service:8085
          predicates:
            - Path=/chat/**
        
        - id: search-service
          uri: http://search-service:8091
          predicates:
            - Path=/search/**
        
        - id: ai-service
          uri: http://ai-service:8088
          predicates:
            - Path=/api/v1/ai/**
        
        - id: trip-planning-service
          uri: http://trip-planning-service:8087
          predicates:
            - Path=/trips/**
        
        - id: admin-service
          uri: http://admin-service:8090
          predicates:
            - Path=/api/v1/admin/**
        
        - id: analytics-service
          uri: http://analytics-service:8093
          predicates:
            - Path=/api/v1/analytics/**
        
        - id: emergency-service
          uri: http://emergency-service:8089
          predicates:
            - Path=/api/v1/emergency/**
```

## Rate Limiting

API rate limiting is enforced per service:

- **Auth Service**: 100 requests/minute per IP
- **Search Service**: 1000 requests/minute per user
- **AI Service**: 50 requests/minute per user
- **Other Services**: 500 requests/minute per user

Rate limit headers are included in responses:
```http
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248000
```

## Error Codes

| Code | Description |
|------|-------------|
| `400` | Bad Request - Invalid input data |
| `401` | Unauthorized - Missing or invalid token |
| `403` | Forbidden - Insufficient permissions |
| `404` | Not Found - Resource not found |
| `409` | Conflict - Resource already exists |
| `422` | Unprocessable Entity - Validation failed |
| `429` | Too Many Requests - Rate limit exceeded |
| `500` | Internal Server Error - Server error |
| `502` | Bad Gateway - Service unavailable |
| `503` | Service Unavailable - Temporary outage |

## SDK and Client Libraries

Official SDKs are available for popular programming languages:

- **TypeScript/JavaScript**: `@hopngo/api-client`
- **Python**: `hopngo-python-sdk`
- **Java**: `hopngo-java-sdk`
- **Go**: `hopngo-go-sdk`

Installation:
```bash
# TypeScript/JavaScript
npm install @hopngo/api-client

# Python
pip install hopngo-python-sdk

# Java
<dependency>
    <groupId>com.hopngo</groupId>
    <artifactId>hopngo-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Testing

Use the interactive documentation above to test API endpoints directly, or use tools like:

- **Postman Collection**: [Download HopNGo API Collection](./assets/HopNGo-API.postman_collection.json)
- **Insomnia Workspace**: [Import HopNGo Workspace](./assets/HopNGo-API.insomnia.json)
- **cURL Examples**: See individual service documentation

## Support

For API support and questions:

- **Documentation**: [https://docs.hopngo.com](https://docs.hopngo.com)
- **Support Email**: [api-support@hopngo.com](mailto:api-support@hopngo.com)
- **Developer Portal**: [https://developers.hopngo.com](https://developers.hopngo.com)
- **Status Page**: [https://status.hopngo.com](https://status.hopngo.com)

---

*API documentation is automatically generated from OpenAPI 3.0 specifications. Last updated: $(date)*