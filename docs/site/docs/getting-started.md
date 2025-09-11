---
sidebar_position: 1
---

# Getting Started

Welcome to HopNGo! This guide will help you get the platform up and running locally for development.

## Overview

HopNGo is a modern travel booking platform with social features, built using a microservices architecture. The platform consists of:

- **Frontend**: React application with TypeScript
- **Backend Services**: Spring Boot microservices
- **Infrastructure**: Docker, PostgreSQL, MongoDB, Redis, RabbitMQ

## Prerequisites

Before you begin, ensure you have the following installed:

- **Docker & Docker Compose**: For running services
- **Node.js 18+**: For frontend development
- **Java 17+**: For backend development
- **Maven 3.8+**: For building Java services
- **Git**: For version control

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/hopngo/hopngo.git
cd hopngo
```

### 2. Start Infrastructure Services

```bash
# Start databases and message queue
docker-compose up -d postgres mongodb redis rabbitmq
```

### 3. Start Backend Services

```bash
# Start all microservices
docker-compose up -d auth-service booking-service social-service chat-service gateway
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

### 5. Verify Installation

Once everything is running, you can access:

- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **Booking Service**: http://localhost:8083
- **Social Service**: http://localhost:8082
- **Chat Service**: http://localhost:8085

## API Documentation

Each service exposes OpenAPI documentation:

- **Auth Service**: http://localhost:8081/swagger-ui/index.html
- **Booking Service**: http://localhost:8083/swagger-ui/index.html
- **Social Service**: http://localhost:8082/swagger-ui/index.html
- **Chat Service**: http://localhost:8085/swagger-ui/index.html

## Development Workflow

### Backend Development

1. **Make changes** to service code
2. **Rebuild** the service: `mvn clean package`
3. **Restart** the service: `docker-compose restart <service-name>`

### Frontend Development

1. **Make changes** to React components
2. **Hot reload** will automatically update the browser
3. **Run tests**: `npm test`

### Database Migrations

Services use Flyway for database migrations:

```bash
# Run migrations manually
mvn flyway:migrate -f auth-service/pom.xml
mvn flyway:migrate -f booking-service/pom.xml
```

## Testing

### Unit Tests

```bash
# Backend tests
mvn test

# Frontend tests
cd frontend && npm test
```

### Integration Tests

```bash
# Run with Testcontainers
mvn verify
```

### E2E Tests

```bash
# Playwright tests
cd frontend && npm run test:e2e
```

## Troubleshooting

### Common Issues

**Services not starting**
- Check Docker logs: `docker-compose logs <service-name>`
- Ensure ports are not in use
- Verify environment variables

**Database connection errors**
- Ensure PostgreSQL/MongoDB containers are running
- Check connection strings in application.yml

**Frontend build errors**
- Clear node_modules: `rm -rf node_modules && npm install`
- Check Node.js version compatibility

### Getting Help

- Check the [Architecture Overview](./architecture/overview.md)
- Review service-specific documentation
- Check GitHub issues for known problems

## Next Steps

- Explore the [Architecture](./architecture/overview.md) to understand the system design
- Learn about individual [Services](./services/auth-service.md)
- Set up your [Development Environment](./development/setup.md)
- Review the [API Reference](/api) for detailed endpoint documentation